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

package de.creative_land.twitch;

import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.domain.Stream;
import de.creative_land.discord.DiscordConnector;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Boolean.parseBoolean;

public class ScheduledRunnable implements Runnable {

    private final TwitchHelix client;
    private final String gameId;
    private final TextChannel channel;

    private final HashSet<DispatchedMessage> registeredStreams;

    public ScheduledRunnable(TwitchHelix client, String gameId) {
        this.client = client;
        this.gameId = gameId;
        this.channel = DiscordConnector.INSTANCE.getGameReferenceDispatchChannel();
        this.registeredStreams = new HashSet<>();

    }

    @Override
    public void run() {
        if (channel != null) {
            var activeStreams = client.getStreams(null, null, null, null, List.of(gameId), null, null, null).execute();
            // Test if stream was announced already (is a "registered" stream) and announce if not.
            for (var stream : activeStreams.getStreams()) {
                if (registeredStreams.stream().map(s -> s.getStream().getId()).noneMatch(s -> s.equals(stream.getId()))) {
                    String message = String.format(
                            "%s is streaming now on twitch: \"%s\"\nWatch here: %s", stream.getUserName(), stream.getTitle(), "https://www.twitch.tv/" + stream.getUserName());
                    channel.sendMessage(message).queue(sentMessage -> {
                        registeredStreams.add(new DispatchedMessage(sentMessage, stream));
                        if (parseBoolean(System.getenv("DEBUG")))
                            System.out.println("Created stream \"" + stream.getTitle() + "\"");
                    });
                } else if (parseBoolean(System.getenv("DEBUG")))
                    System.out.println("Stream \"" + stream.getTitle() + "\" already in list");

            }

            // Filter ended streams (streams which are not in activeStreams but in registeredStreams) and announce if a stream has ended.
            var activeStreamIds = activeStreams.getStreams().stream().map(Stream::getId).collect(Collectors.toList());
            registeredStreams.stream().filter(registeredStream -> !activeStreamIds.contains(registeredStream.getStream().getId())).forEach(
                    dispatchedMessage -> {
                        String message = String.format("%s has ended streaming: \"%s\"", dispatchedMessage.getStream().getUserName(), dispatchedMessage.getStream().getTitle());
                        dispatchedMessage.getMessage().editMessage(message).queue(
                                // Delete stream from registered streams when it ended and this was announced successfully
                                success -> {
                                    registeredStreams.remove(dispatchedMessage);
                                    if (parseBoolean(System.getenv("DEBUG")))
                                        System.out.println("Deleted stream \"" + dispatchedMessage.getStream().getTitle() + "\"");
                                }
                        );
                    }
            );
        }
    }
}
