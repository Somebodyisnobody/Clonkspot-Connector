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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TimeZone;

public class SayHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        if ("POST".equals(httpExchange.getRequestMethod())) {
            try {
                handlePostRequest(httpExchange);
                handleResponse(httpExchange, 200);
            } catch (Exception e) {
                handleResponse(httpExchange, 500);
            }
        }
    }

    private void handlePostRequest(HttpExchange httpExchange) throws IOException, InterruptedException, ParseException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = httpExchange.getRequestBody().read(buffer)) != -1; ) {
            result.write(buffer, 0, length);
        }
        // StandardCharsets.UTF_8.name() > JDK 7
        var body = result.toString(StandardCharsets.UTF_8);
        // split into separate lines
        body = body.replaceAll("&", "\n");
        final String decodedBody = URLDecoder.decode(body, StandardCharsets.UTF_8);

        HashMap<String, String> data = new HashMap<String, String>();
        Scanner scanner = new Scanner(decodedBody);
        while (scanner.hasNextLine()) {
            String[] line = scanner.nextLine().split("=");
            if (line.length == 2) data.put(line[0], line[1]);
            if (line.length == 1) data.put(line[0], null);
        }
        scanner.close();


        final TextChannel channel = DiscordConnector.INSTANCE.getJda().getTextChannelById("1009559715296055338");

        if (channel != null) {
            switch (data.get("shortname")) {
                case "league.clonkspot.org" -> {
                    //Parse date from String
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                    sdf.setTimeZone(TimeZone.getDefault());
                    Date date = sdf.parse(data.get("lastcheck").replaceAll("(^\\d\\d\\.\\d\\d.\\d{4}\\s\\d\\d:\\d\\d:\\d\\d).*", "$1"));

                    //Format output strings
                    final String lastValue = data.get("lastvalue").replaceAll("([0-9]+\\sms).*", "$1");
                    sdf = new SimpleDateFormat("HH:mm");
                    final String lastCheckTime = sdf.format(date);

                    if ("Warnung".equals(data.get("status"))) {
                        String message = String.format(
                                "*Warning:* High response time of the master server. Last value: %s at %s",
                                lastValue, lastCheckTime);
                        channel.sendMessage(message).queue();
                    } else if ("Fehler".equals(data.get("status"))) {
                        String message = String.format(
                                "*Alert:* Clonkspot services are degraded. The master server does not respond within a given threshold. Last value: %s at %s",
                                lastValue, lastCheckTime);
                        channel.sendMessage(message).queue();
                    }
                }
                case "clonkspot.org" -> {

                }
            }
        }

    }

    private void handleResponse(HttpExchange httpExchange, int status) throws IOException {
        OutputStream outputStream = httpExchange.getResponseBody();
        httpExchange.sendResponseHeaders(status, 0);
        outputStream.flush();
        outputStream.close();
    }
}