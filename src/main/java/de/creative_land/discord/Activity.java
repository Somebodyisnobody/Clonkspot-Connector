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

public enum Activity {
    RUNNING("nach neuen offenen Runden auf clonkspot.org."),
    STOPPED("nach gar nix. Er ist pausiert."),
    ERROR_NO_GUILD("falsche Configs: Es ist kein Server konfiguriert, der benutzt werden soll! Übergabeparameter beachten."),
    ERROR_NO_CHANNEL("falsche Configs: Konfigurierter Kanal existiert nicht!"),
    ERROR_NO_ADMIN_ROLE("falsche Configs: Keine Administrationsrolle! Übergabeparameter beachten."),
    ERROR_UPSTREAM_OFFLINE("wann wieder eine Verbindung zum Clonkspot möglich ist."),
    ;

    private final String value;

    Activity(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
