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

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.creative_land.Controller;
import de.creative_land.discord.DiscordConnector;
import de.creative_land.sse.SSEListener;
import de.creative_land.sse.SSEMessage;
import net.dv8tion.jda.api.OnlineStatus;

public class SseListener implements SSEListener {

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 5, 2, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(50));

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
    }

    @Override
    public void onMessage(SSEMessage msg) {
        try {
            executor.execute(() -> DiscordConnector.INSTANCE.dispatcher.process(msg.data, msg.event));
        } catch (RejectedExecutionException e) {
            Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: ThreadPool error: " + e.getMessage());
        }
    }

    @Override
    public void onComplete() {
        Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: SSE channel closed.");
        if (!Objects.equals(DiscordConnector.INSTANCE.status.getCurrentOnlineStatus(), OnlineStatus.DO_NOT_DISTURB)) {
            DiscordConnector.INSTANCE.status.setErrUpstreamOffline();
            Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: New status: ERROR_UPSTREAM_OFFLINE.");
        }
        ClonkspotConnector.INSTANCE.restart();
    }

    @Override
    public void onError(Throwable error) {
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
}
