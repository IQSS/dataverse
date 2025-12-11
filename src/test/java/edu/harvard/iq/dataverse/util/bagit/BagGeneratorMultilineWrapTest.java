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
        String expected = "a".repeat(78) + "\r\n " + "a".repeat(22);
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void multiline_input_indentsSecondAndSubsequentOriginalLines() {
        String input = "Line1\nLine2";
        String expected = "Line1\r\n Line2";
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    void multiline_withCRLF_normalizedAndIndented() {
        String input = "First line\r\nSecond line";
        String expected = "First line\r\n Second line";
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
        String expected = "Header\r\n " + "b".repeat(78) + "\r\n " + "b".repeat(12);
        String out = callMultilineWrap(input);
        assertThat(out).isEqualTo(expected);
    }
}
