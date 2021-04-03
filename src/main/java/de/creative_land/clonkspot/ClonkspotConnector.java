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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import de.creative_land.Controller;
import de.creative_land.sse.SSEListener;
import de.creative_land.sse.SSEParser;

public class ClonkspotConnector {
    public static ClonkspotConnector INSTANCE;

    private SSEListener listener;

    private SSEParser parser;
    
    private CompletableFuture<HttpResponse<Void>> result;

    private final HttpClient client;

    private final HttpRequest request;

    public ClonkspotConnector() {
        INSTANCE = this;

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
        if (parser != null) {
            parser.close();
        }
        if(result != null) {
            result.cancel(true);
        }
        listener = new SseListener();
        parser = new SSEParser(2000);
        parser.setListener(listener);
        result = client.sendAsync(request, BodyHandlers.fromLineSubscriber(parser));
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
                TimeUnit.MILLISECONDS.sleep(Duration.between(now, target).toMillis());
            } catch (InterruptedException ignored) {
            }
            now = Instant.now();
        }
        start();
    }

    /**
     * Closes this connection.
     */
    public void close() {
        if(parser != null) {
            parser.close();
        }
    }
}
