package de.creative_land.discord;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test class for the argument parser
 *
 */
public class CommandParserTest {

    /**
     * Tests an input string against the expected list of tokens.
     * 
     * @param input The input string
     * @param expected The expected tokens.
     * @throws MalformedStringException If input is unexpectedly malformed.
     */
    @ParameterizedTest
    @MethodSource
    public void testParser(String input, String[] expected) throws MalformedStringException {
        if (expected != null) {
            String[] out = CommandParser.parse(input);
            assertArrayEquals(expected, out);
        } else {
            assertThrows(MalformedStringException.class, () -> CommandParser.parse(input));
        }
    }
    
    /**
     * Method to create argument stream for parser test.
     * @return A stream of arguments.
     */
    public static Stream<Arguments> testParser() {
        String[][][] arguments = {
                {{"command arg1 arg2"}, {"command", "arg1", "arg2"}},
                {{"command \"a r g 1\" arg2 'arg 3'"}, {"command", "a r g 1", "arg2", "arg 3"}},
                {{"'command' `arg 1` arg2 arg3"}, {"command", "arg 1", "arg2", "arg3"}},
                {{"'command"}, null},
                {{"command 'arg1' 'arg2"}, null}
        };
        return Arrays.stream(arguments)
                .map(x -> Arguments.of(x[0][0], x[1]));
    }
}
