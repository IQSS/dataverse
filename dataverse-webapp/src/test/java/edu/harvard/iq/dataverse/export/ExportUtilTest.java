package edu.harvard.iq.dataverse.export;

import org.junit.Assert;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;


public class ExportUtilTest {

    @ParameterizedTest
    @MethodSource("parametersForIsPersonTest")
    public void testIsPerson(String name, boolean expected) {
        Assert.assertEquals(expected, ExportUtil.isPerson(name));
    }

    private static Stream<Arguments> parametersForIsPersonTest() {
        return Stream.of(
                Arguments.of("Jan Kowalski", true),
                Arguments.of("John Kowalsky", true),
                Arguments.of("Kowalski, Jan", true),
                Arguments.of("Kowalski, J.", true),
                Arguments.of("Kowalski, J.K.", true),
                Arguments.of("Kowalski, J.K.P.", true),
                Arguments.of("Jan Maria Kowalski", true),
                Arguments.of("Jan Maria Kowalski Rokita", true),
                Arguments.of("Xxx Kowalski", false),
                Arguments.of("Kowalski, Xxx", false),
                Arguments.of("Kowalski, j.", false),
                Arguments.of("Kowalski, J.x.", false),
                Arguments.of("Kowalski, J.5.", false),
                Arguments.of("Kowalski, J.k.P.", false),
                Arguments.of("Jan Maria Kowalski Rokita Nowak", false),
                Arguments.of("Jan", false)
        );
    }
}
