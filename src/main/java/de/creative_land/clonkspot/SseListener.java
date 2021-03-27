////////////////////////////////////////////////////////////////////////////////
// This file is part of the Clonkspot-Connector - https://github.com/Somebodyisnobody/Clonkspot-Connector
//
// Clonkspot-Connector is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// Clonkspot-Connector is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Clonkspot-Connector.  If not, see <http://www.gnu.org/licenses/>.
//
////////////////////////////////////////////////////////////////////////////////

package de.creative_land.clonkspot;

import de.creative_land.Controller;
import de.creative_land.discord.DiscordConnector;
import de.creative_land.sse.SSEListener;
import de.creative_land.sse.SSEMessage;
import net.dv8tion.jda.api.OnlineStatus;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SseListener implements SSEListener {
    
    private static final Duration TIMEOUT = Duration.of(3, ChronoUnit.MINUTES); //TODO move to config

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 5, 2, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(50));

    private final Timer timer = new Timer(true);
    private Optional<TimerTask> task = Optional.empty();
    private Instant lastMessage = Instant.now();

    int errorCounter = 0;
    boolean firstStart = true;

    @Override
    public void onOpen() {
        // Avoid spamming when channel reopens. Message will be sent only once.
        if (firstStart) {
            Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: SSE channel opened.");
            firstStart = false;
        }

        if (errorCounter >= 10 && Objects.equals(DiscordConnector.INSTANCE.status.getCurrentOnlineStatus(),
                OnlineStatus.DO_NOT_DISTURB)) {
            DiscordConnector.INSTANCE.status.setRunning();
            Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: Clonkspot is back!");
            Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: New status: RUNNING.");
        }

        // reset errorCounter on successful connect
        errorCounter = 0;
        
        synchronized (lastMessage) {
            task.ifPresent(TimerTask::cancel);
            task = Optional.of(toTask(this::tryTimeout));
            lastMessage = Instant.now();
            task.ifPresent(t -> timer.schedule(t, TIMEOUT.toMillis()));
        }
    }

    @Override
    public void onMessage(SSEMessage msg) {
        synchronized(lastMessage) {
            lastMessage = Instant.now();
            if (task.map(TimerTask::cancel).orElse(true)) {
                //task was cancelled or never scheduled
                task = Optional.of(toTask(this::tryTimeout));
                task.ifPresent(t -> timer.schedule(t, TIMEOUT.toMillis()));
            } else {
                //task is running or has ran already
                //we are restarting so abort
                return;
            }
        }
        
        try {
            executor.execute(() -> DiscordConnector.INSTANCE.dispatcher.process(msg.data, msg.event));
        } catch (RejectedExecutionException e) {
            Controller.INSTANCE.log.addLogEntry(e);
        }
    }

    @Override
    public void onComplete() {
        synchronized(timer) {
            task.ifPresent(TimerTask::cancel);
        }
        Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: SSE channel closed.");
        if (!Objects.equals(DiscordConnector.INSTANCE.status.getCurrentOnlineStatus(), OnlineStatus.DO_NOT_DISTURB)) {
            DiscordConnector.INSTANCE.status.setErrUpstreamOffline();
            Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: New status: ERROR_UPSTREAM_OFFLINE.");
        }
        ClonkspotConnector.INSTANCE.restart();
    }

    @Override
    public void onError(Throwable error) {
        synchronized(timer) {
            task.ifPresent(TimerTask::cancel);
        }
        if (errorCounter < 30000) {
            errorCounter++;
        }

        // Avoid spamming when channel reopens, just log at 2 errors in the roll
        if (errorCounter > 1) {
            Controller.INSTANCE.log
                    .addLogEntry("ClonkspotConnector: SSE retry error. Error counter: " + errorCounter + ".");
        }

        if (errorCounter >= 10) {
            if (!Objects.equals(DiscordConnector.INSTANCE.status.getCurrentOnlineStatus(),
                    OnlineStatus.DO_NOT_DISTURB)) {
                DiscordConnector.INSTANCE.status.setErrUpstreamOffline();
                Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: New status: ERROR_UPSTREAM_OFFLINE.");
            }
        }
        ClonkspotConnector.INSTANCE.restart();
    }
    
    private void tryTimeout() {
        task.ifPresent(TimerTask::cancel);
        synchronized (timer) {
            Instant now = Instant.now();
            Instant target = lastMessage.plus(TIMEOUT);
            if (now.isBefore(target)) {
                Controller.INSTANCE.log.addLogEntry(String.format(
                        "ClonkspotConnector: Stream restart is %d milliseconds too early.",
                                ChronoUnit.MILLIS.between(now, target)));
            }
        }
        ClonkspotConnector.INSTANCE.start();
    }
    
    private static TimerTask toTask(Runnable runnable) {
        return new TimerTask() {
            
            @Override
            public void run() {
                runnable.run();
            }
        };
    }
}
