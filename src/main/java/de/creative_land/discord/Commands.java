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
import com.fasterxml.jackson.databind.ObjectWriter;
import de.creative_land.Configuration;
import de.creative_land.Controller;
import de.creative_land.IgnoredHostname;
import de.creative_land.discord.dispatch.ManipulationRule;
import de.creative_land.discord.dispatch.MentionRoleCooldown;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.PrivateChannel;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A static collection of methods to serve as entry point for external commands.
 * Methods in this class should be seen in isolation.
 */
public class Commands {

    /* Do not instantiate */
    private Commands() {
    }


    //Entry points

    public static void stop(PrivateChannel c, String[] args) {
        if (!assertArgLength(args, 0, c)) {
            return;
        }
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
        if (!assertArgLength(args, 0, c)) {
            return;
        }
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
        if (!assertArgLength(args, 0, c)) {
            return;
        }
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
        if (!assertArgLength(args, 1, c)) {
            return;
        }
        final var controller = Controller.INSTANCE;
        controller.log
                .addLogEntry(String.format("Notice from \"%s\": %s", c.getUser().getName(), args[0]));
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
        if (!assertArgLength(args, 1, c)) {
            return;
        }
        final var connector = DiscordConnector.INSTANCE;
        final var controller = Controller.INSTANCE;
        if (args[0].charAt(0) == '#')
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
        if (!assertArgLength(args, 1, c)) {
            return;
        }
        final var connector = DiscordConnector.INSTANCE;
        final var controller = Controller.INSTANCE;
        String name = args[0];
        try {
            connector.getJda().getSelfUser().getManager().setName(name).queue();
            c.sendMessage(":white_check_mark: New name set.").queue();
            controller.log.addLogEntry(String.format("DiscordConnector: New name set by \"%s\" (Name: \"%s\").",
                    c.getUser().getName(), name));
        } catch (IllegalArgumentException e) {
            c.sendMessage(":x: Error: " + e.getClass().getName() + ", " + e.getMessage()).queue();
        }
    }

    public static void setHostCooldown(PrivateChannel c, String[] args) {
        if (!assertArgLength(args, 1, c)) {
            return;
        }
        final var controller = Controller.INSTANCE;
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
        if (!assertArgLength(args, 2, c)) {
            return;
        }
        final var controller = Controller.INSTANCE;
        try {
            final var minPlayer = Integer.parseInt(args[0]);
            final var hostname = args[1];
            final var reason = args[2];
            if (controller.configuration.addIgnoredHostname(
                    new IgnoredHostname(hostname, c.getUser().getName(), minPlayer, reason))) {
                c.sendMessage(String.format(
                        ":white_check_mark: New ignored host \"%s\" with minimum of %d players and reason \"%s\" added.",
                        hostname, minPlayer, reason)).queue();
                controller.log.addLogEntry(String.format(
                        "New ignored host \"%s\" with minimum of %d players added by \"%s\" with reason \"%s\".",
                        hostname, minPlayer, c.getUser().getName(), reason));
            } else {
                c.sendMessage(":x: Error: This hostname already in the list.").queue();
            }
        } catch (NumberFormatException e) {
            c.sendMessage(":x: Error: Failed to parse integer.").queue();
            controller.log.addLogEntry("DiscordConnector: Failed to set new ignored host cooldown: ", e);
        }
    }

    public static void removeIgnoredHost(PrivateChannel c, String[] args) {
        if (!assertArgLength(args, 1, c)) {
            return;
        }
        final var controller = Controller.INSTANCE;
        final var hostname = args[0];
        if (controller.configuration.removeIgnoredHostname(hostname)) {
            c.sendMessage(":white_check_mark: Host deleted.").queue();
            controller.log.addLogEntry(String.format(
                    "DiscordConnector: Ignored host \"%s\" removed by \"%s\".",
                    hostname, c.getUser().getName()));
        } else {
            c.sendMessage(":x: Error: Hostname not found.").queue();
        }
    }

