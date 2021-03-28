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
import net.dv8tion.jda.api.entities.PrivateChannel;

public class RemoveManipulationRuleCommand implements ServerCommand {

    @Override
    public void performCommand(PrivateChannel channel, String[] args) {
        if (args.length < 1) {
            channel.sendMessage(":x: Not enough arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        final var name = String.join(" ", args);
        if (Controller.INSTANCE.configuration.removeManipulationRule(name)) {
            channel.sendMessage(":white_check_mark: Manipulation rule deleted.").queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Manipulation rule \"" + name + "\" removed by \"" + channel.getUser().getName() + "\".");
        } else {
            channel.sendMessage(":x: Error: Manipulation rule not found.").queue();
        }
    }
}