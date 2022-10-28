package edu.harvard.iq.dataverse.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


/**
 *
 * @author michael
 */
@RunWith(Enclosed.class)
public class StringUtilTest {

    public StringUtilTest() {
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

    @RunWith(Parameterized.class)
    public static class TestIsEmpty {

        public boolean isValid;
        public String inputString;
        
        public TestIsEmpty(boolean isValid, String inputString) {
            this.isValid = isValid;
            this.inputString = inputString;
        }

        @Parameters
        public static Collection<Object[]> parameters() {
            return Arrays.asList(
                    new Object[][] { 
                        { true, null },
                        { true, "" },
                        { true, " " },
                        { true, "\t" },
                        { true, "\t \t \n" },
                        { false, "a" },
                    }
            );
        }

        /**
         * Test of isEmpty method, of class StringUtil.
         */
        @Test
        public void testIsEmpty() {
            assertEquals( isValid, StringUtil.isEmpty(inputString) );
        }
    }

    @RunWith(Parameterized.class)
    public static class TestIsAlphaNumeric {

        public boolean isValid;
        public String inputString;
        
        public TestIsAlphaNumeric(boolean isValid, String inputString) {
            this.isValid = isValid;
            this.inputString = inputString;
        }

        @Parameters
        public static Collection<Object[]> parameters() {
            return Arrays.asList(
                    new Object[][] { 
                        { true, "abc" },
                        { true, "1230" },
                        { true, "1230abc" },
                        { true, "1230abcABC" },
                        { false, "1230abcABC#" },
                    }
            );
        }

        /**
         * Test of isAlphaNumeric method, of class StringUtil.
         */
        @Test
        public void testIsAlphaNumeric() {
            assertEquals( isValid, StringUtil.isAlphaNumeric(inputString) );
        }
    }

    @RunWith(Parameterized.class)
    public static class TestIsAlphaNumericChar {

        public boolean isValid;
        public char inputChar;
        
        public TestIsAlphaNumericChar(boolean isValid, char inputChar) {
            this.isValid = isValid;
            this.inputChar = inputChar;
        }

        @Parameters
        public static Collection<Object[]> parameters() {
            return Arrays.asList(
                    new Object[][] { 
                        { true, 'a' },
                        { true, 'f' },
                        { true, 'z' },
                        { true, '0' },
                        { true, '1' },
                        { true, '9' },
                        { true, 'A' },
                        { true, 'G' },
                        { true, 'Z' },
                        { false, '@' },
                    }
            );
        }

        /**
         * Test of isAlphaNumericChar method, of class StringUtil.
         */
        @Test
        public void testIsAlphaNumericChar() {
            assertEquals( isValid, StringUtil.isAlphaNumericChar(inputChar) );
        }
    }

    @RunWith(Parameterized.class)
    public static class TestSubstringIncludingLast {

        public String str;
        public String separator;
        public String expectedString;
        
        public TestSubstringIncludingLast(String str, String separator, String expectedString) {
            this.str = str;
            this.separator = separator;
            this.expectedString = expectedString;
        }

        @Parameters
        public static Collection<Object[]> parameters() {
            return Arrays.asList(
                    new Object[][] { 
                        // interface-based partitioning
                        {null, null, null},
                        {null, "", null},
                        {null, "d", null},

                        {"", null, ""},
                        {"", "", ""},
                        {"", "abcdfg", ""},

                        {"abcdfg", null, ""},
                        {"abcdfg", "", ""},
                        {"abcdfg", "d", "dfg"},

                        // functionality-based partitioning
                        {"abcdfg" , null, ""},
                        {"abcdfg", "h", ""},
                        {"abcdfg", "b", "bcdfg"},
                    }
            );
        }

        @Test
        public void testSubstringIncludingLast() {
            assertEquals( expectedString, StringUtil.substringIncludingLast(str, separator) );
        }
    }

    @RunWith(Parameterized.class)
    public static class TestToOption {

        public String inputString;
        public Optional<String> expected;

        public TestToOption(String inputString, Optional<String> expected) {
            this.inputString = inputString;
            this.expected = expected;
        }

        @Parameters
        public static Collection<Object[]> parameters() {
            return Arrays.asList(
                    new Object[][] { 
                        {null, Optional.empty()},
                        {"", Optional.empty()},
                        {"    leadingWhitespace", Optional.of("leadingWhitespace")},
                        {"trailingWhiteSpace    ", Optional.of("trailingWhiteSpace")},
                        {"someString", Optional.of("someString")},
                        {"some string with spaces", Optional.of("some string with spaces")}
                    }
            );
        }

        @Test
        public void testToOption() {
            assertEquals(expected, StringUtil.toOption(inputString));
        }
    }

    @RunWith(Parameterized.class)
    public static class TestSanitizeFileDirectory {

        public String inputString;
        public String expected;
        public boolean aggressively;

        public TestSanitizeFileDirectory(String inputString, String expected, boolean aggressively) {
            this.inputString = inputString;
            this.expected = expected;
            this.aggressively = aggressively;
        }

        @Parameters
        public static Collection<Object[]> parameters() {
            return Arrays.asList(
                    new Object[][] { 
                        {"some\\path\\to\\a\\directory", "some/path/to/a/directory", false},
                        {"some\\//path\\//to\\//a\\//directory", "some/path/to/a/directory", false},
                        // starts with / or - or . or whitepsace
                        {"/some/path/to/a/directory", "some/path/to/a/directory", false},
                        {"-some/path/to/a/directory", "some/path/to/a/directory", false},
                        {".some/path/to/a/directory", "some/path/to/a/directory", false},
                        {" some/path/to/a/directory", "some/path/to/a/directory", false},
                        // ends with / or - or . or whitepsace
                        {"some/path/to/a/directory/", "some/path/to/a/directory", false},
                        {"some/path/to/a/directory-", "some/path/to/a/directory", false},
                        {"some/path/to/a/directory.", "some/path/to/a/directory", false},
                        {"some/path/to/a/directory ", "some/path/to/a/directory", false},

                        {"", null, false},
                        {"/", null, false},

                        // aggressively
                        {"some/path/to/a/dire{`~}ctory", "some/path/to/a/dire.ctory", true},
                        {"some/path/to/a/directory\\.\\.", "some/path/to/a/directory", true},
                    }
            );
        }

        @Test
        public void testSanitizeFileDirectory() {
            assertEquals(expected, StringUtil.sanitizeFileDirectory(inputString, aggressively));
        }
    }

    public static class StringUtilNoParamTest{

        @Test
        public void testHtml2Text() {
            assertEquals(StringUtil.html2text("be <b>bold</b>!"), "be bold!");
            assertEquals(StringUtil.html2text(null), null);
            assertEquals(StringUtil.html2text("<p><b>Description:</b><br />\n"
                    + "Data were taken May-June 2003 and 2005. Flux units are in mJy per 31 arcsecond beam.\n"
                    + "</p>\n"
                    + "\n"
                    + "<p><b>Telescope Information</b><br />\n"
                    + "<a href=\"http://www.submm.caltech.edu/cso/\">Caltech Submillimeter Observatory</a></p>\n"
                    + "\n"
                    + "<p><b>Status:</b><br />\n"
                    + "Final</p>\n"
                    + "\n"
                    + "<p><b>Sampling:</b><br />\n"
                    + "Sensitivity: Average 1 sigma rms = 10 mJy per beam.<br />\n"
                    + "Waveband: 1120 microns<br />\n"
                    + "Resolution: 31 arcsecond beam in 10 arcsecond pixels (diffuse large-scale structure is lost)\n"
                    + "</p>\n"
                    + "\n"
                    + "<p><b>Areal Coverage:</b><br />\n"
                    + "11 square degrees\n"
                    + "</p>\n"
                    + "\n"
                    + "<p><b>Map Center (Galactic):</b><br />\n"
                    + "NA<br />\n"
                    + "NA</p>\n"
                    + "\n"
                    + "<p><b>Map Center (J2000):</b><br />\n"
                    + "RA = 18:29:00<br />\n"
                    + "Dec = +00:30:00 </p>"), "Description: Data were taken May-June 2003 and 2005. Flux units are in mJy per 31 arcsecond beam. Telescope Information Caltech Submillimeter Observatory Status: Final Sampling: Sensitivity: Average 1 sigma rms = 10 mJy per beam. Waveband: 1120 microns Resolution: 31 arcsecond beam in 10 arcsecond pixels (diffuse large-scale structure is lost) Areal Coverage: 11 square degrees Map Center (Galactic): NA NA Map Center (J2000): RA = 18:29:00 Dec = +00:30:00");
            assertEquals(StringUtil.htmlArray2textArray(Arrays.asList("be <b>bold</b>!")), Arrays.asList("be bold!"));
            assertEquals(StringUtil.htmlArray2textArray(null), Collections.emptyList());
        }
        
        @Test
        public void testNullToEmpty() {
            assertEquals( "hello", StringUtil.nullToEmpty("hello") );
            assertEquals( "", StringUtil.nullToEmpty(null) );
        }
        
        @Test
        public void testSymmetricEncryption() {
            String source = "Hello, world! This is an encryption test";
            String password = "12345678";
            final String encrypted = StringUtil.encrypt(source, password);
            final String decrypted = StringUtil.decrypt(encrypted, password);
            
            assertEquals(source, decrypted);
        }
        
        @Test
        public void testIsTrue() {
            Stream.of("yes", "Yes", "  yes  ", "1", "allow", "tRuE")
                .forEach( v -> assertTrue(StringUtil.isTrue(v)) );
            
            Stream.of("es", "no", " 0 s  ", "0", "x", "false")
                .forEach( v -> assertFalse(StringUtil.isTrue(v)) );
            
            assertFalse( StringUtil.isTrue(null) );
        }

        @Test
        public void testNonEmpty_normalString() {
            String expected = "someString";
            assertTrue(StringUtil.nonEmpty(expected));
        }

        @Test
        public void testNonEmpty_null() {
            String expected = null;
            assertFalse(StringUtil.nonEmpty(expected));
        }

        @Test
        public void testNonEmpty_emptyString() {
            String expected = "";
            assertFalse(StringUtil.nonEmpty(expected));
        }
        
        /**
         * full name or organization name cleanup.
         * 
         * @author francesco.cadili@4science.it
         * 
         *         Name is composed of: <First Names> <Family Name>
         */
        @Test
        public void testNormalize() {
            assertEquals(StringUtil.normalize("    Francesco    "), "Francesco");
            assertEquals(StringUtil.normalize("Francesco  Cadili "), "Francesco Cadili");
            assertEquals(StringUtil.normalize("  Cadili,Francesco"), "Cadili, Francesco");
            assertEquals(StringUtil.normalize("Cadili,     Francesco  "), "Cadili, Francesco");
            assertEquals(StringUtil.normalize(null), "");

            // TODO: organization examples...
        }
    }
}
