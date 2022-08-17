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

package de.creative_land.http_input;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.creative_land.discord.DiscordConnector;
import net.dv8tion.jda.api.entities.TextChannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class SayHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String requestParamValue = null;
        if ("POST".equals(httpExchange.getRequestMethod())) {
            try {
                handlePostRequest(httpExchange);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            //return not implemented
        }
        handleResponse(httpExchange, requestParamValue);
    }

    private void handlePostRequest(HttpExchange httpExchange) throws IOException, InterruptedException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = httpExchange.getRequestBody().read(buffer)) != -1; ) {
            result.write(buffer, 0, length);
        }
        // StandardCharsets.UTF_8.name() > JDK 7
        var message = result.toString(StandardCharsets.UTF_8);
        final String decodedMessage = URLDecoder.decode(message, StandardCharsets.UTF_8);
        final TextChannel channel = DiscordConnector.INSTANCE.getJda().getTextChannelById("1009559715296055338");
        if (channel != null) {
            channel.sendMessage(decodedMessage).complete();
        }

    }

    private void handleResponse(HttpExchange httpExchange, String requestParamValue) throws IOException {
        OutputStream outputStream = httpExchange.getResponseBody();
        httpExchange.sendResponseHeaders(200, 0);
        outputStream.flush();
        outputStream.close();
    }
}