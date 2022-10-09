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

package de.creative_land.discord.clonk_game_reference;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ManipulationRule {

    private final String name;

    private final Pattern pattern;

    private final String author;

    private String replacement;

    private List<String> roles;

    public ManipulationRule(@JsonProperty("pattern") @NotNull Pattern pattern, @JsonProperty("replacement") String replacement, @JsonProperty("roles") String[] roles, @JsonProperty("user") String user, @NotNull @JsonProperty("name") String name) {
        this.pattern = pattern;
        this.replacement = replacement;
        if (replacement != null && replacement.equals("null")) this.replacement = null;

        if (roles != null) this.roles = Arrays.asList(roles);
        this.author = user;
        this.name = name;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public String getReplacement() {
        return replacement;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getAuthor() {
        return author;
    }

    public String getName() {
        return name;
    }
}
