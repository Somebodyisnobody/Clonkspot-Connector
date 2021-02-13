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
import net.dv8tion.jda.api.entities.PrivateChannel;

public class HostCooldownCommand implements ServerCommand {

    @Override
    public void performCommand(PrivateChannel channel, String[] args) {
        if (args.length != 1 || args[0].equals("")) {
            channel.sendMessage(":x: Not enough or too much arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        try {
            final var oldCooldown = Controller.INSTANCE.configuration.getHostCooldown();
            final var newCooldown = Integer.parseInt(args[0]);
            Controller.INSTANCE.configuration.setHostCooldown(newCooldown);
            channel.sendMessage(":white_check_mark: Host cooldown set from " + oldCooldown + " to " + newCooldown).queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: New host cooldown set from " + oldCooldown + " minutes to " + newCooldown + " minutes by \"" + channel.getUser().getName() + "\".");
        } catch (NumberFormatException e) {
            channel.sendMessage(":x: Error: Failed to parse integer.").queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Failed to set new host cooldown: " + e.getClass().getName() + ", " + e.getMessage());
        }
    }
}
