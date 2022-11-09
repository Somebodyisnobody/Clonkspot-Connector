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
import de.creative_land.discord.clonk_game_reference.Dispatcher;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;

import javax.security.auth.login.LoginException;

/**
 *
 */
public class DiscordConnector {

    public static DiscordConnector INSTANCE;

    public final Dispatcher gameDispatcher;

    public final Status status;

    public final CommandManager commandManager;

    private JDA jda;

    private Guild guild;

    private Role adminRole;

    private TextChannel gameReferenceDispatchChannel;

    public DiscordConnector(DiscordArguments discordArguments) throws InterruptedException {
        INSTANCE = this;

        discordLogin(discordArguments.getApiKey());

        jda.awaitStatus(JDA.Status.CONNECTED);
        Controller.INSTANCE.log.addLogEntry("DiscordConnector: JDA connected.");
        Controller.INSTANCE.configuration.saveConfig();


        this.status = new Status();
        scanEnvironment(discordArguments);
        this.gameDispatcher = new Dispatcher();
        this.commandManager = new CommandManager();
    }

    /**
     * Creates a {@link JDA}. If no api key is set via jvm-args or system-env it's being read from the config. On fail the application exits.
     *
     * @param apiKey Discord api key.
     */
    private void createJda(String apiKey) throws LoginException {
        JDABuilder builder = JDABuilder.createDefault(apiKey);
        builder.addEventListeners(new ChatListener());
        builder.setChunkingFilter(ChunkingFilter.ALL);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS);
        jda = builder.build();
    }

    /**
     * Sets up the api key and creates a {@link JDA}. If no api key is set via jvm-args or system-envs it's being read from the config. On fail the application exits.
     *
     * @param apiKey Discord api key.
     */
    private void discordLogin(String apiKey) {
        if (apiKey != null) {
            try {
                System.out.println("DiscordConnector: Using new api key.");
                createJda(apiKey);
                Controller.INSTANCE.configuration.setApiKey(apiKey);
            } catch (LoginException loginException) {
                System.out.println("DiscordConnector: New api key is invalid, upstream denied. Exiting!");
                System.exit(12);
            }
        } else if ((apiKey = Controller.INSTANCE.configuration.getApiKey()) != null) {
            try {
                createJda(apiKey);
            } catch (LoginException loginException) {
                System.out.println("DiscordConnector: Stored api key is invalid, upstream denied. Please provide a new key. Exiting!");
                System.exit(12);
            }
        } else {
            System.out.println("DiscordConnector: No api key in config, ENVs or arguments. Exiting!");
            System.exit(11);
        }
    }

    /**
     * Checks in following order:
     * <pre>
     * Is there only one server? If yes:
     * Is there an Admin role? or:
     * Is there an target channel? if yes to both:
     * Sets the status on "Running"
     * </pre>
     *
     * @param discordArguments {@link DiscordArguments} like jvm-args or system-envs used to set up the application. If null, values from config are used.
     */
    public void scanEnvironment(DiscordArguments discordArguments) {
        if (!readNewGuild(discordArguments == null ? 0 : discordArguments.getGuildId())) {
            status.setErrNoGuild();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: New status: ERROR_NO_GUILD.");
        } else if (!readNewAdminRole(discordArguments == null ? null : discordArguments.getAdminRoleName())) {
            status.setErrNoAdminRole();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: New status: ERROR_NO_ADMIN_ROLE.");
        } else if (!readNewGameReferenceDispatchChannel(0)) {
            status.setErrNoChannel();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: New status: ERROR_NO_CHANNEL.");
        } else {
            status.setRunning();
            Controller.INSTANCE.log.addLogEntry("DiscordConnector: New status: RUNNING.");
        }
    }

    /**
     * Sets a new channel for dispatching messages.
     *
     * @param newChannel the id of the new channel.
     * @return true if setting the new channel was successful, false if not.
     */
    public boolean readNewGameReferenceDispatchChannel(long newChannel) {
        if (newChannel > 0) {
            final var newDispatchChannel = jda.getTextChannelById(newChannel);
            if (newDispatchChannel != null) {
                gameReferenceDispatchChannel = newDispatchChannel;
                Controller.INSTANCE.configuration.setTargetDispatchChannel(newChannel);
                return true;
            }
        } else {
            final var newDispatchChannel = jda.getTextChannelById(Controller.INSTANCE.configuration.getTargetDispatchChannel());
            if (newDispatchChannel != null) {
                gameReferenceDispatchChannel = newDispatchChannel;
                return true;
            }
        }
        return false;
    }

    /**
     * Sets a new role which is used to authenticate admins.
     *
     * @param adminRoleName name of the new admin role.
     * @return true if setting the new admin role was successful, false if not.
     */
    public boolean readNewAdminRole(String adminRoleName) {
        if (adminRoleName != null) {
            final var adminRoles = jda.getRolesByName(adminRoleName, false);
            if (adminRoles.size() != 1) {
                return false;
            }
            adminRole = adminRoles.get(0);
            Controller.INSTANCE.configuration.setAdminRole(adminRole.getIdLong());
        } else {
            adminRole = jda.getRoleById(Controller.INSTANCE.configuration.getAdminRole());
            return adminRole != null;
        }
        return true;
    }

    /**
     * Sets a new guild (server) for the application.
     *
     * @param guildId the id of the new channel.
     * @return true if setting the new server was successful, false if not.
     */
    public boolean readNewGuild(long guildId) {
        if (guildId != 0) {
            guild = jda.getGuildById(guildId);
            if (guild == null) {
                return false;
            }
            Controller.INSTANCE.configuration.setGuildId(guildId);
            return true;
        } else {
            guild = jda.getGuildById(Controller.INSTANCE.configuration.getGuildId());
            return guild != null;
        }
    }

    public JDA getJda() {
        return jda;
    }

    public Guild getGuild() {
        return guild;
    }

    public Role getAdminRole() {
        return adminRole;
    }

    public TextChannel getGameReferenceDispatchChannel() {
        return gameReferenceDispatchChannel;
    }
}