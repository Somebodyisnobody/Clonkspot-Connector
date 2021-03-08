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
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.PrivateChannel;

import java.util.Objects;

public class StartCommand implements ServerCommand {

    @Override
    public void performCommand(PrivateChannel channel, String[] args) {
        final var jdaPresence = DiscordConnector.INSTANCE.getJda().getPresence();
        if (Objects.equals(jdaPresence.getActivity(), Activity.watching(de.creative_land.discord.Activity.RUNNING.toString()))) {
            channel.sendMessage(":x: Service already started. New games will be announced when they appear on clonkspot.").queue();
        } else if (Objects.equals(DiscordConnector.INSTANCE.status.getCurrentOnlineStatus(), OnlineStatus.DO_NOT_DISTURB)) {
            channel.sendMessage(":x: Error, please see in log.").queue();
        } else {
            DiscordConnector.INSTANCE.status.setRunning();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Service started by \"" + channel.getUser().getName() + "\".");
            channel.sendMessage(":white_check_mark: Started the service. New games will be announced when they appear on clonkspot.").queue();
        }
    }
}
