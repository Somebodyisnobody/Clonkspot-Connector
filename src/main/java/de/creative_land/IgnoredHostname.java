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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

public class IgnoredHostname {

    private final String hostname;

    private final int minPlayer;

    private final String author;

    private final String reason;

    public IgnoredHostname(@NotNull @JsonProperty("hostname") String hostname, @JsonProperty("author") String author, @JsonProperty("minPlayer") int minPlayer, @JsonProperty("reason") String reason) {
        this.hostname = hostname;
        this.author = author;
        this.minPlayer = minPlayer;
        this.reason = reason;
    }

    public String getHostname() {
        return hostname;
    }

    public String getAuthor() {
        return author;
    }

    public int getMinPlayer() {
        return minPlayer;
    }

    public String getReason() {
        return reason;
    }
}
