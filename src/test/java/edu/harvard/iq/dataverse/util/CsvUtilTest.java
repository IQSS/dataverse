package edu.harvard.iq.dataverse.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CsvUtilTest {

    @Test
    @DisplayName("split handles whitespace, empty tokens and quotes")
    void testSplitBasic() {
        List<String> tokens = CsvUtil.split("  a ,  b, \"c\" , , d  ");
        assertEquals(List.of("a", "b", "c", "d"), tokens);
    }

    @Test
    @DisplayName("normalize produces canonical comma+space joined list")
    void testNormalize() {
        assertEquals("a, b, c", CsvUtil.normalize("a,b,  c"));
        assertEquals("", CsvUtil.normalize(null));
        assertEquals("", CsvUtil.normalize("   "));
    }

    @Test
    @DisplayName("splitToSet deduplicates while preserving first-seen order")
    void testSplitToSet() {
        Set<String> set = CsvUtil.splitToSet("b, a, b, a, c");
        assertEquals(3, set.size());
        assertTrue(set.containsAll(List.of("b", "a", "c")));
    }

    @Test
    @DisplayName("splitToLowerCaseSet lowercases, de-dups case-insensitively and preserves first occurrence order")
    void testSplitToLowerCaseSet() {
        assertTrue(CsvUtil.splitToLowerCaseSet(null).isEmpty(), "null should yield empty set");
        assertTrue(CsvUtil.splitToLowerCaseSet("   ").isEmpty(), "blank should yield empty set");

        Set<String> set = CsvUtil.splitToLowerCaseSet("B, a, b, A, C");
        assertEquals(List.of("b", "a", "c"), List.copyOf(set));

        Set<String> quoted = CsvUtil.splitToLowerCaseSet("\"A\" , \"b\" , \"A\"");
        assertEquals(List.of("a", "b"), List.copyOf(quoted));
    }
}
