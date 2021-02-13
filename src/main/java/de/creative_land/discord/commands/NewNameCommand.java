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

import de.creative_land.Controller;
import de.creative_land.discord.DiscordConnector;
import net.dv8tion.jda.api.entities.PrivateChannel;

public class NewNameCommand implements ServerCommand {

    @Override
    public void performCommand(PrivateChannel channel, String[] args) {
        if (args.length != 1 || args[0].equals("")) {
            channel.sendMessage(":x: Not enough or too much arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        try {
            DiscordConnector.INSTANCE.getJda().getSelfUser().getManager().setName(String.join(" ", args)).queue();
            channel.sendMessage(":white_check_mark: New name set.").queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: New name set by \"" + channel.getUser().getName() + "\" (Name: \"" + String.join(" ", args) + "\").");
        } catch (IllegalArgumentException e) {
            channel.sendMessage(":x: Error: " + e.getClass().getName() + ", " + e.getMessage()).queue();
        }
    }
}
