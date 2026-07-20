
package edu.harvard.iq.dataverse.util.bagit;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests adapted for DD-2093: verify the behavior of BagGenerator.multilineWrap.
 */
public class BagGeneratorMultilineWrapTest {

    private static Method multilineWrap;

    @BeforeAll
    static void setUp() throws NoSuchMethodException {
        // Access the private static method via reflection
        multilineWrap = BagGenerator.class.getDeclaredMethod("multilineWrap", String.class);
        multilineWrap.setAccessible(true);
    }

    private String callMultilineWrap(String input) {
        try {
            return (String) multilineWrap.invoke(null, input);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shortLine_noWrap() {
        String input = "Hello world";
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo("Hello world");
    }

    @Test
    void exactBoundary_78chars_noWrap() {
        String input = "a".repeat(78);
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(input);
    }

    @Test
    void longSingleWord_wrapsAt78WithIndent() {
        String input = "a".repeat(100);
        String expected = "a".repeat(79) + "\r\n " + "a".repeat(21);
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void multiline_input_indentsSecondAndSubsequentOriginalLines() {
        String input = "Line1\nLine2\nLine3";
        String expected = "Line1\r\n Line2\r\n Line3";
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void multiline_withLF_normalizedAndIndented() {
        String input = "a".repeat(200);
        String expected = "a".repeat(79) + "\r\n " + "a".repeat(78) + "\r\n " + "a".repeat(43);
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void emptyLines_trimmedAndSkipped() {
        String input = "Line1\n\nLine3";
        String expected = "Line1\r\n Line3";
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void whitespaceOnlyLines_ignored() {
        String input = "Line1\n   \n\t\t\nLine3";
        String expected = "Line1\r\n Line3";
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void longSecondLine_preservesIndentOnWraps() {
        String line1 = "Header";
        String line2 = "b".repeat(90);
        String input = line1 + "\n" + line2;
        String expected = "Header\r\n " + "b".repeat(79) + "\r\n " + "b".repeat(11);
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void labelLength_reducesFirstLineMaxLength() {
        // With a label of length 20, first line should wrap at 78-20=58 chars
        String label = "l".repeat(20);
        String input = label + "a".repeat(150);
        // First line: 58 chars, subsequent lines: 78
        String expected = label + "a".repeat(59) + "\r\n " + "a".repeat(78) + "\r\n " + "a".repeat(13);
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void labelLength_zero_behavesAsDefault() {
        String input = "a".repeat(100);
        String expected = "a".repeat(79) + "\r\n " + "a".repeat(21);
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void labelLength_withMultipleLines_onlyAffectsFirstLine() {
        String label = "l".repeat(15);
        String input = label + "a".repeat(100) + "\nSecond line content";
        // First line wraps at 79-15=64, then continues at 78 per line
        // Second line starts fresh and wraps normally
        String expected = label + "a".repeat(64) + "\r\n " + "a".repeat(36) + "\r\n Second line content";
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void wrapsAtWordBoundary_notMidWord() {
        // Create a string with a word boundary at position 75
        // "a" repeated 75 times, then a space, then more characters
        String input = "a".repeat(75) + " " + "b".repeat(20);
        // Should wrap at the space (position 75), not at position 79
        String expected = "a".repeat(75) + "\r\n " + "b".repeat(20);
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void wrapsAtWordBoundary_multipleSpaces() {
        // Test with word boundary closer to the limit
        String input = "a".repeat(70) + " word " + "b".repeat(20);
        // Should wrap after "word" (at position 76)
        String expected = "a".repeat(70) + " word\r\n " + "b".repeat(20);
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void wrapsAtWordBoundary_withLabelLength() {
        String label = "l".repeat(20);
        // With label length=20, first line wraps at 78-20=58
        // Create string with word boundary at position 55
        String input = label + "a".repeat(55) + " " + "b".repeat(30);
        // Should wrap at the space (position 55)
        String expected = label + "a".repeat(55) + "\r\n " + "b".repeat(30);
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    // Tests for additional line separator characters

    @Test
    void multiline_withCR_normalizedAndIndented() {
        String input = "Line1\rLine2\rLine3";
        String expected = "Line1\r\n Line2\r\n Line3";
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void multiline_withCRLF_normalizedAndIndented() {
        String input = "Line1\r\nLine2\r\nLine3";
        String expected = "Line1\r\n Line2\r\n Line3";
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void multiline_withVT_normalizedAndIndented() {
        // VT (U+000B) - Vertical Tab
        String input = "Line1\u000BLine2\u000BLine3";
        String expected = "Line1\r\n Line2\r\n Line3";
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void multiline_withFF_normalizedAndIndented() {
        // FF (U+000C) - Form Feed
        String input = "Line1\u000CLine2\u000CLine3";
        String expected = "Line1\r\n Line2\r\n Line3";
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void multiline_withNEL_normalizedAndIndented() {
        // NEL (U+0085) - Next Line
        String input = "Line1\u0085Line2\u0085Line3";
        String expected = "Line1\r\n Line2\r\n Line3";
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void multiline_withLS_normalizedAndIndented() {
        // LS (U+2028) - Line Separator
        String input = "Line1\u2028Line2\u2028Line3";
        String expected = "Line1\r\n Line2\r\n Line3";
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void multiline_withPS_normalizedAndIndented() {
        // PS (U+2029) - Paragraph Separator
        String input = "Line1\u2029Line2\u2029Line3";
        String expected = "Line1\r\n Line2\r\n Line3";
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void multiline_mixedSeparators_normalizedAndIndented() {
        // Test with a mix of different line separators
        String input = "Line1\nLine2\rLine3\r\nLine4\u000BLine5\u000CLine6\u0085Line7\u2028Line8\u2029Line9";
        String expected = "Line1\r\n Line2\r\n Line3\r\n Line4\r\n Line5\r\n Line6\r\n Line7\r\n Line8\r\n Line9";
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void emptyLines_withVariousSeparators_trimmedAndSkipped() {
        // Test empty lines with different separators
        String input = "Line1\n\nLine3\r\rLine5\u000B\u000BLine7";
        String expected = "Line1\r\n Line3\r\n Line5\r\n Line7";
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void longLine_withCRLF_wrapsAndIndents() {
        String input = "a".repeat(100) + "\r\n" + "b".repeat(100);
        String expected = "a".repeat(79) + "\r\n " + "a".repeat(21) + "\r\n " + "b".repeat(79) + "\r\n " + "b".repeat(21);
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void longLine_withMixedSeparators_wrapsAndIndents() {
        String input = "a".repeat(100) + "\n" + "b".repeat(100) + "\r" + "c".repeat(100);
        String expected = "a".repeat(79) + "\r\n " + "a".repeat(21) + "\r\n " + "b".repeat(79) + "\r\n " + "b".repeat(21) + "\r\n " + "c".repeat(79) + "\r\n " + "c".repeat(21);
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }
}
