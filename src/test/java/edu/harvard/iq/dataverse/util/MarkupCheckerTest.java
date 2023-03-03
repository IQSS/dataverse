package edu.harvard.iq.dataverse.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MarkupCheckerTest {

    /**
     * Test of sanitizeBasicHTML method, of class MarkupChecker.
     */
    @ParameterizedTest
    @CsvSource(value = {
        "<script>alert('hi')</script>, ''",
        "'<map name=\"rtdcCO\">', '<map name=\"rtdcCO\"></map>'",
        // make sure we do not destroy the <area> tags
        "'<area shape=\"rect\" coords=\"42,437,105,450\" href=\"/dvn/dv/rtdc/faces/study/StudyPage.xhtml?globalId=hdl:10904/10006\" title=\"Galactic Center (DHT02)\" alt=\"Galactic Center (DHT02)\">', '<area shape=\"rect\" coords=\"42,437,105,450\" href=\"/dvn/dv/rtdc/faces/study/StudyPage.xhtml?globalId=hdl:10904/10006\" title=\"Galactic Center (DHT02)\" alt=\"Galactic Center (DHT02)\">'",
        // make sure we do not destroy the <map> tags
        "'<map name=\"rtdcCO\"><area shape=\"rect\" coords=\"42,437,105,450\" href=\"/dvn/dv/rtdc/faces/study/StudyPage.xhtml?globalId=hdl:10904/10006\" title=\"Galactic Center (DHT02)\" alt=\"Galactic Center (DHT02)\"></map>', '<map name=\"rtdcCO\"><area shape=\"rect\" coords=\"42,437,105,450\" href=\"/dvn/dv/rtdc/faces/study/StudyPage.xhtml?globalId=hdl:10904/10006\" title=\"Galactic Center (DHT02)\" alt=\"Galactic Center (DHT02)\"></map>'",
        "'<p>hello</', '<p>hello&lt;/</p>'",
        "'<h1>hello</h2>', '<h1>hello</h1>'",
        "'the <a href=\"http://dataverse.org\" target=\"_blank\">Dataverse project</a> in a new window', 'the <a href=\"http://dataverse.org\" target=\"_blank\" rel=\"nofollow\">Dataverse project</a> in a new window'",
        "'the <a href=\"http://dataverse.org\">Dataverse project</a> in a new window', 'the <a href=\"http://dataverse.org\" rel=\"nofollow\" target=\"_blank\">Dataverse project</a> in a new window'",
        "NULL, NULL"
    }, nullValues = {"NULL"})
    public void testSanitizeBasicHTML(String unsafe, String safe) {
        assertEquals(safe, MarkupChecker.sanitizeBasicHTML(unsafe));
    }

    /**
     * Test of stripAllTags method, of class MarkupChecker.
     */
    @ParameterizedTest
    @CsvSource(value = {
        "'', ''",
        "NULL, NULL",
        "Johnson & Johnson <>, Johnson & Johnson <>",
        "Johnson && Johnson <&>&, Johnson && Johnson <&>&"
    }, nullValues = {"NULL"})
    public void testStripAllTags(String unsafe, String safe) {
        assertEquals(safe, MarkupChecker.stripAllTags(unsafe));
    }
    
    /**
     * Test of stripAllTags method, of class MarkupChecker.
     */
    @Test
    public void testEscapeHtml() {
        assertEquals("foo&lt;br&gt;bar", MarkupChecker.escapeHtml("foo<br>bar"));
    }

}
