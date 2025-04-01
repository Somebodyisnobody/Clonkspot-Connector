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

import com.here.oksse.OkSse;
import com.here.oksse.ServerSentEvent;
import de.creative_land.Controller;
import okhttp3.Request;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClonkspotConnector {
    public static ClonkspotConnector INSTANCE;

    private final Request request;

    private final OkSse okSse;

    public ServerSentEvent sse;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final WatchDog watchDog;

    public ClonkspotConnector() {
        INSTANCE = this;

        request = new Request.Builder().url(Controller.INSTANCE.configuration.getSseEndpoint()).build();

        okSse = new OkSse();

        watchDog = new WatchDog(
                this::restartSSE,
                Duration.of(Controller.INSTANCE.configuration.getSseWatchdogTimeout(), ChronoUnit.SECONDS)
        );
        restartSSE();
        executorService.execute(watchDog);
    }

    /**
     * Creates a new SSE-Client and overwrites an old one.
     */
    protected void restartSSE() {
        if (sse != null) sse.close();
        sse = okSse.newServerSentEvent(request, new SseListener(watchDog));
    }



}
