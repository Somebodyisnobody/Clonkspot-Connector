// This file is part of the Clonkspot-Connector - https://github.com/Somebodyisnobody/Clonkspot-Connector/
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

package de.creative_land.discord.commands;

import de.creative_land.Controller;
import de.creative_land.IgnoredHostname;
import net.dv8tion.jda.api.entities.PrivateChannel;

public class AddIgnoredHostCommand implements ServerCommand {

    @Override
    public void performCommand(PrivateChannel channel, String[] args) {
        if (args.length < 2 || args[0].equals("") || args[1].equals("")) {
            channel.sendMessage(":x: Not enough arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        try {
            final var joinedArgs = String.join(" ", args);
            String[] arguments = joinedArgs.charAt(0) == "`".toCharArray()[0] ? joinedArgs.substring(1).split("`") : new String[0];
            if (arguments.length == 5 && !arguments[0].equals(" ") && arguments[1].equals(" ") && !arguments[2].equals(" ") && arguments[3].equals(" ") && !arguments[4].equals(" ")) {

                final var minPlayer = Integer.parseInt(arguments[0]);
                if (Controller.INSTANCE.configuration.addIgnoredHostname(new IgnoredHostname(arguments[2], channel.getUser().getName(), minPlayer, arguments[4]))) {
                    channel.sendMessage(":white_check_mark: New ignored host \"" + arguments[2] + "\" with minimum of " + minPlayer + " players and reason \"" + arguments[4] + "\" added").queue();
                    Controller.INSTANCE.log.addLogEntry("DiscordConnector: New ignored host \"" + arguments[2] + "\" with minimum of " + minPlayer + " players added by \"" + channel.getUser().getName() + "\" with reason \"" + arguments[4] + "\".");
                } else {
                    channel.sendMessage(":x: Error: This hostname already in the list.").queue();
                }

            }
        } catch (NumberFormatException e) {
            channel.sendMessage(":x: Error: Failed to parse integer.").queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Failed to set new ignored host cooldown: " + e.getClass().getName() + ", " + e.getMessage());
        }
    }
}
