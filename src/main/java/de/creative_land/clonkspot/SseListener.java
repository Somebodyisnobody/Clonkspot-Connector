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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.OnlineStatus;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class SseListener implements ServerSentEvent.Listener {

    int errorCounter = 0;
    private final WatchDog watchDog;
    boolean errorStatusSet = false;

    /**
     * The underlying implementation only calls onOpen()
     * when the HTTP response was successful (Okhttp3.Response::isSuccessful)
     */
    @Override
    public void onOpen(ServerSentEvent sse, Response response) {
        Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: SSE channel opened.");

        watchDog.feed();

        errorCounter = 0;

        if (errorStatusSet && Objects.equals(DiscordConnector.INSTANCE.status.getCurrentOnlineStatus(), OnlineStatus.DO_NOT_DISTURB)) {
            DiscordConnector.INSTANCE.status.setRunning();
            Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: Clonkspot is back!");
            Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: New status: RUNNING.");
            errorStatusSet = false;
        }
    }

    @Override
    public void onMessage(ServerSentEvent sse, String id, String event, String message) {
        watchDog.feed();
        try {
            DiscordConnector.INSTANCE.gameDispatcher.process(message, event);
        } catch (Exception e) {
            Controller.INSTANCE.log.addLogEntry(e);
        }
    }

    @Override
    public void onComment(ServerSentEvent sse, String comment) {
        watchDog.feed();
        log.debug("ClonkspotConnector: SSE comment received: '{}'", comment);
    }

    @Override
    public boolean onRetryTime(ServerSentEvent sse, long milliseconds) {
        return true;
    }

    @Override
    public boolean onRetryError(ServerSentEvent sse, Throwable throwable, Response response) {
        if (throwable instanceof IOException ioException) {
            Controller.INSTANCE.log.addLogEntry(
                    "ClonkspotConnector: SSE error: '%s'".formatted(ioException)
            );
        }

        // Don't try again if http status code is errornous.
        if (response != null && !response.isSuccessful()) {
            Controller.INSTANCE.log.addLogEntry(
                    "ClonkspotConnector: SSE response from upstream returned HTTP status code: '%s' Will not retry again.".formatted(
                            response.code()
                    )
            );
            setErrorStatus();
            return false;
        }

        // Don't try again after more than 10 unsuccessful retries
        if (errorCounter >= 10) {
            Controller.INSTANCE.log
                    .addLogEntry("ClonkspotConnector: Tried to reconnect SSE client 10 times. Will not retry again.");
            setErrorStatus();
            return false;
        }

        errorCounter++;
        return true;
    }

    private void setErrorStatus() {
        errorStatusSet = true;
        if (!Objects.equals(DiscordConnector.INSTANCE.status.getCurrentOnlineStatus(), OnlineStatus.DO_NOT_DISTURB)) {
            DiscordConnector.INSTANCE.status.setErrUpstreamOffline();
            Controller.INSTANCE.log.addLogEntry("ClonkspotConnector: New status: ERROR_UPSTREAM_OFFLINE.");
        }
    }

    @Override
    public void onClosed(ServerSentEvent sse) {
        log.info("ClonkspotConnector: SSE channel closed.");
    }

    @Override
    public Request onPreRetry(ServerSentEvent sse, Request originalRequest) {
        return originalRequest;
    }
}
