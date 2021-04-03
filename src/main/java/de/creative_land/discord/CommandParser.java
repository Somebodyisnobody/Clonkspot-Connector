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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Class to parse an input string to individual tokens of arguments.
 */
public class CommandParser {

    /**
     * Parses an input string into separate arguments. Normal split occurs at the whitespace character unless it is quoted.
     * @param input The input string.
     * @return An array containing the arguments.
     * @throws MalformedStringException If the input string is not properly formatted.
     */
    public static String[] parse(String input) throws MalformedStringException {
        if (input == null) {
            throw new NullPointerException("input is null.");
        }
        Set<Character> delimitiers = Set.of('"', '\'', '`');
        final StringBuilder sb = new StringBuilder(30);
        final List<String> args = new ArrayList<>(3);
        Optional<Character> quote = Optional.empty();

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);

            //Look for ending quote when already in a quote and push parsed argument to args
            if (quote.map(c -> c == ch).orElse(false)) {
                quote = Optional.empty();
                String res = sb.toString();
                sb.setLength(0);
                if (!res.isBlank()) {
                    args.add(res);
                }
                continue;
            }

            //Append characters of quoted argument to StringBuilder
            if (quote.isPresent()) {
                sb.append(ch);
                continue;
            }

            //Look for spaces (non-quoted) and push parsed argument to args
            if (ch == ' ') {
                String res = sb.toString();
                sb.setLength(0);
                if (!res.isBlank()) {
                    args.add(res);
                }
                continue;
            }

            //Look for starting quote. If found set quote with the right char and continue parsing the argument
            if (delimitiers.contains(ch)) {
                quote = Optional.of(ch);
                continue;
            }

            //Append characters of non-quoted argument to StringBuilder
            sb.append(ch);
        }

        //Starting quote has no ending quote
        if (quote.isPresent()) {
            throw new MalformedStringException(String.format("Malformed input. \"%s\"", input));
        }

        //For last non-quoted argument
        String res = sb.toString();
        if (!res.isBlank()) {
            args.add(res);
        }

        return args.toArray(String[]::new);
    }
}
