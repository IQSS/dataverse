package edu.harvard.iq.dataverse.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author michael
 */
public class StringUtilTest {

    public StringUtilTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
    }

    public static Stream<Arguments> parametersIsEmpty() {
        return Stream.of(
                        Arguments.of(true, null),
                        Arguments.of(true, ""),
                        Arguments.of(true, " "),
                        Arguments.of(true, "\t"),
                        Arguments.of(true, "\t \t \n"),
                        Arguments.of(false, "a")
        );
    }

    /**
     * Test of isEmpty method, of class StringUtil.
     */
    @ParameterizedTest
    @MethodSource("parametersIsEmpty")
    public void testIsEmpty(boolean isValid, String inputString) {
        assertEquals(isValid, StringUtil.isEmpty(inputString));
    }

    public static Stream<Arguments> parametersIsAlphaNumeric() {
        return Stream.of(
                        Arguments.of(true, "abc"),
                        Arguments.of(true, "1230"),
                        Arguments.of(true, "1230abc"),
                        Arguments.of(true, "1230abcABC"),
                        Arguments.of(false, "1230abcABC#")
        );
    }

    /**
     * Test of isAlphaNumeric method, of class StringUtil.
     */
    @ParameterizedTest
    @MethodSource("parametersIsAlphaNumeric")
    public void testIsAlphaNumeric(boolean isValid, String inputString) {
        assertEquals(isValid, StringUtil.isAlphaNumeric(inputString));
    }

    public static Stream<Arguments> parametersIsAlphaNumericChar() {
        return Stream.of(
                        Arguments.of(true, 'a'),
                        Arguments.of(true, 'f'),
                        Arguments.of(true, 'z'),
                        Arguments.of(true, '0'),
                        Arguments.of(true, '1'),
                        Arguments.of(true, '9'),
                        Arguments.of(true, 'A'),
                        Arguments.of(true, 'G'),
                        Arguments.of(true, 'Z'),
                        Arguments.of(false, '@')
        );
    }

    /**
     * Test of isAlphaNumericChar method, of class StringUtil.
     */
    @ParameterizedTest
    @MethodSource("parametersIsAlphaNumericChar")
    public void testIsAlphaNumericChar(boolean isValid, char inputChar) {
        assertEquals(isValid, StringUtil.isAlphaNumericChar(inputChar));
    }

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
        assertEquals("hello", StringUtil.nullToEmpty("hello"));
        assertEquals("", StringUtil.nullToEmpty(null));
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
                .forEach(v -> assertTrue(StringUtil.isTrue(v)));

        Stream.of("es", "no", " 0 s  ", "0", "x", "false")
                .forEach(v -> assertFalse(StringUtil.isTrue(v)));

        assertFalse(StringUtil.isTrue(null));
    }
}
