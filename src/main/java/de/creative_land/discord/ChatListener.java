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

package de.creative_land.discord;

import de.creative_land.Controller;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ChatListener extends ListenerAdapter {

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        final var commandManager = DiscordConnector.INSTANCE.commandManager;
        String message = event.getMessage().getContentDisplay();
        PrivateChannel channel = event.getChannel();


        if (event.getAuthor().getIdLong() != event.getJDA().getSelfUser().getIdLong()) {
            DiscordConnector.INSTANCE.getGuild().retrieveMember(event.getAuthor()).queue(member -> {
                if (!commandManager.checkAdmin(member)) {
                    channel.sendMessage("You are not permitted to talk with me!").queue();
                    Controller.INSTANCE.log.addLogEntry("DiscordConnector: Unauthorized command: \"" + event.getMessage().getContentDisplay() + "\", by " + event.getMessage().getAuthor().getName() + ".");
                    return;
                }

                commandManager.selectAndPerformCommand(message, event.getChannel());
            });
        }
    }
}
