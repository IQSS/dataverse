package edu.harvard.iq.dataverse.util.bagit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class BagGeneratorTest {

    private static String invokeWrapAndIndent(BagGenerator instance, String input) throws Exception {
        Method m = BagGenerator.class.getDeclaredMethod("wrapAndIndent", String.class);
        m.setAccessible(true);
        return (String) m.invoke(instance, input);
    }

    @Test
    void wrapAndIndent_nullReturnsEmpty() throws Exception {
        BagGenerator bg = Mockito.mock(BagGenerator.class, Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
        String out = invokeWrapAndIndent(bg, null);
        assertEquals("", out);
    }

    @Test
    void wrapAndIndent_embeddedNewlinesAreIndented() throws Exception {
        BagGenerator bg = Mockito.mock(BagGenerator.class, Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
        String input = "Line1\nLine2\nLine3";
        String out = invokeWrapAndIndent(bg, input);
        String expected = "Line1\r\n  Line2\r\n  Line3";
        assertEquals(expected, out);
    }

    @Test
    void wrapAndIndent_longLineWrapsAt78WithTwoSpaceContinuation() throws Exception {
        BagGenerator bg = Mockito.mock(BagGenerator.class, Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
        String input = "a".repeat(100);

        String out = invokeWrapAndIndent(bg, input);

        String expected = "a".repeat(78) + "\r\n  " + "a".repeat(22);
        assertEquals(expected, out, "Expected wrapping and indenting after 78 chars");
    }

    @Test
    void wrapAndIndent_mixedNewlinesAndWraps() throws Exception {
        BagGenerator bg = Mockito.mock(BagGenerator.class, Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
        String base = "This is a line that will be followed by a newline and then a very long line:";
        String longText = "a".repeat(90);
        String input = base + "\n" + longText;

        String out = invokeWrapAndIndent(bg, input);
        String continuation = "\r\n  ";

        assertTrue(out.contains(continuation), "Output should contain CRLF + two space continuation");
        assertFalse(out.contains("\n") && !out.contains("\r\n"), "Raw LF without CR should not be present");

        // Exact structure: base + CRLF + two spaces + first 76 'a's + CRLF + two spaces + remaining 14 'a's
        String expected = base + continuation + longText.substring(0, 76) + continuation + longText.substring(76);
        assertEquals(expected, out);
    }
}
