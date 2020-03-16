package edu.harvard.iq.dataverse.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author rmp553
 */
public class MarkupCheckerTest {

    @ParameterizedTest
    @MethodSource("parametersForSanitizeBasicHTML")
    public void sanitizeBasicHTML(String inputHtml, String expected) {
        assertEquals(expected, MarkupChecker.sanitizeBasicHTML(inputHtml));
    }

    private static Stream<Arguments> parametersForSanitizeBasicHTML() {
        return Stream.of(
                Arguments.of("<script>alert('hi')</script>", ""),
                Arguments.of("<map name=\"rtdcCO\">", "<map name=\"rtdcCO\"></map>"),
                Arguments.of("<area shape=\"rect\" coords=\"42,437,105,450\" href=\"/dvn/dv/rtdc/faces/study/StudyPage.xhtml?globalId=hdl:10904/10006\" title=\"Galactic Center (DHT02)\" alt=\"Galactic Center (DHT02)\">",
                             "<area shape=\"rect\" coords=\"42,437,105,450\" href=\"/dvn/dv/rtdc/faces/study/StudyPage.xhtml?globalId=hdl:10904/10006\" title=\"Galactic Center (DHT02)\" alt=\"Galactic Center (DHT02)\">"),
                Arguments.of("<map name=\"rtdcCO\"><area shape=\"rect\" coords=\"42,437,105,450\" href=\"/dvn/dv/rtdc/faces/study/StudyPage.xhtml?globalId=hdl:10904/10006\" title=\"Galactic Center (DHT02)\" alt=\"Galactic Center (DHT02)\"></map>",
                            "<map name=\"rtdcCO\"><area shape=\"rect\" coords=\"42,437,105,450\" href=\"/dvn/dv/rtdc/faces/study/StudyPage.xhtml?globalId=hdl:10904/10006\" title=\"Galactic Center (DHT02)\" alt=\"Galactic Center (DHT02)\"></map>"),
                Arguments.of("<p>hello</", "<p>hello&lt;/</p>"),
                Arguments.of("<h1>hello</h2>", "<h1>hello</h1>"),
                Arguments.of("the <a href=\"http://dataverse.org\" target=\"_blank\">Dataverse project</a> in a new window",
                        "the \n<a href=\"http://dataverse.org\" target=\"_blank\" rel=\"nofollow\">Dataverse project</a> in a new window"),
                Arguments.of("the <a href=\"http://dataverse.org\">Dataverse project</a> in a new window",
                        "the \n<a href=\"http://dataverse.org\" rel=\"nofollow\" target=\"_blank\">Dataverse project</a> in a new window")
        );
    }

    @Test
    public void sanitizeBasicHTML_null() {
        assertNull(MarkupChecker.sanitizeBasicHTML(null));
    }

    @Test
    public void stripAllTags() {
        assertEquals("", MarkupChecker.stripAllTags(""));
    }

    @Test
    public void stripAllTags_null() {
        assertNull(MarkupChecker.stripAllTags(null));
    }

    @Test
    public void escapeHtml() {
        assertEquals("foo&lt;br&gt;bar", MarkupChecker.escapeHtml("foo<br>bar"));
    }

}
