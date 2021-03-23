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
import de.creative_land.sse.SSEListener;
import de.creative_land.sse.SSEParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class ClonkspotConnector {
    public static ClonkspotConnector INSTANCE;

    private final SSEListener listener;

    private final SSEParser parser;

    private final HttpClient client;

    private final HttpRequest request;

    public ClonkspotConnector() {
        INSTANCE = this;

        listener = new SseListener();

        parser = new SSEParser(2000); // TODO offload to config
        parser.setListener(listener);

        client = HttpClient.newHttpClient();

        request = HttpRequest.newBuilder()
                .uri(URI.create(Controller.INSTANCE.configuration.getSseEndpoint()))
                .build();
        start();
    }

    /**
     * Creates a new SSE-Client and overwrites an old one.
     */
    protected void start() {
        var result = client.sendAsync(request, BodyHandlers.fromLineSubscriber(parser));
        result.exceptionally(e -> {
            listener.onError(e);
            return null;
        });
        result.thenAccept(r -> listener.onComplete());
        listener.onOpen();
    }

    /**
     * Tries to restart the sse client obeying the timeout specified by the server.
     */
    public void restart() {
        Instant now = Instant.now();
        Instant target = now.plus(parser.getTimeout(), ChronoUnit.MILLIS);
        while (now.isBefore(target)) {
            try {
                long delta = Duration.between(now, target).toMillis();
                if (delta < 1) {
                    break;
                }
                Thread.sleep(delta);
            } catch (InterruptedException ignored) {
            }
            now = Instant.now();
        }
        start();
    }

    /**
     * Closes this conncetion. This action is irreversible and no restarts will be possible.
     */
    public void close() {
        parser.close();
    }
}
