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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PrivateChannel;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class CommandManager {

    private final Map<String, Command> commands;

    public CommandManager() {
        this.commands = new HashMap<>();

        registerCommand("help", List.of(), "Prints this help message.", this::printHelp);

        registerCommand("stop", List.of(), "Starts the service.", Commands::stop);

        registerCommand("start", List.of(), "Stops the service.", Commands::start);

        registerCommand("log", List.of(), "Prints the log.", Commands::log);

        registerCommand("addnotice", List.of("string"), "Inserts a notice into the log.", Commands::notice);

        registerCommand("config", List.of(), "Prints the current configuration.", Commands::config);

        registerCommand("targetchannel", List.of("channel name"), "Sets a new target channel.",
                Commands::targetChannel);

        registerCommand("newname", List.of("name"), "Sets a new name for the bot.", Commands::newName);

        registerCommand("hostcooldown", List.of("minutes"), "Sets a new general host cooldown for all hosts.",
                Commands::setHostCooldown);

        registerCommand("addignoredhost", List.of("`mininum players`", "`hostname`", "`reason`"),
                "Ignores a host if the number of players isn't reached (case-sensitive).", Commands::addIgnoredHost);

        registerCommand("removeignoredhost", List.of("hostname"),
                "Removes a host from the the ignored hosts list (case-sensitive).", Commands::removeIgnoredHost);

        registerCommand("addmentionrolecooldown", List.of("role", "minutes"),
                "Adds a cooldown for a role mention (case-sensitive).", Commands::addRoleMentionCooldown);

        registerCommand("removementionrolecooldown", List.of("role"),
                "Removes an existing cooldown for role mention (case-sensitive).", Commands::removeRoleMentionCooldown);

        registerCommand("addmanipulationrule", List.of("`name`", "`pattern`", "`replacement`"),
                "Manipulating game titles and mention roles (replacement = regex capture group).",
                Commands::addManipulationRule);

        registerCommand("removemanipulationrule", List.of("name"), "Removes an existing manipulation rule.",
                Commands::removeManipulationRule);

        registerCommand("resolveid", List.of("id"), "Resolves a dispatched game reference by id.", Commands::resolveID);

        registerCommand("clonkversion", List.of("`engine`", "`build version`"),
                "Sets a new Clonk version for the bot which must match on new refrences. Use `null` and 0 for no restriction.",
                Commands::setVersion);
    }

    /**
     * Registers a new command
     *
     * @param key  The key that is used to invoke this command.
     * @param args An array of named arguments used for the help display.
     * @param help The help message explaining the purpose of this command.
     * @param fn   The function to invoke when this command is called.
     */
    private void registerCommand(String key, List<String> args, String help, BiConsumer<PrivateChannel, String[]> fn) {
        commands.put(key, Command.of(key, args, help, fn));
    }

    /**
     * Selects and runs the right command.
     *
     * @param string  full command string.
     * @param channel the user ({@link PrivateChannel}) who issued the command.
     */
    public void selectAndPerformCommand(String string, PrivateChannel channel) {
        String[] values = string.split(" ", 2);
        if (values.length < 2) {
            values = new String[]{values[0], ""};
        }

        String key = values[0];
        String[] args = values[1].split(" ");

        var command = Optional.ofNullable(commands.get(key))
                .orElseGet(() -> Command.of("invalid", List.of(), "Invalid command message.",
                        (c, a) -> c.sendMessage("Command not found. Type \"help\" for a list of commands.").queue()));

        command.fn.accept(channel, args);
    }

    /**
     * Checks if the given user is permitted to issue commands.
     *
     * @param member the user ({@link Member})
     * @return true if the user is permitted, otherwise false.
     */
    public boolean checkAdmin(Member member) {
        for (var role : member.getRoles()) {
            if (role.getIdLong() == DiscordConnector.INSTANCE.getAdminRole().getIdLong()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Formats a help message with all registered commands and outputs it to the
     * specified channel.
     *
     * @param c    The channel.
     * @param args The arguments.
     */
    private void printHelp(PrivateChannel c, String[] args) {
        final var message = "Available commands:\n" + commands.values().stream()
                .map(s -> {
                    String a = s.args.isEmpty() ? "" : s.args.stream().collect(Collectors.joining("> <", "<", ">"));
                    return String.format("%-30s %-30s %s", s.key, a, s.help);
                }).collect(Collectors.joining("\n"));
        try {
            if (message.length() < 2000) {
                c.sendMessage("```\n" + message + "\n```").queue();
            } else {
                c.sendFile(message.getBytes(StandardCharsets.UTF_8), "help.txt").queue();
            }
        } catch (Exception e) {
            c.sendMessage(":x: Error: " + e.getClass().getName() + ", " + e.getMessage()).queue();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: Failed to print help command: ", e);
        }
    }

    /**
     * Small helper class to collect all information about a command
     */
    private static class Command {
        /**
         * the key
         */
        public final String key;

        /**
         * the arguments
         */
        public final List<String> args;

        /**
         * the help message
         */
        public final String help;

        /**
         * The function
         */
        public final BiConsumer<PrivateChannel, String[]> fn;

        /**
         * constructor
         */
        private Command(String key, List<String> args, String help, BiConsumer<PrivateChannel, String[]> fn) {
            this.key = key;
            this.args = args;
            this.help = help;
            this.fn = fn;
        }

        /**
         * Builds a new command object
         */
        public static Command of(String key, List<String> args, String help, BiConsumer<PrivateChannel, String[]> fn) {
            return new Command(key, args, help, fn);
        }
    }
}
