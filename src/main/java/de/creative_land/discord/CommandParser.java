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

            if (quote.map(c -> c == ch).orElse(false)) {
                quote = Optional.empty();
                args.add(sb.toString());
                sb.setLength(0);
                continue;
            }
            if (quote.isPresent()) {
                sb.append(ch);
                continue;
            }
            if (ch == ' ') {
                String res = sb.toString();
                sb.setLength(0);
                if (!res.isBlank()) {
                    args.add(res);
                }
                continue;
            }
            if(delimitiers.contains(ch)) {
                quote = Optional.of(ch);
                continue;
            }
            sb.append(ch);
        }

        if (quote.isPresent()) {
            throw new MalformedStringException(String.format("Malformed input. \"%s\"", input));
        }

        String res = sb.toString();
        if (!res.isBlank()) {
            args.add(res);
        }

        return args.toArray(String[]::new);
    }
}
