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
import com.github.twitch4j.helix.domain.StreamList;
import de.creative_land.Controller;
import de.creative_land.discord.DiscordConnector;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.Boolean.parseBoolean;

public class ScheduledRunnable implements Runnable {

    private final TwitchHelix client;
    private final String gameId;
    private final TextChannel channel;
    private final HashSet<DispatchedMessage> registeredStreams;

    private final TwitchConfiguration config = Controller.INSTANCE.configuration.getTwitchConfiguration();

    public ScheduledRunnable(TwitchHelix client, String gameId) {
        this.client = client;
        this.gameId = gameId;
        this.channel = DiscordConnector.INSTANCE.getGameReferenceDispatchChannel();
        this.registeredStreams = new HashSet<>();

    }

    @Override
    public void run() {
        if (channel != null) {
            if (parseBoolean(System.getenv("DEBUG")))
                System.out.println("###################### New run ######################");
            var activeStreams = client.getStreams(null, null, null, null, List.of(gameId), null, null, null).execute();
            checkNewStreams(activeStreams);
            checkEndedStreams(activeStreams);
        }
    }

    /**
     * Test if stream was announced already (is a "registered" stream) and announce if not.
     *
     * @param activeStreams The list of currently active streams.
     */
    private void checkNewStreams(StreamList activeStreams) {
        for (var stream : activeStreams.getStreams()) {
            var optionalStream = registeredStreams.stream().filter(dispatchedMessage -> dispatchedMessage.getStream().getId().equals(stream.getId())).findFirst();
            if (optionalStream.isEmpty()) {
                String message = buildStreamStartedMessage(stream);
                channel.sendMessage(message).queue(sentMessage -> {
                    registeredStreams.add(new DispatchedMessage(sentMessage, stream));
                    if (parseBoolean(System.getenv("DEBUG")))
                        System.out.println("Created stream \"" + stream.getTitle() + "\"");
                });
            } else {
                var dispatchedMessage = optionalStream.get();
                int oldTimeout = dispatchedMessage.timeoutCounter;
                dispatchedMessage.timeoutCounter = 0;
                if (parseBoolean(System.getenv("DEBUG")))
                    System.out.println("Stream \"" + stream.getTitle() + "\" already in list. Old timeout counter: " + oldTimeout + ", new: " + dispatchedMessage.timeoutCounter);
            }
        }
    }

    /**
     * Filter ended streams (streams which are not in activeStreams but in registeredStreams) and announce if a stream has ended.
     * Twitch API seems to have a bug where it hides active streams, so they are reannounced after a while. This goes on and on and ends in a loop. Therefore, we need to set a timeout to n times which the stream must not be shown in the list to be deannounced.
     *
     * @param activeStreams The list of currently active streams.
     */
    private void checkEndedStreams(StreamList activeStreams) {
        var activeStreamIds = activeStreams.getStreams().stream().map(Stream::getId).collect(Collectors.toList());
        registeredStreams.stream().filter(registeredStream -> !activeStreamIds.contains(registeredStream.getStream().getId())).forEach(
                dispatchedMessage -> {
                    dispatchedMessage.timeoutCounter++;
                    if (parseBoolean(System.getenv("DEBUG")))
                        System.out.println("Timeout stream \"" + dispatchedMessage.getStream().getTitle() + "\": " + dispatchedMessage.timeoutCounter);
                    // If the stream didn't appear n times in the list then it has to be deannounced
                    if (dispatchedMessage.timeoutCounter > 4) {
                        String message = buildStreamEndedMessage(dispatchedMessage.getStream());
                        dispatchedMessage.getMessage().editMessage(message).queue(
                                // Delete stream from registered streams when it ended and this was announced successfully
                                success -> {
                                    registeredStreams.remove(dispatchedMessage);
                                    if (parseBoolean(System.getenv("DEBUG")))
                                        System.out.println("Deleted stream \"" + dispatchedMessage.getStream().getTitle() + "\"");
                                }
                        );
                    }

                }
        );
    }

    private String buildStreamStartedMessage(Stream stream) {
        String role = "";
        if (!Objects.equals(config.getNotificationRole(), "")) {
            // Cut away the "@" at the beginning of the role if it occurs there.
            var roleString = config.getNotificationRole();
            roleString = roleString.charAt(0) == '@' ? roleString.substring(1) : roleString;
            var roleId = DiscordConnector.INSTANCE.getJda().getRolesByName(roleString, false);
            if (!roleId.isEmpty()) {
                role = roleId.get(0).getAsMention() + " ";
            }
        }
        return String.format(
                "%s%s%s is streaming now on twitch: \"%s\"\nWatch here: %s", getIcon(), role, stream.getUserName(), stream.getTitle(), "https://www.twitch.tv/" + stream.getUserName());
    }

    private String buildStreamEndedMessage(@NotNull Stream stream) {
        return String.format("%s%s has ended streaming: \"%s\"", getIcon(), stream.getUserName(), stream.getTitle());
    }

    private String getIcon() {
        String icon = "";
        if (!Objects.equals(config.getIconString(), "")) {
            var iconString = config.getIconString();
            iconString = iconString.replace(":", "");
            var iconId = DiscordConnector.INSTANCE.getJda().getEmotesByName(iconString, false);
            if (!iconId.isEmpty()) {
                icon = iconId.get(0).getAsMention() + " ";
            }
        }
        return icon;
    }
}
