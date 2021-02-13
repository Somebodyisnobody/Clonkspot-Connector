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
import de.creative_land.discord.dispatch.ManipulationRule;
import net.dv8tion.jda.api.entities.PrivateChannel;

import java.util.regex.Pattern;

public class AddManipulationRuleCommand implements ServerCommand {

    @Override
    public void performCommand(PrivateChannel channel, String[] args) {

        try {
            //Parse strings
            final var joinedArgs = String.join(" ", args);
            String[] arguments = joinedArgs.charAt(0) == "`".toCharArray()[0] ? joinedArgs.substring(1).split("`") : new String[0];
            if (arguments.length == 7 && !arguments[0].equals(" ") && arguments[1].equals(" ") && !arguments[2].equals(" ") && arguments[3].equals(" ") && !arguments[4].equals(" ") && arguments[5].equals(" ") && !arguments[6].equals(" ")) {

                //Remove @
                String[] roles = arguments[6].split(",");
                for (int i = 0; i < roles.length; i++) {
                    if (roles[i].charAt(0) == "@".charAt(0)) roles[i] = roles[i].substring(1);
                }

                //Fire
                final var manipulationRule = new ManipulationRule(Pattern.compile(arguments[2]), arguments[4], roles, channel.getUser().getName(), arguments[0]);
                if (Controller.INSTANCE.configuration.addManipulationRule(manipulationRule)) {

                    Controller.INSTANCE.log.addLogEntry("DiscordConnector: New manipulation rule \"" + manipulationRule.getName() + "\" with with pattern \"" + manipulationRule.getPattern() + "\" and replacement \"" + manipulationRule.getReplacement() + "\" added by \"" + channel.getUser().getName() + "\" with following mentions:");

                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(":white_check_mark: New manipulation rule `").append(manipulationRule.getName()).append("` with pattern `").append(manipulationRule.getPattern().toString()).append("` and replacement `").append(manipulationRule.getReplacement()).append("` successfully added and mentions the following roles:").append("\n");
                    if (manipulationRule.getRoles().isEmpty()) {
                        stringBuilder.append("\tNo roles are mentioned").append("\n");
                        Controller.INSTANCE.log.addLogEntry("DiscordConnector: No roles are mentioned.");
                    } else {
                        for (int i = 0; i < manipulationRule.getRoles().size(); i++) {
                            stringBuilder.append("\t@").append(manipulationRule.getRoles().get(i)).append("\n");
                            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Mention: @" + manipulationRule.getRoles().get(i) + ".");
                        }
                    }
                    channel.sendMessage(stringBuilder.toString()).queue();
                } else {
                    channel.sendMessage(":x: Error: This rule already in the list.").queue();
                }

            } else {
                channel.sendMessage(":x: Not enough arguments. Type \"help\" for a list of commands.").queue();
            }
        } catch (Exception e) {
            channel.sendMessage(":x: Error, please see in log.").queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Failed to set new manipulation rule: " + e.getClass().getName() + ", " + e.getMessage());
        }
    }
}