    public static void addRoleMentionCooldown(PrivateChannel c, String[] args) {
        if (!assertArgLength(args, 2, c)) {
            return;
        }
        final var controller = Controller.INSTANCE;
        try {
            final var role = args[0].charAt(0) == '@' ? args[0].substring(1) : args[0];
            final var cooldown = Integer.parseInt(args[1]);

            if (controller.configuration
                    .addMentionRoleCooldown(new MentionRoleCooldown(role, cooldown, c.getUser().getName()))) {
                c.sendMessage(String.format(
                        ":white_check_mark: New role cooldown set: %d minutes for \"@%s\".",
                        cooldown, role)).queue();
                controller.log.addLogEntry(String.format(
                        "DiscordConnector: New role cooldown set: %d minutes for \"@%s\" by \"%s\".",
                        cooldown, role, c.getUser().getName()));
            } else {
                c.sendMessage(String.format(":x: Error: Role does already exist: \"%s\".", role)).queue();
            }
        } catch (NumberFormatException e) {
            c.sendMessage(":x: Failed to parse integer.").queue();
            controller.log.addLogEntry("DiscordConnector: Failed to set new role cooldown: ", e);
        }
    }

    public static void removeRoleMentionCooldown(PrivateChannel c, String[] args) {
        if (!assertArgLength(args, 1, c)) {
            return;
        }
        final var controller = Controller.INSTANCE;

        final var role = args[0].charAt(0) == '@' ? args[0].substring(1) : args[0];
        if (controller.configuration.removeMentionRoleCooldown(role)) {
            c.sendMessage(String.format(":white_check_mark: Cooldown for role \"%s\" removed.", role)).queue();
            controller.log.addLogEntry(String.format("DiscordConnector: Cooldown for role \"%s\" removed by \"%s\".",
                    role, c.getUser().getName()));
        } else {
            c.sendMessage(String.format(":x: Error: Role does not exist: \"%s\".", role)).queue();
        }
    }

    public static void addManipulationRule(PrivateChannel c, String[] args) {
        if (!assertArgLength(args, 4, c)) {
            return;
        }
        final var controller = Controller.INSTANCE;

        try {
            final var name = args[0];
            final var pattern = args[1];
            final var replacement = args[2];
            final var roles = Arrays.stream(args[3].split(","))
                    .map(s -> s.startsWith("@") ? s.substring(1) : s)
                    .toArray(String[]::new);

            // Fire
            final var manipulationRule = new ManipulationRule(Pattern.compile(pattern), replacement, roles,
                    c.getUser().getName(), name);

            if (controller.configuration.addManipulationRule(manipulationRule)) {
                controller.log.addLogEntry(String.format(
                        "DiscordConnector: New manipulation rule \"%s\" with pattern \"%s\" and replacement \"%s\" added by \"%s\" with following mentions:",
                        manipulationRule.getName(), manipulationRule.getPattern(), manipulationRule.getReplacement(),
                        c.getUser().getName()));

                final StringBuilder sb = new StringBuilder();
                sb.append(String.format(
                        ":white_check_mark: New manipulation rule `%s` with pattern `%s` and replacement `%s` successfully added and mentions the following roles:\n",
                        manipulationRule.getName(), manipulationRule.getPattern(), manipulationRule.getReplacement()));

                if (manipulationRule.getRoles().isEmpty()) {
                    sb.append("\tNo roles are mentioned\n");
                    controller.log.addLogEntry("DiscordConnector: No roles are mentioned.");
                } else {
                    manipulationRule.getRoles().stream()
                            .map(s -> {
                                controller.log.addLogEntry(String.format("DiscordConnector: Mention: %s.", s));
                                return String.format("\t@%s\n", s);
                            })
                            .forEach(s -> sb.append(s));
                }
                c.sendMessage(sb.toString()).queue();
            } else {
                c.sendMessage(":x: Error: This rule already in the list.").queue();
            }
        } catch (Exception e) {
            c.sendMessage(":x: Error, please see in log.").queue();
            controller.log.addLogEntry("DiscordConnector: Failed to set new manipulation rule: ", e);
        }
    }

