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
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.Boolean.parseBoolean;

public class PrtgHttpHandler implements HttpHandler {

    private boolean stoppedByMonitoring;

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

    private void handlePostRequest(HttpExchange httpExchange) throws IOException, ParseException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = httpExchange.getRequestBody().read(buffer)) != -1; ) {
            result.write(buffer, 0, length);
        }
        var body = result.toString(StandardCharsets.UTF_8);
        // Split into separate lines when k=v&k=v&... is used in incoming data
        body = body.replaceAll("&", "\n");
        final String decodedBody = URLDecoder.decode(body, StandardCharsets.UTF_8);
        // Print incoming data to console for debugging purposes
        if (parseBoolean(System.getenv("DEBUG"))) System.out.println(body);

        HashMap<String, String> data = new HashMap<>();
        Scanner scanner = new Scanner(decodedBody);
        // For each line split key from value at "=" and write it into the "data"-map
        while (scanner.hasNextLine()) {
            String[] line = scanner.nextLine().split("=");
            if (line.length == 2) data.put(line[0], line[1]);
            if (line.length == 1) data.put(line[0], null);
        }
        scanner.close();


        //final TextChannel channel = DiscordConnector.INSTANCE.getJda().getTextChannelById("1009559715296055338");
        final TextChannel channel = DiscordConnector.INSTANCE.getGameReferenceDispatchChannel();

        // Abort on misconfigured discord-api or if the sensor is not tagged with the "discord-notify" tag
        if (channel != null && data.get("objecttags") != null && data.get("objecttags").contains("discord-notify")) {

            //Parse date from String
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            sdf.setTimeZone(TimeZone.getDefault());
            Date date = sdf.parse(data.get("lastcheck").replaceAll("(^\\d\\d\\.\\d\\d.\\d{4}\\s\\d\\d:\\d\\d:\\d\\d).*", "$1"));

            // Format output strings
            String lastValue = "Unknown";
            if (data.get("lastvalue") != null) {
                lastValue = data.get("lastvalue").replaceAll("([0-9]+\\sms).*", "$1");
            }
            sdf = new SimpleDateFormat("HH:mm z", Locale.ENGLISH);
            final String lastCheckTime = sdf.format(date);
            final String shortname = data.get("shortname");

            switch (shortname) {
                case "league.clonkspot.org" -> {
                    // Build and send messages
                    if ("Warnung".equals(data.get("laststatus"))) {
                        String message = String.format(
                                ":warning: **Warning:** High response time of the league masterserver. Last value: %s at %s.",
                                lastValue, lastCheckTime);
                        channel.sendMessage(message).queue();
                        if (this.stoppedByMonitoring) {
                            DiscordConnector.INSTANCE.status.setRunning();
                            this.stoppedByMonitoring = false;
                        }
                    } else if ("Fehler".equals(data.get("laststatus"))) {
                        String message = String.format(
                                ":x: **Alert:** Clonkspot services are degraded. The league masterserver does not respond within a given threshold. Last value: %s at %s.  Use `league.crsm.cf:80` as temporary alternate server in the game options or join directly via IP.",
                                lastValue, lastCheckTime);
                        channel.sendMessage(message).queue();
                        if (Objects.equals(DiscordConnector.INSTANCE.status.getCurrentOnlineStatus(), OnlineStatus.ONLINE)) {
                            DiscordConnector.INSTANCE.status.setErrUpstreamOffline();
                            this.stoppedByMonitoring = true;
                        }
                    } else if ("OK".equals(data.get("laststatus")) && this.stoppedByMonitoring) {
                        String message = String.format(
                                ":white_check_mark: The league masterserver is back in normal state since %s. Don't forget to turn off the alternate server in the game options.", lastCheckTime);
                        channel.sendMessage(message).queue();
                        DiscordConnector.INSTANCE.status.setRunning();
                        this.stoppedByMonitoring = false;
                    } else if ("OK".equals(data.get("laststatus")) && !this.stoppedByMonitoring) {
                        String message = String.format(
                                ":white_check_mark: The league masterserver is back in normal state since %s.", lastCheckTime);
                        channel.sendMessage(message).queue();
                    }
                }
                case "clonkspot.org", "crema.clonkspot.org", "forum.clonkspot.org" -> {
                    // Build and send messages
                    if ("Warnung".equals(data.get("laststatus"))) {
                        String message = String.format(
                                ":warning: **Warning:** High response time of service \"%s\". Last value: %s at %s.",
                                shortname, lastValue, lastCheckTime);
                        channel.sendMessage(message).queue();
                    } else if ("Fehler".equals(data.get("laststatus"))) {
                        String message = String.format(
                                ":x: **Alert:** Clonkspot services are degraded. The service \"%s\" does not respond within a given threshold. Last value: %s at %s.",
                                shortname, lastValue, lastCheckTime);
                        channel.sendMessage(message).queue();
                    } else if ("OK".equals(data.get("laststatus"))) {
                        String message = String.format(
                                ":white_check_mark: The service \"%s\" is back in normal state since %s.", shortname, lastCheckTime);
                        channel.sendMessage(message).queue();
                        DiscordConnector.INSTANCE.status.setRunning();
                        this.stoppedByMonitoring = false;
                    }
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