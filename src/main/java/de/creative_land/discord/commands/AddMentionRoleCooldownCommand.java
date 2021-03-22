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
import de.creative_land.discord.dispatch.MentionRoleCooldown;
import net.dv8tion.jda.api.entities.PrivateChannel;

public class AddMentionRoleCooldownCommand implements ServerCommand {

    @Override
    public void performCommand(PrivateChannel channel, String[] args) {

        if (args.length != 2 || args[0].equals("") || args[1].equals("")) {
            channel.sendMessage(":x: Not enough or too much arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        try {
            if (args[0].charAt(0) == "@".charAt(0)) args[0] = args[0].substring(1);
            final var cooldown = Integer.parseInt(args[1]);
            if (Controller.INSTANCE.configuration.addMentionRoleCooldown(new MentionRoleCooldown(args[0], cooldown, channel.getUser().getName()))) {
                channel.sendMessage(":white_check_mark: New role cooldown set: " + cooldown + " minutes for \"@" + args[0] + "\".").queue();
                Controller.INSTANCE.log.addLogEntry("DiscordConnector: New role cooldown set: " + cooldown + " minutes for \"@" + args[0] + "\" by \"" + channel.getUser().getName() + "\".");
            } else {
                channel.sendMessage(":x: Error: Role does already exist: \"" + args[0] + "\".").queue();
            }
        } catch (NumberFormatException e) {
            channel.sendMessage(":x: Failed to parse integer.").queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Failed to set new role cooldown: ", e);
        }

    }
}
