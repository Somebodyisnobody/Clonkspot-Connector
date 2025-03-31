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

package de.creative_land.discord;

import de.creative_land.Controller;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ChatListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(@NonNull MessageReceivedEvent event) {
        super.onMessageReceived(event);

        if (!event.getChannelType().equals(ChannelType.PRIVATE)) {
            return;
        }
        final String message = event.getMessage().getContentDisplay();
        final PrivateChannel channel = event.getChannel().asPrivateChannel();

        if (event.getAuthor().getIdLong() != event.getJDA().getSelfUser().getIdLong()) {
            try {
                DiscordConnector.INSTANCE.getGuild().retrieveMember(event.getAuthor()).queue(member -> {
                    final CommandManager commandManager = DiscordConnector.INSTANCE.commandManager;
                    if (!commandManager.checkAdmin(member)) {
                        channel.sendMessage("You are not permitted to talk with me!").queue();
                        Controller.INSTANCE.log.addLogEntry("DiscordConnector: Unauthorized command: \"" + event.getMessage().getContentDisplay() + "\", by " + event.getMessage().getAuthor().getName() + ".");
                        return;
                    }

                    commandManager.selectAndPerformCommand(message, channel);
                });
            } catch (ErrorResponseException e) {
                Controller.INSTANCE.log.addLogEntry("DiscordConnector: Unauthorized command: \"" + event.getMessage().getContentDisplay() + "\", by " + event.getMessage().getAuthor().getName() + ".");
            }
        }


    }
}
