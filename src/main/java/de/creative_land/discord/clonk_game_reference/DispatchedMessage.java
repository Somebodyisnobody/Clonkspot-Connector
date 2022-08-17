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

package de.creative_land.discord.clonk_game_reference;

import de.creative_land.clonkspot.model.GameReference;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;

public class DispatchedMessage {

    private final OffsetDateTime created;

    /**
     * The last message which was sent to discord. When parent object is marked as deleted last message is not updated.
     */
    private Message message;

    /**
     * The last game reference which caused an action on discord.
     */
    private GameReference gameReference;

    private boolean deleted;

    public DispatchedMessage(@NotNull Message message, @NotNull GameReference gameReference) {
        this.message = message;
        this.gameReference = gameReference;
        this.created = OffsetDateTime.now();
        this.deleted = false;
    }

    public Message getMessage() {
        return message;
    }

    public GameReference getGameReference() {
        return gameReference;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean getDeleted() {
        return deleted;
    }

    public DispatchedMessage update(@NotNull Message message, @NotNull GameReference gameReference) {
        this.message = message;
        this.gameReference = gameReference;
        return this;
    }

    public DispatchedMessage markAsDeleted(@NotNull GameReference gameReference) {
        this.gameReference = gameReference;
        this.deleted = true;
        return this;
    }
}