    public static void removeManipulationRule(PrivateChannel c, String[] args) {
        if (!assertArgLength(args, 1, c)) {
            return;
        }
        final var controller = Controller.INSTANCE;

        final var name = args[0];
        if (controller.configuration.removeManipulationRule(name)) {
            c.sendMessage(":white_check_mark: Manipulation rule deleted.").queue();
            controller.log.addLogEntry(String.format("DiscordConnector: Manipulation rule \"%s\" removed by \"%s\".",
                    name, c.getUser().getName()));
        } else {
            c.sendMessage(":x: Error: Manipulation rule not found.").queue();
        }
    }

    public static void resolveID(PrivateChannel c, String[] args) {
        if (!assertArgLength(args, 1, c)) {
            return;
        }
        final var controller = Controller.INSTANCE;
        final var connector = DiscordConnector.INSTANCE;
        try {
            final var gameReference = connector.dispatcher.getDispatchedMessages().stream()
                    .filter(dispatchedMessage -> dispatchedMessage.getGameReference().id == Integer.parseInt(args[0]))
                    .findFirst();
            if (gameReference.isPresent()) {
                final ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();

                final String formattedGameReference = writer.writeValueAsString(gameReference.get().getGameReference());
                if (formattedGameReference.length() < 1900) {
                    c.sendMessage(String.format("```\n%s\n```", formattedGameReference)).queue();
                } else {
                    c.sendFile(formattedGameReference.getBytes(StandardCharsets.UTF_8), "game reference.txt").queue();
                }
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
        if (!assertArgLength(args, 2, c)) {
            return;
        }
        Controller controller = Controller.INSTANCE;
        try {
            final var engineBuild = Integer.parseInt(args[0]);
            final var engine = args[1];

            controller.configuration.setEngine(engine);
            controller.configuration.setEngineBuild(engineBuild);
            c.sendMessage(String.format(
                    ":white_check_mark: New Clonk version requirement set: \"%s\" on build %d.",
                    engine, engineBuild))
                    .queue();
            controller.log.addLogEntry(String.format(
                    "DiscorConnector: New Clonk version requirement set by \"%s\" (Engine: \"%s\", Build: \"%d\").",
                    c.getUser().getName(), engine, engineBuild));
        } catch (NumberFormatException e) {
            c.sendMessage(":x: Error: Failed to parse integer.").queue();
        } catch (Exception e) {
            c.sendMessage(":x: Error, please see in log.").queue();
            controller.log.addLogEntry("DiscordConnector: Failed to set new Clonk version: ", e);
        }
    }

    public static void removeVersion(PrivateChannel c, String[] args) {
        if (!assertArgLength(args, 0, c)) {
            return;
        }
        Controller controller = Controller.INSTANCE;
        Configuration config = controller.configuration;

        config.setEngine(null);
        config.setEngineBuild(0);
        c.sendMessage(":white_check_mark: Clonk version requirement removed.").queue();
        controller.log.addLogEntry(
                String.format("DiscordConnector: Clonk version requirement removed by %s.", c.getUser().getName()));
    }

    // Helper methods

    /**
     * Asserts that the argument length is exactly the specified length.
     * Optionally writes an error message to the channel.
     *
     * @param args    The argument array.
     * @param length  The length
     * @param channel channel The channel to write an error to. May be null.
     * @return True if the arguments are exactly the specified length.
     */
    private static boolean assertArgLength(String[] args, int length, PrivateChannel channel) {
        return assertArgLength(args, length, length, channel);
    }

    /**
     * Asserts that the argument length is between a higher and a lower bound.
     * Optionally writes an error message to the channel.
     *
     * @param args       The argument array.
     * @param lowerBound The lowest allowed length, inclusive.
     * @param upperBound The highest allowed length, inclusive.
     * @param channel    The channel to write an error to. May be null.
     * @return True if the arguments are between the boundaries.
     */
    private static boolean assertArgLength(String[] args, int lowerBound, int upperBound, PrivateChannel channel) {
        Optional<PrivateChannel> ch = Optional.ofNullable(channel);
        if (args.length < lowerBound) {
            ch.ifPresent(c -> c.sendMessage(":x: Not enough arguments. Type \"help\" for a list of commands.").queue());
            return false;
        }
        if (args.length > upperBound) {
            ch.ifPresent(c -> c.sendMessage(":x: Too many arguments. Type \"help\" for a list of commands.").queue());
            return false;
        }
        return true;
    }
}
