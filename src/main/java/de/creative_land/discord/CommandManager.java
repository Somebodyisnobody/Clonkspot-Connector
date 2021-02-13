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

import de.creative_land.Command;
import de.creative_land.discord.commands.*;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PrivateChannel;

import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class CommandManager {
    public final LinkedHashMap<Command, ServerCommand> commands;

    public CommandManager() {
        this.commands = new LinkedHashMap<>();

        this.commands.put(Command.STOP, new StopCommand());
        this.commands.put(Command.START, new StartCommand());
        this.commands.put(Command.LOG, new LogCommand());
        this.commands.put(Command.ADDNOTICE, new AddNoticeCommand());
        this.commands.put(Command.CONFIG, new ConfigCommand());
        this.commands.put(Command.TARGETCHANNEL, new TargetChannelCommand());
        this.commands.put(Command.NEWNAME, new NewNameCommand());
        this.commands.put(Command.HOSTCOOLDOWN, new HostCooldownCommand());
        this.commands.put(Command.ADDIGNOREDHOST, new AddIgnoredHostCommand());
        this.commands.put(Command.REMOVEIGNOREDHOST, new RemoveIgnoredHostCommand());
        this.commands.put(Command.ADDMENTIONROLECOOLDOWN, new AddMentionRoleCooldownCommand());
        this.commands.put(Command.REMOVEMENTIONROLECOOLDOWN, new RemoveMentionRoleCooldownCommand());
        this.commands.put(Command.ADDMANIPULATIONRULE, new AddManipulationRuleCommand());
        this.commands.put(Command.REMOVEMANIPULATIONRULE, new RemoveManipulationRuleCommand());
        this.commands.put(Command.RESOLVEID, new ResolveIdCommand());
        this.commands.put(Command.CLONKVERSION, new ClonkVersionCommand());
    }

    /**
     * Selects and runs the right command.
     *
     * @param command full command string.
     * @param channel the user ({@link PrivateChannel}) who issued the command.
     */
    public void selectAndPerformCommand(String command, PrivateChannel channel) {

        int delimiter = command.indexOf(' ');
        String name = delimiter > 0 ? command.substring(0, delimiter) : command;
        String[] arguments = delimiter > 0 ? command.substring(delimiter + 1).split(" ") : new String[0];

        ServerCommand serverCommand = null;
        try {
            serverCommand = commands.get(Command.valueOf(name.toUpperCase()));
        } catch (IllegalArgumentException ignored) {
        }
        if (serverCommand != null) {
            serverCommand.performCommand(channel, arguments);
        } else if (name.equalsIgnoreCase("help")) {
            channel.sendMessage("```\nAvailable commands:\n" + commands.keySet().stream().map(Command::toString).collect(Collectors.joining("\n")) + "\n```").queue();
        } else {
            channel.sendMessage("Command not found. Type \"help\" for a list of commands.").queue();
        }

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
}
