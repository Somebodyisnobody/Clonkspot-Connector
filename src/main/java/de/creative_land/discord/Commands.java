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

/**
 * A static collection of methods to serve as entry point for external commands.
 * Methods in this class should not depend on each other and should be seen in isolation.
 * 
 */
public class Commands {
    
    /* Do not instantiate */
    private Commands() {}

    public static void stop(PrivateChannel c, String[] args) {
        final var connector = DiscordConnector.INSTANCE;
        final var controller = Controller.INSTANCE;
        final var jdaPresence = connector.getJda().getPresence();

        if (Objects.equals(jdaPresence.getActivity(),
                Activity.watching(de.creative_land.discord.Activity.STOPPED.toString()))) {
            c.sendMessage(":x: Service already stopped. No games will be announced.").queue();
        } else if (Objects.equals(connector.status.getCurrentOnlineStatus(), OnlineStatus.DO_NOT_DISTURB)) {
            c.sendMessage(":x: Error, please see in log.").queue();
        } else {
            connector.status.setStopped();
            controller.log
                    .addLogEntry(String.format("DiscordConnector: Service stopped by \"%s\".", c.getUser().getName()));
            c.sendMessage(":white_check_mark: Stopped the service. No games will be announced anymore.").queue();
        }
    }

    public static void start(PrivateChannel c, String[] args) {
        final var connector = DiscordConnector.INSTANCE;
        final var controller = Controller.INSTANCE;
        final var jdaPresence = connector.getJda().getPresence();
        if (Objects.equals(jdaPresence.getActivity(),
                Activity.watching(de.creative_land.discord.Activity.RUNNING.toString()))) {
            c.sendMessage(":x: Service already started. New games will be announced when they appear on clonkspot.")
                    .queue();
        } else if (Objects.equals(connector.status.getCurrentOnlineStatus(), OnlineStatus.DO_NOT_DISTURB)) {
            c.sendMessage(":x: Error, please see in log.").queue();
        } else {
            connector.status.setRunning();
            controller.log
                    .addLogEntry(String.format("DiscordConnector: Service started by \"%s\".", c.getUser().getName()));
            c.sendMessage(
                    ":white_check_mark: Started the service. New games will be announced when they appear on clonkspot.")
                    .queue();
        }
    }

    public static void log(PrivateChannel c, String[] args) {
        final var controller = Controller.INSTANCE;
        try {
            String log = controller.log.printLog();
            if (log.length() + 8 < 2000) {
                c.sendMessage(String.format("```\n%s\n```", log)).queue();
            } else {
                c.sendFile(log.getBytes(StandardCharsets.UTF_8), "log.txt").queue();
            }
        } catch (Exception e) {
            c.sendMessage(":x: Error: " + e.getClass().getName() + ", " + e.getMessage()).queue();
            controller.log.addLogEntry("DiscordConnector: Failed to retrieve log: ", e);
        }
    }

