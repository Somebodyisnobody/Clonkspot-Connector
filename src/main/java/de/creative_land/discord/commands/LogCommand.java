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

import java.nio.charset.StandardCharsets;

public class LogCommand implements ServerCommand {

    @Override
    public void performCommand(PrivateChannel channel, String[] args) {
        try {
            String log = Controller.INSTANCE.log.printLog();
            if (log.length() < 2000) {
                channel.sendMessage("```\n" + log + "\n```").queue();
            } else {
                channel.sendFile(log.getBytes(StandardCharsets.UTF_8), "log.txt").queue();
            }
        } catch (Exception e) {
            channel.sendMessage(":x: Error: " + e.getClass().getName() + ", " + e.getMessage()).queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Failed to retrieve log: " + e.getClass().getName() + ", " + e.getMessage());
        }

    }
}
