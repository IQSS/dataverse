package edu.harvard.iq.dataverse.util;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author rmp553
 */
public class MarkupCheckerTest {

    public MarkupCheckerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private void msg(String s) {
        System.out.println(s);
    }

    private void msgu(String s) {
        msg("--------------------------------------");
        msg(s);
    }

    /**
     * Test of sanitizeBasicHTML method, of class MarkupChecker.
     */
    @Test
    public void testSanitizeBasicHTML() {
        System.out.println("sanitizeBasicHTML");

        /*String safeStr = "<img src=\"some/png.png\" alt=\"bee\" class=\"some-class\">";
        String sanitized = MarkupChecker.sanitizeBasicHTML(safeStr);
        this.msgu("safeStr: " + safeStr + "\nsanitized: " + sanitized);
        assertTrue(safeStr.equals(sanitized));
         */
        String safeStr = "<script>alert('hi')</script>";
        String sanitized = MarkupChecker.sanitizeBasicHTML(safeStr);
        this.msgu("safeStr: " + safeStr + "\nsanitized: " + sanitized);
        assertTrue(sanitized.equals(""));

        String unsafeStr = "<map name=\"rtdcCO\">";
        safeStr = "<map name=\"rtdcCO\"></map>";
        sanitized = MarkupChecker.sanitizeBasicHTML(unsafeStr);
        this.msgu("safeStr: " + safeStr + "\nsanitized: " + sanitized);
        assertTrue(safeStr.equals(sanitized));

        unsafeStr = "<area shape=\"rect\" coords=\"42,437,105,450\" href=\"/dvn/dv/rtdc/faces/study/StudyPage.xhtml?globalId=hdl:10904/10006\" title=\"Galactic Center (DHT02)\" alt=\"Galactic Center (DHT02)\">";
        safeStr = unsafeStr;//"<map name=\"rtdcCO\"></map>";
        sanitized = MarkupChecker.sanitizeBasicHTML(unsafeStr);
        this.msgu("safeStr: " + safeStr + "\nsanitized: " + sanitized);
        assertTrue(safeStr.equals(sanitized));

        unsafeStr = "<map name=\"rtdcCO\"><area shape=\"rect\" coords=\"42,437,105,450\" href=\"/dvn/dv/rtdc/faces/study/StudyPage.xhtml?globalId=hdl:10904/10006\" title=\"Galactic Center (DHT02)\" alt=\"Galactic Center (DHT02)\"></map>";
        safeStr = unsafeStr;//"<map name=\"rtdcCO\"></map>";
        sanitized = MarkupChecker.sanitizeBasicHTML(unsafeStr);
        this.msgu("safeStr: " + safeStr + "\nsanitized: " + sanitized);
        assertTrue(safeStr.equals(sanitized));

        unsafeStr = "<p>hello</";
        safeStr = "<p>hello&lt;/</p>";
        sanitized = MarkupChecker.sanitizeBasicHTML(unsafeStr);
        this.msgu("safeStr: " + safeStr + "\nsanitized: " + sanitized);
        assertTrue(safeStr.equals(sanitized));

        unsafeStr = "<h1>hello</h2>";
        safeStr = "<h1>hello</h1>";
        sanitized = MarkupChecker.sanitizeBasicHTML(unsafeStr);
        this.msgu("safeStr: " + safeStr + "\nsanitized: " + sanitized);
        assertTrue(safeStr.equals(sanitized));

        unsafeStr = "the <a href=\"http://dataverse.org\" target=\"_blank\">Dataverse project</a> in a new window";
        safeStr = "the \n<a href=\"http://dataverse.org\" rel=\"nofollow\" target=\"_blank\">Dataverse project</a> in a new window";
        sanitized = MarkupChecker.sanitizeBasicHTML(unsafeStr);
        this.msgu("safeStr: " + safeStr + "\nsanitized: " + sanitized);
        assertTrue(safeStr.equals(sanitized));
        
        unsafeStr = "the <a href=\"http://dataverse.org\">Dataverse project</a> in a new window";
        safeStr = "the \n<a href=\"http://dataverse.org\" rel=\"nofollow\" target=\"_blank\">Dataverse project</a> in a new window";
        sanitized = MarkupChecker.sanitizeBasicHTML(unsafeStr);
        this.msgu("safeStr: " + safeStr + "\nsanitized: " + sanitized);
        assertTrue(safeStr.equals(sanitized));      
        
        //test null
        unsafeStr = null;
        sanitized = MarkupChecker.sanitizeBasicHTML(unsafeStr);
        assertNull(sanitized);

    }

    /**
     * Test of stripAllTags method, of class MarkupChecker.
     */
    @Test
    public void testStripAllTags() {

        System.out.println("stripAllTags");
        String unsafe = "";
        String expResult = "";
        String result = MarkupChecker.stripAllTags(unsafe);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.

        //test null
        unsafe = null;
        result = MarkupChecker.stripAllTags(unsafe);
        assertNull(result);
        
    }

    @Test
    public void testEscapeHtml() {
        assertEquals("foo&lt;br&gt;bar", MarkupChecker.escapeHtml("foo<br>bar"));
    }

}
