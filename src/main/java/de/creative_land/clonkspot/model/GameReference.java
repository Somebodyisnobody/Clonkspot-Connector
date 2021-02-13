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

package de.creative_land.clonkspot.model;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
public class GameReference {

    final public int id;

    final public String title;

    final public String status;

    final public String league;

    final public String comment;

    final public int maxPlayers;

    final public String hostname;

    final public String created;

    final public String updated;

    final public String engine;

    final public int engineBuild;

    final public Player[] players;

    final public Flags flags;

    final public Scenario scenario;

    public String essEventType;

    public GameReference(@JsonProperty("id") int id, @JsonProperty("title") String title, @JsonProperty("status") String status,
                         @JsonProperty("type") String league, @JsonProperty("comment") String comment, @JsonProperty("maxPlayers") int maxPlayers,
                         @JsonProperty("host") String hostname, @JsonProperty("created") String created, @JsonProperty("updated") String updated,
                         @JsonProperty("engine") String engine, @JsonProperty("engineBuild") int engineBuild, @JsonProperty("players") Player[] players,
                         @JsonProperty("flags") Flags flags, @JsonProperty("scenario") Scenario scenario) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.league = league;
        this.comment = comment;
        this.maxPlayers = maxPlayers;
        this.hostname = hostname;
        this.created = created;
        this.updated = updated;
        this.engine = engine;
        this.engineBuild = engineBuild;
        this.players = players;
        this.flags = flags;
        this.scenario = scenario;
    }
}

