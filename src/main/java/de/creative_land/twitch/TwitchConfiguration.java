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

package de.creative_land.twitch;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TwitchConfiguration {
    /**
     * The client id for the API connection.
     */
    private final String clientId;

    /**
     * The token for the API connection.
     */
    private final String clientSecret;

    /**
     * The name of the game to search for streams.
     */
    private final String game;

    /**
     * The initial delay in seconds after which the game list is being fetched.
     */
    private final int initialStartupDelay;

    /**
     * The period in seconds in which the game list is being fetched.
     */
    private final int searchPeriod;

    /**
     * The identifier for an icon that will be placed at the beginning of a Twitch related message. E.g. ":smile:"
     */
    private final String iconString;

    /**
     * If set a role will be poked when a new stream starts
     */
    private final String notificationRole;

    public TwitchConfiguration(@JsonProperty("clientId") String clientId, @JsonProperty("clientSecret") String clientSecret, @JsonProperty("game") String game, @JsonProperty("initialStartupDelay") int initialStartupDelay, @JsonProperty("searchPeriod") int searchPeriod, @JsonProperty("iconString") String iconString, @JsonProperty("notificationRole") String notificationRole) {
        this.clientId = clientId == null ? "" : clientId;
        this.clientSecret = clientSecret == null ? "" : clientSecret;
        this.game = game == null ? "" : game;
        this.initialStartupDelay = initialStartupDelay == 0 ? 5 : initialStartupDelay;
        this.searchPeriod = searchPeriod == 0 ? 30 : searchPeriod;
        this.iconString = iconString == null ? "" : iconString;
        this.notificationRole = notificationRole == null ? "" : notificationRole;
    }

    public String getIconString() {
        return iconString;
    }

    public String getNotificationRole() {
        return notificationRole;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getGame() {
        return game;
    }

    public int getInitialStartupDelay() {
        return initialStartupDelay;
    }

    public int getSearchPeriod() {
        return searchPeriod;
    }
}
