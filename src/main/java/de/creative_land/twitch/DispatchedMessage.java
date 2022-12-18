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

import com.github.twitch4j.helix.domain.Stream;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;

class DispatchedMessage {

    private final OffsetDateTime created;
    /**
     * The last game reference which caused an action on discord.
     */
    private Stream stream;
    /**
     * The last message which was sent to discord. When parent object is marked as deleted last message is not updated.
     */
    private Message message;

    /**
     * Twitch API seems to have a bug where it hides active streams, so they are reannounced after a while. This goes on and on and ends in a loop. Therefore, we need to set a timeout to n times which the stream must not be shown in the list to be deannounced.
     * This variable is for tracing how often the stream was not in the list of active streams.
     */
    public int timeoutCounter;

    public DispatchedMessage(@NotNull Message message, @NotNull Stream stream) {
        this.message = message;
        this.stream = stream;
        this.created = OffsetDateTime.now();
    }

    public Message getMessage() {
        return message;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public DispatchedMessage update(@NotNull Message message, @NotNull Stream stream) {
        this.message = message;
        this.stream = stream;
        return this;
    }

    public Stream getStream() {
        return stream;
    }

}
