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

package de.creative_land.clonkspot.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class GameRefEvent {
    @NonNull
    private final GameReference gameReference;

    @NonNull
    private final EventType eventType;

    public enum EventType {
        CREATE("create"),
        UPDATE("update"),
        LOBBY_CLOSED("delete"),
        GAME_ENDED("end"),
        UNKNOWN_ENDED_OR_CLOSED(null),
        ;

        private final String value;

        EventType(String value) {
            this.value = value;
        }

        /**
         * Returns the enum constant of this type with the specified value.
         * The string must match exactly a value used to declare an enum constant in this type.
         *
         * @param value The value of the enum constant.
         * @return The enum constant with the specified value.
         */
        public static EventType fromValue(@NonNull String value) {
            for (EventType b : EventType.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}
