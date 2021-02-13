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
import de.creative_land.discord.DiscordConnector;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.PrivateChannel;

import java.util.Objects;

public class TargetChannelCommand implements ServerCommand {

    @Override
    public void performCommand(PrivateChannel channel, String[] args) {
        if (args.length != 1 || args[0].equals("")) {
            channel.sendMessage(":x: Not enough or too much arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        if (args[0].charAt(0) == "#".charAt(0)) args[0] = args[0].substring(1);
        final var textChannels = DiscordConnector.INSTANCE.getJda().getTextChannelsByName(args[0], true);
        if (textChannels.isEmpty()) {
            channel.sendMessage(":x: Channel not found.").queue();
        } else if (textChannels.size() > 1) {
            channel.sendMessage(":x: Too many channels found (retry case sensitive).").queue();
        } else {
            if (DiscordConnector.INSTANCE.readNewTargetDispatchChannel(textChannels.get(0).getIdLong())) {
                channel.sendMessage(":white_check_mark: New target channel set.").queue();
                Controller.INSTANCE.log.addLogEntry("DiscordConnector: New target channel set by \"" + channel.getUser().getName() + "\" (Channel: \"#" + textChannels.get(0).getName() + "\").");
                if (Objects.equals(DiscordConnector.INSTANCE.getJda().getPresence().getActivity(), Activity.competing(de.creative_land.discord.Activity.ERROR_NO_CHANNEL.toString()))) {
                    DiscordConnector.INSTANCE.status.setRunning();
                    channel.sendMessage(":white_check_mark: Started the service. New games will be announced when they appear on clonkspot.").queue();
                    Controller.INSTANCE.log.addLogEntry("DiscordConnector: Service started by \"" + channel.getUser().getName() + "\".");
                }
            }
        }
    }
}
