// This file is part of the Clonkspot-Connector - https://github.com/Somebodyisnobody/Clonkspot-Connector/
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

package de.creative_land.clonkspot;

import com.here.oksse.OkSse;
import com.here.oksse.ServerSentEvent;
import de.creative_land.Controller;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.concurrent.TimeUnit;

public class ClonkspotConnector {
    public static ClonkspotConnector INSTANCE;

    private final Request request;

    private final OkSse okSse;

    private final SseListener listener;

    public ServerSentEvent sse;

    public ClonkspotConnector() {
        INSTANCE = this;

        listener = new SseListener();

        @SuppressWarnings("unused")
        OkHttpClient client = new OkHttpClient.Builder().readTimeout(200, TimeUnit.SECONDS).build();
        //OkSse okSse = new OkSse(client);
        okSse = new OkSse();
        request = new Request.Builder().url(Controller.INSTANCE.configuration.getSseEndpoint()).build();
        startSse();
    }

    /**
     * Creates a new SSE-Client and overwrites an old one.
     */
    protected void startSse() {
        sse = okSse.newServerSentEvent(request, listener);
    }

}
