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

package de.creative_land;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.creative_land.clonkspot.ClonkspotConnector;
import de.creative_land.discord.DiscordArguments;
import de.creative_land.discord.DiscordConnector;
import net.dv8tion.jda.api.OnlineStatus;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Controller {
    public static final String VERSION = "1.1";

    public static Controller INSTANCE;

    public final Log log;

    public Configuration configuration;

    private Controller(String joinUrl, String sseEndpoint) {
        INSTANCE = this;
        this.log = new Log();
        //JSON from file to Object
        try {
            ObjectMapper mapper = new ObjectMapper();
            Controller.INSTANCE.log.addLogEntry("Controller: Importing from disk: " + System.getProperty("user.dir") + File.separator + "config.json");
            configuration = mapper.readValue(new File(System.getProperty("user.dir") + File.separator + "config.json"), Configuration.class);
        } catch (IOException e) {
            Controller.INSTANCE.log.addLogEntry("Controller: Error while importing file to disk: " + e.getMessage());
        }
        if (configuration == null) {
            Controller.INSTANCE.log.addLogEntry("Controller: Using new configuration file (Is saved, when the discord login was successful).");
            configuration = new Configuration(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        //Configure global settings
        if (joinUrl == null && Controller.INSTANCE.configuration.getJoinUrl() == null) {
            System.out.println("Controller: No join url in config, ENVs or arguments. Exiting!");
            System.exit(1);
        }
        if (joinUrl != null) Controller.INSTANCE.configuration.setJoinUrl(joinUrl);

        if (sseEndpoint == null && Controller.INSTANCE.configuration.getSseEndpoint() == null) {
            System.out.println("Controller: No SSE endpoint in config, ENVs or arguments. Exiting!");
            System.exit(1);
        }
        if (sseEndpoint != null) Controller.INSTANCE.configuration.setSseEndpoint(sseEndpoint);
    }

    public static void main(String[] args) throws InterruptedException {
        long guildId = parseLong(System.getenv(Argument.GUILD_ID.name()));
        String adminRoleName = System.getenv(Argument.ADMIN_ROLE_NAME.name());
        String apiKey = System.getenv(Argument.KEY.name());
        String joinUrl = System.getenv(Argument.JOIN_URL.name());
        String sseEndpoint = System.getenv(Argument.SSE_ENDPOINT.name());

        if (args.length == 1) {
            printArgumentsHelp();
            System.exit(1);
        }

        try {
            for (int i = 0; i < args.length - 1; i += 2) {

                if (args[i].equalsIgnoreCase(Argument.GUILD_ID.toString())) {
                    guildId = parseLong(args[i + 1]);
                    continue;
                }
                if (args[i].equalsIgnoreCase(Argument.ADMIN_ROLE_NAME.toString())) {
                    adminRoleName = args[i + 1];
                    continue;
                }
                if (args[i].equalsIgnoreCase(Argument.KEY.toString())) {
                    apiKey = args[i + 1];
                    continue;
                }
                if (args[i].equalsIgnoreCase(Argument.JOIN_URL.toString())) {
                    joinUrl = args[i + 1];
                    continue;
                }
                if (args[i].equalsIgnoreCase(Argument.SSE_ENDPOINT.toString())) {
                    sseEndpoint = args[i + 1];
                    continue;
                }
                if (args[i].equalsIgnoreCase(Argument.HELP.toString())) {
                    printArgumentsHelp();
                    System.exit(0);
                }
            }
        } catch (IllegalArgumentException e) {
            printArgumentsHelp();
            System.exit(1);
        }

        final var arguments = new DiscordArguments(guildId, adminRoleName, apiKey);

        new Controller(joinUrl, sseEndpoint);
        new DiscordConnector(arguments);
        new ClonkspotConnector();
        readConsole();
    }

    /**
     * Parses a string into a long.
     *
     * @param string the string to be parsed.
     * @return the parsed long or 0 if parsing failed.
     */
    private static long parseLong(String string) {
        if (string != null) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException e) {
                System.out.println("Controller: Failed to parse guildId (was \"" + string + "\"). Ignoring it.");
            }
        }
        return 0;
    }

    /**
     * Builds a string to print a list of arguments.
     */
    @SuppressWarnings({"SpellCheckingInspection", "StringBufferReplaceableByString"})
    private static void printArgumentsHelp() {
        //System.out.println(Controller.class.getClass().getPackage().getImplementationVersion());
        final var stringBuilder = new StringBuilder();
        final var newline = "\n";
        stringBuilder.append("Clonkspot-Connector v").append(VERSION).append(newline).append(newline);
        stringBuilder.append("Available arguments:").append(newline);
        stringBuilder.append("\t").append("--guildid <number> ------------ Discord server id").append(newline);
        stringBuilder.append("\t").append("--adminrolename <String> ------ Permitted discord group to control the application").append(newline);
        stringBuilder.append("\t").append("--key <String> ---------------- Discord api key").append(newline);
        stringBuilder.append("\t").append("--joinurl <String> ------------ Url which is dispatched for others to join games").append(newline);
        stringBuilder.append("\t").append("--sseendpoint <String> -------- SSE endpoint where the application searches for new games").append(newline);
        stringBuilder.append("\t").append("--help ------------------------ Show this help").append(newline);
        System.out.print(stringBuilder.toString());
    }

    /**
     * Starts a new thread to listen to the console and process incoming commands.
     */
    public static void readConsole() {
        new Thread(() -> {
            String line;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            try {
                while ((line = bufferedReader.readLine()) != null) {

                    if (line.equalsIgnoreCase(Command.EXIT.name())) {
                        DiscordConnector.INSTANCE.getJda().getPresence().setStatus(OnlineStatus.OFFLINE);
                        DiscordConnector.INSTANCE.getJda().shutdown();
                        ClonkspotConnector.INSTANCE.close();
                        System.out.println("Issued shutdown by console");
                        break;
                    } else if (line.equalsIgnoreCase(Command.HELP.name())) {
                        @SuppressWarnings("StringBufferReplaceableByString")
                        final var stringBuilder = new StringBuilder();
                        final var newline = "\n";
                        stringBuilder.append("Available commands:").append(newline);
                        stringBuilder.append("\t").append("exit ------------ Stop the application").append(newline);
                        stringBuilder.append("\t").append("help ------------ Show this help").append(newline);
                        System.out.println(stringBuilder.toString());
                    } else if (line.equals("")) {
                        System.out.println("I am still alive!");
                    } else {
                        System.out.println("Unknown command, type \"help\" for a list of commands.");
                    }
                }
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();
    }
}
