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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.creative_land.discord.DiscordConnector;
import de.creative_land.discord.clonk_game_reference.ManipulationRule;
import de.creative_land.discord.clonk_game_reference.MentionRoleCooldown;
import net.dv8tion.jda.api.JDA;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {

    private final ArrayList<ManipulationRule> manipulationRules;

    private final ArrayList<IgnoredHostname> ignoredHostnames;

    private final ArrayList<MentionRoleCooldown> mentionRoleCooldowns;

    private String apiKey;

    private long targetDispatchChannel;

    private long adminRole;

    private long guildId;

    /**
     * Cooldown in minutes.
     */
    private int hostCooldown;

    private String engine;

    private int engineBuild;

    private String joinUrl;

    private String sseEndpoint;

    public Configuration(@JsonProperty("manipulationRules") ArrayList<ManipulationRule> manipulationRules, @JsonProperty("ignoredHostnames") ArrayList<IgnoredHostname> ignoredHostnames, @JsonProperty("mentionRoleCooldowns") ArrayList<MentionRoleCooldown> mentionRoleCooldowns) {
        this.manipulationRules = manipulationRules;
        this.ignoredHostnames = ignoredHostnames;
        this.mentionRoleCooldowns = mentionRoleCooldowns;
    }

    private void saveConfig() {
        //Dont save until system booted
        if (DiscordConnector.INSTANCE != null && DiscordConnector.INSTANCE.getJda() != null && DiscordConnector.INSTANCE.getJda().getStatus() == JDA.Status.CONNECTED) {

            //Object to JSON in file
            ObjectMapper mapper = new ObjectMapper();
            try {
                mapper.writeValue(new File(System.getProperty("user.dir") + File.separator + "config.json"), this);
            } catch (IOException e) {
                Controller.INSTANCE.log.addLogEntry("Controller: Error while writing file to disk: ", e);
            }
        }
    }

    public ArrayList<ManipulationRule> getManipulationRules() {
        return manipulationRules;
    }

    public boolean addManipulationRule(ManipulationRule manipulationRule) {
        if (this.manipulationRules.stream().noneMatch(hostname -> hostname.getName().equalsIgnoreCase(manipulationRule.getName()))) {
            try {
                this.manipulationRules.add(manipulationRule);
                saveConfig();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean removeManipulationRule(String name) {
        for (int i = 0; i < manipulationRules.size(); i++) {
            if (manipulationRules.get(i).getName().equalsIgnoreCase(name)) {
                manipulationRules.remove(i);
                saveConfig();
                return true;
            }
        }
        return false;
    }

    public ArrayList<IgnoredHostname> getIgnoredHostnames() {
        return ignoredHostnames;
    }

    public boolean addIgnoredHostname(IgnoredHostname ignoredHostname) {
        if (this.ignoredHostnames.stream().noneMatch(hostname -> hostname.getHostname().equals(ignoredHostname.getHostname()))) {
            try {
                this.ignoredHostnames.add(ignoredHostname);
                saveConfig();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean removeIgnoredHostname(String hostname) {
        for (int i = 0; i < ignoredHostnames.size(); i++) {
            if (ignoredHostnames.get(i).getHostname().equals(hostname)) {
                ignoredHostnames.remove(i);
                saveConfig();
                return true;
            }
        }
        return false;
    }

    public ArrayList<MentionRoleCooldown> getMentionRoleCooldowns() {
        return mentionRoleCooldowns;
    }

    public boolean addMentionRoleCooldown(MentionRoleCooldown mentionRoleCooldown) {
        if (this.mentionRoleCooldowns.stream().noneMatch(mentionRoleCooldown1 -> mentionRoleCooldown1.getRole().equals(mentionRoleCooldown.getRole()))) {
            try {
                this.mentionRoleCooldowns.add(mentionRoleCooldown);
                saveConfig();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean removeMentionRoleCooldown(String role) {
        for (int i = 0; i < mentionRoleCooldowns.size(); i++) {
            if (mentionRoleCooldowns.get(i).getRole().equals(role)) {
                mentionRoleCooldowns.remove(i);
                saveConfig();
                return true;
            }
        }
        return false;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        saveConfig();
    }

    public long getTargetDispatchChannel() {
        return targetDispatchChannel;
    }

    public void setTargetDispatchChannel(long targetDispatchChannel) {
        this.targetDispatchChannel = targetDispatchChannel;
        saveConfig();
    }

    public long getAdminRole() {
        return adminRole;
    }

    public void setAdminRole(long adminRole) {
        this.adminRole = adminRole;
        saveConfig();
    }

    public long getGuildId() {
        return guildId;
    }

    public void setGuildId(long guildId) {
        this.guildId = guildId;
        saveConfig();
    }

    public int getHostCooldown() {
        return hostCooldown;
    }

    public void setHostCooldown(int hostCooldown) {
        this.hostCooldown = hostCooldown;
        saveConfig();
    }

    public String getEngine() {
        return "".equals(engine) ? null : engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
        saveConfig();
    }

    public int getEngineBuild() {
        return engineBuild;
    }

    public void setEngineBuild(int engineBuild) {
        this.engineBuild = engineBuild;
        saveConfig();
    }

    public String getJoinUrl() {
        return joinUrl;
    }

    public void setJoinUrl(String joinUrl) {
        this.joinUrl = joinUrl;
        saveConfig();
    }

    public String getSseEndpoint() {
        return sseEndpoint;
    }

    public void setSseEndpoint(String sseEndpoint) {
        this.sseEndpoint = sseEndpoint;
        saveConfig();
    }
}
