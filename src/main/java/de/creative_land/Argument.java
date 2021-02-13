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

@SuppressWarnings("SpellCheckingInspection")
public enum Argument {
    HELP("--help"),
    GUILD_ID("--guildid"),
    ADMIN_ROLE_NAME("--adminrolename"),
    KEY("--key"),
    JOIN_URL("--joinurl"),
    SSE_ENDPOINT("--sseendpoint"),
    ;

    private final String value;

    Argument(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
