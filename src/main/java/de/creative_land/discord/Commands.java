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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.creative_land.Controller;
import de.creative_land.IgnoredHostname;
import de.creative_land.discord.dispatch.ManipulationRule;
import de.creative_land.discord.dispatch.MentionRoleCooldown;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.PrivateChannel;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Pattern;

public class Commands {

    public static void stop(PrivateChannel c, String[] args) {
        final var jdaPresence = DiscordConnector.INSTANCE.getJda().getPresence();
        if (Objects.equals(jdaPresence.getActivity(),
                Activity.watching(de.creative_land.discord.Activity.STOPPED.toString()))) {
            c.sendMessage(":x: Service already stopped. No games will be announced.").queue();
        } else if (Objects.equals(DiscordConnector.INSTANCE.status.getCurrentOnlineStatus(),
                OnlineStatus.DO_NOT_DISTURB)) {
            c.sendMessage(":x: Error, please see in log.").queue();
        } else {
            DiscordConnector.INSTANCE.status.setStopped();
            Controller.INSTANCE.log
                    .addLogEntry("DiscordConnector: Service stopped by \"" + c.getUser().getName() + "\".");
            c.sendMessage(":white_check_mark: Stopped the service. No games will be announced anymore.").queue();
        }
    }

    public static void start(PrivateChannel c, String[] args) {
        final var jdaPresence = DiscordConnector.INSTANCE.getJda().getPresence();
        if (Objects.equals(jdaPresence.getActivity(),
                Activity.watching(de.creative_land.discord.Activity.RUNNING.toString()))) {
            c.sendMessage(":x: Service already started. New games will be announced when they appear on clonkspot.")
                    .queue();
        } else if (Objects.equals(DiscordConnector.INSTANCE.status.getCurrentOnlineStatus(),
                OnlineStatus.DO_NOT_DISTURB)) {
            c.sendMessage(":x: Error, please see in log.").queue();
        } else {
            DiscordConnector.INSTANCE.status.setRunning();
            Controller.INSTANCE.log
                    .addLogEntry("DiscordConnector: Service started by \"" + c.getUser().getName() + "\".");
            c.sendMessage(
                    ":white_check_mark: Started the service. New games will be announced when they appear on clonkspot.")
                    .queue();
        }
    }

    public static void log(PrivateChannel c, String[] args) {
        try {
            String log = Controller.INSTANCE.log.printLog();
            if (log.length() < 2000) {
                c.sendMessage("```\n" + log + "\n```").queue();
            } else {
                c.sendFile(log.getBytes(StandardCharsets.UTF_8), "log.txt").queue();
            }
        } catch (Exception e) {
            c.sendMessage(":x: Error: " + e.getClass().getName() + ", " + e.getMessage()).queue();
            Controller.INSTANCE.log.addLogEntry(
                    "DiscordConnector: Failed to retrieve log: " + e.getClass().getName() + ", " + e.getMessage());
        }
    }

