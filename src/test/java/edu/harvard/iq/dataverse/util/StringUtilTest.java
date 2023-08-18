package edu.harvard.iq.dataverse.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class StringUtilTest {
    
    /**
     * Test of isEmpty method, of class StringUtil.
     */
    @ParameterizedTest
    @CsvSource(value = {
        "false, a",
        "true, NULL",
        "true, ''",
        "true, ' '",
        "true, \t",
        "true, \t \t \n"
    }, nullValues = "NULL")
    void testIsEmpty(boolean isValid, String inputString) {
        assertEquals( isValid, StringUtil.isEmpty(inputString) );
    }
    
    /**
     * Test of isAlphaNumeric method, of class StringUtil.
     */
    @ParameterizedTest
    @CsvSource({
        "true,abc",
        "true,1230",
        "true,1230abc",
        "true,1230abcABC",
        "false,1230abcABC#"
    })
    void testIsAlphaNumeric(boolean isValid, String inputString) {
        assertEquals(isValid, StringUtil.isAlphaNumeric(inputString) );
    }
    
    /**
     * Test of isAlphaNumericChar method, of class StringUtil.
     */
    @ParameterizedTest
    @CsvSource({
        "true,'a'",
        "true,'f'",
        "true,'z'",
        "true,'0'",
        "true,'1'",
        "true,'9'",
        "true,'A'",
        "true,'G'",
        "true,'Z'",
        "false,'@'"
    })
    void testIsAlphaNumericChar(boolean isValid, char inputChar) {
        assertEquals(isValid, StringUtil.isAlphaNumericChar(inputChar) );
    }
    
    @ParameterizedTest
    @CsvSource(value = {
        // interface-based partitioning
        "NULL, NULL, NULL",
        "NULL, '', NULL",
        "NULL, d, NULL",
        
        "'', NULL, ''",
        "'', '', ''",
        "'', abcdfg, ''",
        
        "abcdfg, NULL, ''",
        "abcdfg, '', ''",
        "abcdfg, d, dfg",
        
        // functionality-based partitioning
        "abcdfg, NULL, ''",
        "abcdfg, h, ''",
        "abcdfg, b, bcdfg"
    }, nullValues = "NULL")
    void testSubstringIncludingLast(String str, String separator, String expectedString) {
        assertEquals( expectedString, StringUtil.substringIncludingLast(str, separator) );
    }

    static Stream<Arguments> toOptionData() {
        return Stream.of(
            Arguments.of(Optional.empty(), null),
            Arguments.of(Optional.empty(), ""),
            Arguments.of(Optional.of("leadingWhitespace"), "    leadingWhitespace"),
            Arguments.of(Optional.of("trailingWhiteSpace"), "trailingWhiteSpace    "),
            Arguments.of(Optional.of("someString"), "someString"),
            Arguments.of(Optional.of("some string with spaces"), "some string with spaces")
        );
    }
    
    @ParameterizedTest
    @MethodSource("toOptionData")
    void testToOption(Optional<String> expected, String inputString) {
        assertEquals(expected, StringUtil.toOption(inputString));
    }
    
    static Stream<Arguments> sanitizeData() {
        return Stream.of(
            Arguments.of("some\\path\\to\\a\\directory", "some/path/to/a/directory", false),
            Arguments.of("some\\//path\\//to\\//a\\//directory", "some/path/to/a/directory", false),
            // starts with / or - or . or whitepsace
            Arguments.of("/some/path/to/a/directory", "some/path/to/a/directory", false),
            Arguments.of("-some/path/to/a/directory", "some/path/to/a/directory", false),
            Arguments.of(".some/path/to/a/directory", "some/path/to/a/directory", false),
            Arguments.of(" some/path/to/a/directory", "some/path/to/a/directory", false),
            // ends with / or - or . or whitepsace
            Arguments.of("some/path/to/a/directory/", "some/path/to/a/directory", false),
            Arguments.of("some/path/to/a/directory-", "some/path/to/a/directory", false),
            Arguments.of("some/path/to/a/directory.", "some/path/to/a/directory", false),
            Arguments.of("some/path/to/a/directory ", "some/path/to/a/directory", false),
            
            Arguments.of("", null, false),
            Arguments.of("/", null, false),
            
            // aggressively
            Arguments.of("some/path/to/a/dire{`~}ctory", "some/path/to/a/dire.ctory", true),
            Arguments.of("some/path/to/a/directory\\.\\.", "some/path/to/a/directory", true)
        );
    }
    
    @ParameterizedTest
    @MethodSource("sanitizeData")
    void testSanitizeFileDirectory(String inputString, String expected, boolean aggressively) {
        assertEquals(expected, StringUtil.sanitizeFileDirectory(inputString, aggressively));
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
