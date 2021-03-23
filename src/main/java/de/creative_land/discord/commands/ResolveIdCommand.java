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

package de.creative_land.discord.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.creative_land.Controller;
import de.creative_land.discord.DiscordConnector;
import net.dv8tion.jda.api.entities.PrivateChannel;

public class ResolveIdCommand implements ServerCommand {

    @Override
    public void performCommand(PrivateChannel channel, String[] args) {
        try {
            final var gameReference = DiscordConnector.INSTANCE.dispatcher.getDispatchedMessages().stream().filter(dispatchedMessage -> dispatchedMessage.getGameReference().id == Integer.parseInt(args[0])).findFirst();
            if (gameReference.isPresent()) {
                ObjectMapper mapper = new ObjectMapper();
                channel.sendMessage(mapper.writeValueAsString(gameReference.get().getGameReference())).queue();
            } else {
                channel.sendMessage("No game reference matched that id. It was not dispatched or is no longer available in cache.").queue();
            }
        } catch (NumberFormatException e) {
            channel.sendMessage(":x: Error: Failed to parse integer.").queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Failed to resolve game reference: " + e.getClass().getName() + ", " + e.getMessage());
        } catch (JsonProcessingException e) {
            channel.sendMessage(":x: Error while parsing GameReferenceModel.").queue();
            Controller.INSTANCE.log.addLogEntry("Controller: Error while parsing GameReferenceModel: ", e);
        }
    }
}
