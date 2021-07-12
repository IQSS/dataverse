package edu.harvard.iq.dataverse.util.json;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonUtilTest {
    
    private static Stream<Arguments> testArgs() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of("", ""),
            Arguments.of("junk", "junk"),
            Arguments.of("{}", "\n{\n}"),
            Arguments.of("{\"foo\": \"bar\"}", "\n{\n" + "    \"foo\": \"bar\"\n" + "}"));
    }
    
    
    @ParameterizedTest
    @MethodSource("testArgs")
    public void testPrettyPrint(String input, String expected) {
        assertEquals(expected, JsonUtil.prettyPrint(input));
    }

}
