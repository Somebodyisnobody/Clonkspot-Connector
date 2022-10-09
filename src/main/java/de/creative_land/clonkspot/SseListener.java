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

import com.here.oksse.ServerSentEvent;
import de.creative_land.Controller;
import de.creative_land.discord.DiscordConnector;
import net.dv8tion.jda.api.OnlineStatus;
import okhttp3.Request;
import okhttp3.Response;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SseListener implements ServerSentEvent.Listener {

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 5, 2, TimeUnit.MINUTES, new ArrayBlockingQueue<>(50));

    int errorCounter = 0;
    boolean firstStart = true;

    @Override
    public void onOpen(ServerSentEvent sse, Response response) {
        //Avoid spamming when channel reopens. Message will be sent only once.
        if (firstStart) {
            Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: SSE channel opened.");
            firstStart = false;
        }

        if (errorCounter >= 10 && Objects.equals(DiscordConnector.INSTANCE.status.getCurrentOnlineStatus(), OnlineStatus.DO_NOT_DISTURB)) {
            DiscordConnector.INSTANCE.status.setRunning();
            Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: Clonkspot is back!");
            Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: New status: RUNNING.");
        }

        //reset errorCounter on successful connect
        errorCounter = 0;
    }

    @Override
    public void onMessage(ServerSentEvent sse, String id, String event, String message) {
        try {
            executor.execute(() -> DiscordConnector.INSTANCE.gameDispatcher.process(message, event));
        } catch (Exception e) {
            Controller.INSTANCE.log.addLogEntry(e);
        }
    }

    @Override
    public void onComment(ServerSentEvent sse, String comment) {
        System.out.println("ClonkspotConnector: SSE comment received.");
    }

    @Override
    public boolean onRetryTime(ServerSentEvent sse, long milliseconds) {
        System.out.println("ClonkspotConnector: SSE retry time.");
        return false;
    }

    @Override
    public boolean onRetryError(ServerSentEvent sse, Throwable throwable, Response response) {
        if (errorCounter < 30000) {
            errorCounter++;
        }

        //Avoid spamming when channel reopens, just log at 2 errors in the roll
        if (errorCounter > 1) {
            Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: SSE retry error. Error counter: " + errorCounter + ".");
        }

        if (errorCounter >= 10) {
            if (!Objects.equals(DiscordConnector.INSTANCE.status.getCurrentOnlineStatus(), OnlineStatus.DO_NOT_DISTURB)) {
                DiscordConnector.INSTANCE.status.setErrUpstreamOffline();
                Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: New status: ERROR_UPSTREAM_OFFLINE.");
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {
            }
        }
        ClonkspotConnector.INSTANCE.startSse();
        return false;
    }

    @Override
    public void onClosed(ServerSentEvent sse) {
        //Avoid spamming when channel reopens. Message will be sent if onRetryError() wasn't called before
        if (errorCounter == 0) System.out.println("ClonkspotConnector: SSE channel closed.");
    }

    @Override
    public Request onPreRetry(ServerSentEvent sse, Request originalRequest) {
        return null;
    }
}
