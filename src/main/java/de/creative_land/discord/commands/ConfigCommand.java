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
import net.dv8tion.jda.api.entities.PrivateChannel;

import java.nio.charset.StandardCharsets;

public class ConfigCommand implements ServerCommand {

    @Override
    public void performCommand(PrivateChannel channel, String[] args) {
        final var configuration = Controller.INSTANCE.configuration;
        final var stringBuilder = new StringBuilder();
        final var newline = "\n";
        stringBuilder.append("Current configuration:").append(newline);

        try {
            //noinspection ConstantConditions
            stringBuilder.append("Server name: ").append(DiscordConnector.INSTANCE.getJda().getGuildById(configuration.getGuildId()).getName()).append(newline);
            //noinspection ConstantConditions
            stringBuilder.append("Admin role name: ").append(DiscordConnector.INSTANCE.getJda().getRoleById(configuration.getAdminRole()).getName()).append(newline);
            stringBuilder.append("Target dispatch channel name: ").append(DiscordConnector.INSTANCE.getJda().getTextChannelById(configuration.getTargetDispatchChannel())).append(newline);
        } catch (Exception ignored) {
        }
        stringBuilder.append("General host cooldown: ").append(configuration.getHostCooldown()).append(" minutes").append(newline);
        stringBuilder.append("Clonk version: \"").append(configuration.getEngine()).append("\" on ").append(configuration.getEngineBuild()).append(newline);
        stringBuilder.append("SSE Endpoint: \"").append(configuration.getSseEndpoint()).append(newline);

        stringBuilder.append(newline);
        stringBuilder.append("Ignored hostnames:").append(newline);
        if (configuration.getIgnoredHostnames().isEmpty()) {
            stringBuilder.append("\tNo ignored hostname set").append(newline);
        } else {
            for (final var ignoredHostname : configuration.getIgnoredHostnames()) {
                stringBuilder.append("\t\"").append(ignoredHostname.getHostname()).append("\" under a minimum of ").append(ignoredHostname.getMinPlayer()).append(" players by \"").append(ignoredHostname.getAuthor()).append("\" with reason \"").append(ignoredHostname.getReason()).append("\".").append(newline);
            }
        }
        stringBuilder.append(newline);

        stringBuilder.append("Mention roles cooldowns:").append(newline);
        if (configuration.getMentionRoleCooldowns().isEmpty()) {
            stringBuilder.append("\tNo cooldown set").append(newline);
        } else {
            for (final var mentionRoleCooldown : configuration.getMentionRoleCooldowns()) {
                stringBuilder.append("\tRole \"").append(mentionRoleCooldown.getRole()).append("\" with cooldown of ").append(mentionRoleCooldown.getCooldown()).append(" minutes by \"").append(mentionRoleCooldown.getAuthor()).append("\"").append(newline);
            }
        }
        stringBuilder.append(newline);

        stringBuilder.append("Manipulation rules:").append(newline);
        if (configuration.getManipulationRules().isEmpty()) {
            stringBuilder.append("\tNo manipulation rules set").append(newline);
        } else {
            for (final var manipulationRule : configuration.getManipulationRules()) {
                stringBuilder.append("\tRule \"").append(manipulationRule.getName()).append("\" with pattern \"").append(manipulationRule.getPattern().toString()).append("\" and replacement \"").append(manipulationRule.getReplacement()).append("\" was set by \"").append(manipulationRule.getAuthor()).append("\" and mentions the following roles:").append(newline);
                if (manipulationRule.getRoles().isEmpty()) {
                    stringBuilder.append("\t\tNo roles are mentioned").append(newline);
                } else {
                    for (int i = 0; i < manipulationRule.getRoles().size(); i++) {
                        stringBuilder.append("\t\t@").append(manipulationRule.getRoles().get(i)).append(newline);
                    }
                }
            }
        }
        stringBuilder.append(newline);

        final var message = stringBuilder.toString();
        try {
            if (message.length() < 2000) {
                channel.sendMessage("```\n" + message + "\n```").queue();
            } else {
                channel.sendFile(message.getBytes(StandardCharsets.UTF_8), "config.txt").queue();
            }
        } catch (Exception e) {
            channel.sendMessage(":x: Error: " + e.getClass().getName() + ", " + e.getMessage()).queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Failed to retrieve config: " + e.getClass().getName() + ", " + e.getMessage());
        }
    }
}