    public static void notice(PrivateChannel c, String[] args) {
        if (args.length == 0) {
            c.sendMessage(":x: Not enough arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        Controller.INSTANCE.log.addLogEntry("Notice from \"" + c.getUser().getName() + "\": " + String.join(" ", args));
        c.sendMessage(":white_check_mark: Notice successfully stored in the log.").queue();
    }

    public static void config(PrivateChannel c, String[] args) {
        final var configuration = Controller.INSTANCE.configuration;
        final var stringBuilder = new StringBuilder();
        final var newline = "\n";
        stringBuilder.append("Current configuration:").append(newline);

        try {
            // noinspection ConstantConditions
            stringBuilder.append("Server name: ")
                    .append(DiscordConnector.INSTANCE.getJda().getGuildById(configuration.getGuildId()).getName())
                    .append(newline);
            // noinspection ConstantConditions
            stringBuilder.append("Admin role name: ")
                    .append(DiscordConnector.INSTANCE.getJda().getRoleById(configuration.getAdminRole()).getName())
                    .append(newline);
            stringBuilder.append("Target dispatch channel name: ").append(
                    DiscordConnector.INSTANCE.getJda().getTextChannelById(configuration.getTargetDispatchChannel()))
                    .append(newline);
        } catch (Exception ignored) {
        }
        stringBuilder.append("General host cooldown: ").append(configuration.getHostCooldown()).append(" minutes")
                .append(newline);
        stringBuilder.append("Clonk version: \"").append(configuration.getEngine()).append("\" on ")
                .append(configuration.getEngineBuild()).append(newline);
        stringBuilder.append("SSE Endpoint: \"").append(configuration.getSseEndpoint()).append(newline);

        stringBuilder.append(newline);
        stringBuilder.append("Ignored hostnames:").append(newline);
        if (configuration.getIgnoredHostnames().isEmpty()) {
            stringBuilder.append("\tNo ignored hostname set").append(newline);
        } else {
            for (final var ignoredHostname : configuration.getIgnoredHostnames()) {
                stringBuilder.append("\t\"").append(ignoredHostname.getHostname()).append("\" under a minimum of ")
                        .append(ignoredHostname.getMinPlayer()).append(" players by \"")
                        .append(ignoredHostname.getAuthor()).append("\" with reason \"")
                        .append(ignoredHostname.getReason()).append("\".").append(newline);
            }
        }
        stringBuilder.append(newline);

        stringBuilder.append("Mention roles cooldowns:").append(newline);
        if (configuration.getMentionRoleCooldowns().isEmpty()) {
            stringBuilder.append("\tNo cooldown set").append(newline);
        } else {
            for (final var mentionRoleCooldown : configuration.getMentionRoleCooldowns()) {
                stringBuilder.append("\tRole \"").append(mentionRoleCooldown.getRole()).append("\" with cooldown of ")
                        .append(mentionRoleCooldown.getCooldown()).append(" minutes by \"")
                        .append(mentionRoleCooldown.getAuthor()).append("\"").append(newline);
            }
        }
        stringBuilder.append(newline);

        stringBuilder.append("Manipulation rules:").append(newline);
        if (configuration.getManipulationRules().isEmpty()) {
            stringBuilder.append("\tNo manipulation rules set").append(newline);
        } else {
            for (final var manipulationRule : configuration.getManipulationRules()) {
                stringBuilder.append("\tRule \"").append(manipulationRule.getName()).append("\" with pattern \"")
                        .append(manipulationRule.getPattern().toString()).append("\" and replacement \"")
                        .append(manipulationRule.getReplacement()).append("\" was set by \"")
                        .append(manipulationRule.getAuthor()).append("\" and mentions the following roles:")
                        .append(newline);
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
                c.sendMessage("```\n" + message + "\n```").queue();
            } else {
                c.sendFile(message.getBytes(StandardCharsets.UTF_8), "config.txt").queue();
            }
        } catch (Exception e) {
            c.sendMessage(":x: Error: " + e.getClass().getName() + ", " + e.getMessage()).queue();
            Controller.INSTANCE.log.addLogEntry(
                    "DiscordConnector: Failed to retrieve config: " + e.getClass().getName() + ", " + e.getMessage());
        }
    }

    public static void targetChannel(PrivateChannel c, String[] args) {
        if (args.length != 1 || args[0].equals("")) {
            c.sendMessage(":x: Not enough or too much arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        if (args[0].charAt(0) == "#".charAt(0))
            args[0] = args[0].substring(1);
        final var textChannels = DiscordConnector.INSTANCE.getJda().getTextChannelsByName(args[0], true);
        if (textChannels.isEmpty()) {
            c.sendMessage(":x: Channel not found.").queue();
        } else if (textChannels.size() > 1) {
            c.sendMessage(":x: Too many channels found (retry case sensitive).").queue();
        } else {
            if (DiscordConnector.INSTANCE.readNewTargetDispatchChannel(textChannels.get(0).getIdLong())) {
                c.sendMessage(":white_check_mark: New target channel set.").queue();
                Controller.INSTANCE.log.addLogEntry("DiscordConnector: New target channel set by \""
                        + c.getUser().getName() + "\" (Channel: \"#" + textChannels.get(0).getName() + "\").");
                if (Objects.equals(DiscordConnector.INSTANCE.getJda().getPresence().getActivity(),
                        Activity.competing(de.creative_land.discord.Activity.ERROR_NO_CHANNEL.toString()))) {
                    DiscordConnector.INSTANCE.status.setRunning();
                    c.sendMessage(
                            ":white_check_mark: Started the service. New games will be announced when they appear on clonkspot.")
                            .queue();
                    Controller.INSTANCE.log
                            .addLogEntry("DiscordConnector: Service started by \"" + c.getUser().getName() + "\".");
                }
            }
        }
    }

    public static void newName(PrivateChannel c, String[] args) {
        if (args.length != 1 || args[0].equals("")) {
            c.sendMessage(":x: Not enough or too much arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        try {
            DiscordConnector.INSTANCE.getJda().getSelfUser().getManager().setName(String.join(" ", args)).queue();
            c.sendMessage(":white_check_mark: New name set.").queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: New name set by \"" + c.getUser().getName()
                    + "\" (Name: \"" + String.join(" ", args) + "\").");
        } catch (IllegalArgumentException e) {
            c.sendMessage(":x: Error: " + e.getClass().getName() + ", " + e.getMessage()).queue();
        }
    }

    public static void setHostCooldown(PrivateChannel c, String[] args) {
        if (args.length != 1 || args[0].equals("")) {
            c.sendMessage(":x: Not enough or too much arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        try {
            final var oldCooldown = Controller.INSTANCE.configuration.getHostCooldown();
            final var newCooldown = Integer.parseInt(args[0]);
            Controller.INSTANCE.configuration.setHostCooldown(newCooldown);
            c.sendMessage(":white_check_mark: Host cooldown set from " + oldCooldown + " to " + newCooldown).queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: New host cooldown set from " + oldCooldown
                    + " minutes to " + newCooldown + " minutes by \"" + c.getUser().getName() + "\".");
        } catch (NumberFormatException e) {
            c.sendMessage(":x: Error: Failed to parse integer.").queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Failed to set new host cooldown: "
                    + e.getClass().getName() + ", " + e.getMessage());
        }
    }

    public static void addIgnoredHost(PrivateChannel c, String[] args) {
        if (args.length < 2 || args[0].equals("") || args[1].equals("")) {
            c.sendMessage(":x: Not enough arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        try {
            final var joinedArgs = String.join(" ", args);
            String[] arguments = joinedArgs.charAt(0) == "`".toCharArray()[0] ? joinedArgs.substring(1).split("`")
                    : new String[0];
            if (arguments.length == 5 && !arguments[0].equals(" ") && arguments[1].equals(" ")
                    && !arguments[2].equals(" ") && arguments[3].equals(" ") && !arguments[4].equals(" ")) {

                final var minPlayer = Integer.parseInt(arguments[0]);
                if (Controller.INSTANCE.configuration.addIgnoredHostname(
                        new IgnoredHostname(arguments[2], c.getUser().getName(), minPlayer, arguments[4]))) {
                    c.sendMessage(":white_check_mark: New ignored host \"" + arguments[2] + "\" with minimum of "
                            + minPlayer + " players and reason \"" + arguments[4] + "\" added").queue();
                    Controller.INSTANCE.log.addLogEntry("DiscordConnector: New ignored host \"" + arguments[2]
                            + "\" with minimum of " + minPlayer + " players added by \"" + c.getUser().getName()
                            + "\" with reason \"" + arguments[4] + "\".");
                } else {
                    c.sendMessage(":x: Error: This hostname already in the list.").queue();
                }

            }
        } catch (NumberFormatException e) {
            c.sendMessage(":x: Error: Failed to parse integer.").queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Failed to set new ignored host cooldown: "
                    + e.getClass().getName() + ", " + e.getMessage());
        }
    }

    public static void removeIgnoredHost(PrivateChannel c, String[] args) {
        if (args.length < 1) {
            c.sendMessage(":x: Not enough arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        final var hostname = String.join(" ", args);
        if (Controller.INSTANCE.configuration.removeIgnoredHostname(hostname)) {
            c.sendMessage(":white_check_mark: Host deleted.").queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Ignored host \"" + hostname + "\" removed by \""
                    + c.getUser().getName() + "\".");
        } else {
            c.sendMessage(":x: Error: Hostname not found.").queue();
        }
    }

    public static void addRoleMentionCooldown(PrivateChannel c, String[] args) {
        if (args.length != 2 || args[0].equals("") || args[1].equals("")) {
            c.sendMessage(":x: Not enough or too much arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        try {
            if (args[0].charAt(0) == "@".charAt(0))
                args[0] = args[0].substring(1);
            final var cooldown = Integer.parseInt(args[1]);
            if (Controller.INSTANCE.configuration
                    .addMentionRoleCooldown(new MentionRoleCooldown(args[0], cooldown, c.getUser().getName()))) {
                c.sendMessage(
                        ":white_check_mark: New role cooldown set: " + cooldown + " minutes for \"@" + args[0] + "\".")
                        .queue();
                Controller.INSTANCE.log.addLogEntry("DiscordConnector: New role cooldown set: " + cooldown
                        + " minutes for \"@" + args[0] + "\" by \"" + c.getUser().getName() + "\".");
            } else {
                c.sendMessage(":x: Error: Role does already exist: \"" + args[0] + "\".").queue();
            }
        } catch (NumberFormatException e) {
            c.sendMessage(":x: Failed to parse integer.").queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Failed to set new role cooldown: "
                    + e.getClass().getName() + ", " + e.getMessage());
        }
    }

    public static void removeRoleMentionCooldown(PrivateChannel c, String[] args) {
        if (args.length != 1 || args[0].equals("")) {
            c.sendMessage(":x: Not enough or too much arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        if (args[0].charAt(0) == "@".charAt(0))
            args[0] = args[0].substring(1);
        if (Controller.INSTANCE.configuration.removeMentionRoleCooldown(args[0])) {
            c.sendMessage(":white_check_mark: Cooldown for role \"" + args[0] + "\" removed.").queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Cooldown for role \"" + args[0] + "\" removed by \""
                    + c.getUser().getName() + "\".");
        } else {
            c.sendMessage(":x: Error: Role does not exist: \"" + args[0] + "\".").queue();
        }
    }

    public static void addManipulationRule(PrivateChannel c, String[] args) {
        try {
            // Parse strings
            final var joinedArgs = String.join(" ", args);
            String[] arguments = joinedArgs.charAt(0) == "`".toCharArray()[0] ? joinedArgs.substring(1).split("`")
                    : new String[0];
            if (arguments.length == 7 && !arguments[0].equals(" ") && arguments[1].equals(" ")
                    && !arguments[2].equals(" ") && arguments[3].equals(" ") && !arguments[4].equals(" ")
                    && arguments[5].equals(" ") && !arguments[6].equals(" ")) {

                // Remove @
                String[] roles = arguments[6].split(",");
                for (int i = 0; i < roles.length; i++) {
                    if (roles[i].charAt(0) == "@".charAt(0))
                        roles[i] = roles[i].substring(1);
                }

                // Fire
                final var manipulationRule = new ManipulationRule(Pattern.compile(arguments[2]), arguments[4], roles,
                        c.getUser().getName(), arguments[0]);
                if (Controller.INSTANCE.configuration.addManipulationRule(manipulationRule)) {

                    Controller.INSTANCE.log.addLogEntry("DiscordConnector: New manipulation rule \""
                            + manipulationRule.getName() + "\" with with pattern \"" + manipulationRule.getPattern()
                            + "\" and replacement \"" + manipulationRule.getReplacement() + "\" added by \""
                            + c.getUser().getName() + "\" with following mentions:");

                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(":white_check_mark: New manipulation rule `")
                            .append(manipulationRule.getName()).append("` with pattern `")
                            .append(manipulationRule.getPattern().toString()).append("` and replacement `")
                            .append(manipulationRule.getReplacement())
                            .append("` successfully added and mentions the following roles:").append("\n");
                    if (manipulationRule.getRoles().isEmpty()) {
                        stringBuilder.append("\tNo roles are mentioned").append("\n");
                        Controller.INSTANCE.log.addLogEntry("DiscordConnector: No roles are mentioned.");
                    } else {
                        for (int i = 0; i < manipulationRule.getRoles().size(); i++) {
                            stringBuilder.append("\t@").append(manipulationRule.getRoles().get(i)).append("\n");
                            Controller.INSTANCE.log.addLogEntry(
                                    "DiscordConnector: Mention: @" + manipulationRule.getRoles().get(i) + ".");
                        }
                    }
                    c.sendMessage(stringBuilder.toString()).queue();
                } else {
                    c.sendMessage(":x: Error: This rule already in the list.").queue();
                }

            } else {
                c.sendMessage(":x: Not enough arguments. Type \"help\" for a list of commands.").queue();
            }
        } catch (Exception e) {
            c.sendMessage(":x: Error, please see in log.").queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Failed to set new manipulation rule: "
                    + e.getClass().getName() + ", " + e.getMessage());
        }
    }

    public static void removeManipulationRule(PrivateChannel c, String[] args) {
        if (args.length < 1) {
            c.sendMessage(":x: Not enough arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        final var name = String.join(" ", args);
        if (Controller.INSTANCE.configuration.removeManipulationRule(name)) {
            c.sendMessage(":white_check_mark: Manipulation rule deleted.").queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Manipulation rule \"" + name + "\" removed by \""
                    + c.getUser().getName() + "\".");
        } else {
            c.sendMessage(":x: Error: Manipulation rule not found.").queue();
        }
    }

    public static void resolveID(PrivateChannel c, String[] args) {
        try {
            final var gameReference = DiscordConnector.INSTANCE.dispatcher.getDispatchedMessages().stream()
                    .filter(dispatchedMessage -> dispatchedMessage.getGameReference().id == Integer.parseInt(args[0]))
                    .findFirst();
            if (gameReference.isPresent()) {
                ObjectMapper mapper = new ObjectMapper();
                c.sendMessage(mapper.writeValueAsString(gameReference.get().getGameReference())).queue();
            } else {
                c.sendMessage(
                        "No game reference matched that id. It was not dispatched or is no longer available in cache.")
                        .queue();
            }
        } catch (NumberFormatException e) {
            c.sendMessage(":x: Error: Failed to parse integer.").queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Failed to resolve game reference: "
                    + e.getClass().getName() + ", " + e.getMessage());
        } catch (JsonProcessingException e) {
            c.sendMessage(":x: Error while parsing GameReferenceModel.").queue();
            Controller.INSTANCE.log.addLogEntry("Controller: Error while parsing GameReferenceModel: "
                    + e.getClass().getName() + ", " + e.getMessage());
        }
    }

    public static void setVersion(PrivateChannel c, String[] args) {
        try {
            // Parse strings
            final var joinedArgs = String.join(" ", args);
            String[] arguments = joinedArgs.charAt(0) == "`".toCharArray()[0] ? joinedArgs.substring(1).split("`")
                    : new String[0];
            if (arguments.length == 3 && !arguments[0].equals(" ") && arguments[1].equals(" ")
                    && !arguments[2].equals(" ")) {
                final var engineBuild = Integer.parseInt(arguments[2]);
                final var engine = arguments[0].equals("null") ? "" : arguments[0];

                Controller.INSTANCE.configuration.setEngine(engine);
                Controller.INSTANCE.configuration.setEngineBuild(engineBuild);
                if (engineBuild == 0 || engine.equals("")) {
                    c.sendMessage(":white_check_mark: Clonk version requirement removed.").queue();
                    Controller.INSTANCE.log.addLogEntry("DiscordConnector: Clonk version requirement removed by \""
                            + c.getUser().getName() + "\").");
                } else {
                    c.sendMessage(":white_check_mark: New Clonk version requirement set: \"" + engine + "\" on build "
                            + engineBuild + ".").queue();
                    Controller.INSTANCE.log.addLogEntry(
                            "DiscordConnector: New Clonk version requirement set by \"" + c.getUser().getName()
                                    + "\" (Engine: \"" + engine + "\", Build: \"" + engineBuild + "\").");
                }
            } else {
                c.sendMessage(":x: Not enough arguments. Type \"help\" for a list of commands.").queue();
            }
        } catch (NumberFormatException e) {
            c.sendMessage(":x: Error: Failed to parse integer.").queue();
        } catch (Exception e) {
            c.sendMessage(":x: Error, please see in log.").queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Failed to set new Clonk version: "
                    + e.getClass().getName() + ", " + e.getMessage());
        }
    }
}