    public static void notice(PrivateChannel c, String[] args) {
        final var controller = Controller.INSTANCE;
        if (args.length == 0) {
            c.sendMessage(":x: Not enough arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        controller.log
                .addLogEntry(String.format("Notice from \"%s\": %s", c.getUser().getName(), String.join(" ", args)));
        c.sendMessage(":white_check_mark: Notice successfully stored in the log.").queue();
    }

    public static void config(PrivateChannel c, String[] args) {
        final var connector = DiscordConnector.INSTANCE;
        final var controller = Controller.INSTANCE;
        final var conf = controller.configuration;
        final var sb = new StringBuilder();
        sb.append("Current configuration:\n");

        try {
            // noinspection ConstantConditions
            sb.append(String.format("Server name: %s\n", connector.getJda().getGuildById(conf.getGuildId()).getName()));
            // noinspection ConstantConditions
            sb.append(String.format("Admin role name: %s\n",
                    connector.getJda().getRoleById(conf.getAdminRole()).getName()));
            sb.append(String.format("Target dispatch channel name: %s\n",
                    connector.getJda().getTextChannelById(conf.getTargetDispatchChannel())));
        } catch (Exception ignored) {
        }
        sb.append(String.format("General host cooldown: %d minutes\n", conf.getHostCooldown()));
        sb.append(String.format("Clonk version: \"%s\" on %d\n", conf.getEngine(), conf.getEngineBuild()));
        sb.append(String.format("SSE Endpoint: \"%s\"\n", conf.getSseEndpoint()));

        sb.append("\n");

        sb.append("Ignored hostnames:\n");
        if (conf.getIgnoredHostnames().isEmpty()) {
            sb.append("\tNo ignored hostname set.\n");
        } else {
            for (final var ignoredHostname : conf.getIgnoredHostnames()) {
                sb.append(String.format("\t\"%s\" under a minimum of %d players by \"%s\" with reason \"%s\".\n",
                        ignoredHostname.getHostname(), ignoredHostname.getMinPlayer(), ignoredHostname.getAuthor(),
                        ignoredHostname.getReason()));
            }
        }
        sb.append("\n");

        sb.append("Mention roles cooldowns:\n");
        if (conf.getMentionRoleCooldowns().isEmpty()) {
            sb.append("\tNo cooldown set.\n");
        } else {
            for (final var mentionRoleCooldown : conf.getMentionRoleCooldowns()) {
                sb.append(String.format("\tRole \"%s\" with cooldown of %d minutes by \"%s\"\n",
                        mentionRoleCooldown.getRole(), mentionRoleCooldown.getCooldown(),
                        mentionRoleCooldown.getAuthor()));
            }
        }
        sb.append("\n");

        sb.append("Manipulation rules:\n");
        if (conf.getManipulationRules().isEmpty()) {
            sb.append("\tNo manipulation rules set.\n");
        } else {
            for (final var manipulationRule : conf.getManipulationRules()) {
                sb.append(String.format(
                        "\tRule \"%s\" with pattern \"%s\" and replacement \"%s\" was set by \"%s\" and mentions the following roles:\n",
                        manipulationRule.getName(), manipulationRule.getPattern(), manipulationRule.getReplacement(),
                        manipulationRule.getAuthor()));

                if (manipulationRule.getRoles().isEmpty()) {
                    sb.append("\t\tNo roles are mentioned.\n");
                } else {
                    for (int i = 0; i < manipulationRule.getRoles().size(); i++) {
                        sb.append(String.format("\t\t@%s\n", manipulationRule.getRoles().get(i)));
                    }
                }
            }
        }
        sb.append("\n");

        final var message = sb.toString();
        try {
            if (message.length() + 8 < 2000) {
                c.sendMessage("```\n" + message + "\n```").queue();
            } else {
                c.sendFile(message.getBytes(StandardCharsets.UTF_8), "config.txt").queue();
            }
        } catch (Exception e) {
            c.sendMessage(":x: Error: " + e.getClass().getName() + ", " + e.getMessage()).queue();
            controller.log.addLogEntry("DiscordConnector: Failed to retrieve config: ", e);
        }
    }

    public static void targetChannel(PrivateChannel c, String[] args) {
        final var connector = DiscordConnector.INSTANCE;
        final var controller = Controller.INSTANCE;
        if (args.length != 1 || args[0].equals("")) {
            c.sendMessage(":x: Not enough or too much arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        if (args[0].charAt(0) == "#".charAt(0))
            args[0] = args[0].substring(1);
        final var textChannels = connector.getJda().getTextChannelsByName(args[0], true);
        if (textChannels.isEmpty()) {
            c.sendMessage(":x: Channel not found.").queue();
        } else if (textChannels.size() > 1) {
            c.sendMessage(":x: Too many channels found (retry case sensitive).").queue();
        } else {
            if (connector.readNewTargetDispatchChannel(textChannels.get(0).getIdLong())) {
                c.sendMessage(":white_check_mark: New target channel set.").queue();
                controller.log.addLogEntry(
                        String.format("DiscordConnector: New target channel set by \"%s\" (Channel: \"#%s\").",
                                c.getUser().getName(), textChannels.get(0).getName()));
                if (Objects.equals(connector.getJda().getPresence().getActivity(),
                        Activity.competing(de.creative_land.discord.Activity.ERROR_NO_CHANNEL.toString()))) {
                    connector.status.setRunning();
                    c.sendMessage(
                            ":white_check_mark: Started the service. New games will be announced when they appear on clonkspot.")
                            .queue();
                    controller.log.addLogEntry(
                            String.format("DiscordConnector: Service started by \"%s\".", c.getUser().getName()));
                }
            }
        }
    }

    public static void newName(PrivateChannel c, String[] args) {
        final var connector = DiscordConnector.INSTANCE;
        final var controller = Controller.INSTANCE;
        if (args.length != 1 || args[0].equals("")) {
            c.sendMessage(":x: Not enough or too much arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        try {
            connector.getJda().getSelfUser().getManager().setName(String.join(" ", args)).queue();
            c.sendMessage(":white_check_mark: New name set.").queue();
            controller.log.addLogEntry(String.format("DiscordConnector: New name set by \"%s\" (Name: \"%s\").",
                    c.getUser().getName(), String.join(" ", args)));
        } catch (IllegalArgumentException e) {
            c.sendMessage(":x: Error: " + e.getClass().getName() + ", " + e.getMessage()).queue();
        }
    }

    public static void setHostCooldown(PrivateChannel c, String[] args) {
        final var controller = Controller.INSTANCE;
        if (args.length != 1 || args[0].equals("")) {
            c.sendMessage(":x: Not enough or too much arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        try {
            final var oldCooldown = controller.configuration.getHostCooldown();
            final var newCooldown = Integer.parseInt(args[0]);
            controller.configuration.setHostCooldown(newCooldown);
            c.sendMessage(String.format(":white_check_mark: Host cooldown set from %d to %d", oldCooldown, newCooldown))
                    .queue();
            controller.log.addLogEntry(
                    String.format("DiscordConnector: New host cooldown set from %d minutes to %d minutes by \"%s\".",
                            oldCooldown, newCooldown, c.getUser().getName()));
        } catch (NumberFormatException e) {
            c.sendMessage(":x: Error: Failed to parse integer.").queue();
            controller.log.addLogEntry("DiscordConnector: Failed to set new host cooldown: ", e);
        }
    }

    public static void addIgnoredHost(PrivateChannel c, String[] args) {
        final var controller = Controller.INSTANCE;
        if (args.length < 2 || args[0].equals("") || args[1].equals("")) {
            c.sendMessage(":x: Not enough arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        try {
            final var joinedArgs = String.join(" ", args);
            String[] arguments = joinedArgs.charAt(0) == '`' ? joinedArgs.substring(1).split("`")
                    : new String[0];
            if (arguments.length == 5 && !arguments[0].equals(" ") && arguments[1].equals(" ")
                    && !arguments[2].equals(" ") && arguments[3].equals(" ") && !arguments[4].equals(" ")) {

                final var minPlayer = Integer.parseInt(arguments[0]);
                if (controller.configuration.addIgnoredHostname(
                        new IgnoredHostname(arguments[2], c.getUser().getName(), minPlayer, arguments[4]))) {
                    c.sendMessage(String.format(
                            ":white_check_mark: New ignored host \"%s\" with minimum of %d players and reason \"%s\" added.",
                            arguments[2], minPlayer, arguments[4])).queue();
                    controller.log.addLogEntry(String.format(
                            "New ignored host \"%s\" with minimum of %d players added by \"%s\" with reason \"%s\".",
                            arguments[2], minPlayer, c.getUser().getName(), arguments[4]));
                } else {
                    c.sendMessage(":x: Error: This hostname already in the list.").queue();
                }

            }
        } catch (NumberFormatException e) {
            c.sendMessage(":x: Error: Failed to parse integer.").queue();
            controller.log.addLogEntry("DiscordConnector: Failed to set new ignored host cooldown: ", e);
        }
    }

    public static void removeIgnoredHost(PrivateChannel c, String[] args) {
        final var controller = Controller.INSTANCE;
        if (args.length < 1) {
            c.sendMessage(":x: Not enough arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        final var hostname = String.join(" ", args);
        if (controller.configuration.removeIgnoredHostname(hostname)) {
            c.sendMessage(":white_check_mark: Host deleted.").queue();
            controller.log.addLogEntry("DiscordConnector: Ignored host \"" + hostname + "\" removed by \""
                    + c.getUser().getName() + "\".");
        } else {
            c.sendMessage(":x: Error: Hostname not found.").queue();
        }
    }

    public static void addRoleMentionCooldown(PrivateChannel c, String[] args) {
        final var controller = Controller.INSTANCE;
        if (args.length != 2 || args[0].equals("") || args[1].equals("")) {
            c.sendMessage(":x: Not enough or too much arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        try {
            if (args[0].charAt(0) == "@".charAt(0))
                args[0] = args[0].substring(1);
            final var cooldown = Integer.parseInt(args[1]);
            if (controller.configuration
                    .addMentionRoleCooldown(new MentionRoleCooldown(args[0], cooldown, c.getUser().getName()))) {
                c.sendMessage(
                        ":white_check_mark: New role cooldown set: " + cooldown + " minutes for \"@" + args[0] + "\".")
                        .queue();
                controller.log.addLogEntry("DiscordConnector: New role cooldown set: " + cooldown + " minutes for \"@"
                        + args[0] + "\" by \"" + c.getUser().getName() + "\".");
            } else {
                c.sendMessage(":x: Error: Role does already exist: \"" + args[0] + "\".").queue();
            }
        } catch (NumberFormatException e) {
            c.sendMessage(":x: Failed to parse integer.").queue();
            controller.log.addLogEntry("DiscordConnector: Failed to set new role cooldown: ", e);
        }
    }

    public static void removeRoleMentionCooldown(PrivateChannel c, String[] args) {
        final var controller = Controller.INSTANCE;
        if (args.length != 1 || args[0].equals("")) {
            c.sendMessage(":x: Not enough or too much arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        if (args[0].charAt(0) == "@".charAt(0))
            args[0] = args[0].substring(1);
        if (controller.configuration.removeMentionRoleCooldown(args[0])) {
            c.sendMessage(String.format(":white_check_mark: Cooldown for role \"%s\" removed.", args[0])).queue();
            controller.log.addLogEntry(String.format("DiscordConnector: Cooldown for role \"%s\" removed by \"%s\".",
                    args[0], c.getUser().getName()));
        } else {
            c.sendMessage(":x: Error: Role does not exist: \"" + args[0] + "\".").queue();
        }
    }

    public static void addManipulationRule(PrivateChannel c, String[] args) {
        final var controller = Controller.INSTANCE;
        try {
            // Parse strings
            final var joinedArgs = String.join(" ", args);
            String[] arguments = joinedArgs.charAt(0) == '`' ? joinedArgs.substring(1).split("`")
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
                if (controller.configuration.addManipulationRule(manipulationRule)) {

                    controller.log.addLogEntry(String.format(
                            "DiscordConnector: New manipulation rule \"%s\" with pattern \"%s\" and replacement \"%s\" added by \"%s\" with following mentions:",
                            manipulationRule.getName(), manipulationRule.getPattern(),
                            manipulationRule.getReplacement(), c.getUser().getName()));

                    final StringBuilder sb = new StringBuilder();
                    sb.append(String.format(
                            ":white_check_mark: New manipulation rule `%s` with pattern `%s` and replacement `%s` successfully added and mentions the following roles:\n",
                            manipulationRule.getName(), manipulationRule.getPattern(),
                            manipulationRule.getReplacement()));
                    if (manipulationRule.getRoles().isEmpty()) {
                        sb.append("\tNo roles are mentioned\n");
                        controller.log.addLogEntry("DiscordConnector: No roles are mentioned.");
                    } else {
                        for (int i = 0; i < manipulationRule.getRoles().size(); i++) {
                            sb.append(String.format("\t@%s\n", manipulationRule.getRoles().get(i)));
                            controller.log.addLogEntry(String.format("DiscordConnector: Mention: @%s.",
                                    manipulationRule.getRoles().get(i)));
                        }
                    }
                    c.sendMessage(sb.toString()).queue();
                } else {
                    c.sendMessage(":x: Error: This rule already in the list.").queue();
                }

            } else {
                c.sendMessage(":x: Not enough arguments. Type \"help\" for a list of commands.").queue();
            }
        } catch (Exception e) {
            c.sendMessage(":x: Error, please see in log.").queue();
            controller.log.addLogEntry("DiscordConnector: Failed to set new manipulation rule: ", e);
        }
    }

    public static void removeManipulationRule(PrivateChannel c, String[] args) {
        final var controller = Controller.INSTANCE;
        if (args.length < 1) {
            c.sendMessage(":x: Not enough arguments. Type \"help\" for a list of commands.").queue();
            return;
        }
        final var name = String.join(" ", args);
        if (controller.configuration.removeManipulationRule(name)) {
            c.sendMessage(":white_check_mark: Manipulation rule deleted.").queue();
            controller.log.addLogEntry(String.format("DiscordConnector: Manipulation rule \"%s\" removed by \"%s\".",
                    name, c.getUser().getName()));
        } else {
            c.sendMessage(":x: Error: Manipulation rule not found.").queue();
        }
    }

    public static void resolveID(PrivateChannel c, String[] args) {
        final var controller = Controller.INSTANCE;
        final var connector = DiscordConnector.INSTANCE;
        try {
            final var gameReference = connector.dispatcher.getDispatchedMessages().stream()
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
            controller.log.addLogEntry("DiscordConnector: Failed to resolve game reference: ", e);
        } catch (JsonProcessingException e) {
            c.sendMessage(":x: Error while parsing GameReferenceModel.").queue();
            controller.log.addLogEntry("Controller: Error while parsing GameReferenceModel: ", e);
        }
    }

    public static void setVersion(PrivateChannel c, String[] args) {
        Controller controller = Controller.INSTANCE;
        try {
            // Parse strings
            final var joinedArgs = String.join(" ", args);
            String[] arguments = joinedArgs.charAt(0) == '`' ? joinedArgs.substring(1).split("`") : new String[0];
            if (arguments.length == 3 && !arguments[0].equals(" ") && arguments[1].equals(" ")
                    && !arguments[2].equals(" ")) {
                final var engineBuild = Integer.parseInt(arguments[2]);
                final var engine = arguments[0].equals("null") ? "" : arguments[0];

                controller.configuration.setEngine(engine);
                controller.configuration.setEngineBuild(engineBuild);
                if (engineBuild == 0 || engine.equals("")) {
                    c.sendMessage(":white_check_mark: Clonk version requirement removed.").queue();
                    controller.log.addLogEntry(String.format(
                            "DiscordConnector: Clonk version requirement removed by %s.", c.getUser().getName()));
                } else {
                    c.sendMessage(
                            String.format(":white_check_mark: New Clonk version requirement set: \"%s\" on build %d.",
                                    engine, engineBuild))
                            .queue();
                    controller.log.addLogEntry(String.format(
                            "DiscorConnector: New Clonk version requirement set by \"%s\" (Engine: \"%s\", Build: \"%d\").",
                            c.getUser().getName(), engine, engineBuild));
                }
            } else {
                c.sendMessage(":x: Not enough arguments. Type \"help\" for a list of commands.").queue();
            }
        } catch (NumberFormatException e) {
            c.sendMessage(":x: Error: Failed to parse integer.").queue();
        } catch (Exception e) {
            c.sendMessage(":x: Error, please see in log.").queue();
            controller.log.addLogEntry("DiscordConnector: Failed to set new Clonk version: ", e);
        }
    }
}
