package edu.harvard.iq.dataverse.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CsvUtilTest {

    @Test
    @DisplayName("split handles whitespace and empty tokens; does not alter quotes")
    void testSplitBasic() {
        List<String> tokens = CsvUtil.split("  a ,  b, \"c\" , , d  ");
        assertEquals(List.of("a", "b", "\"c\"", "d"), tokens);
    }

    @Test
    @DisplayName("splitToLowerCaseSet lowercases, de-dups and preserves first occurrence order (quotes preserved)")
    void testSplitToLowerCaseSet() {
        assertTrue(CsvUtil.splitToLowerCaseSet(null).isEmpty(), "null should yield empty set");
        assertTrue(CsvUtil.splitToLowerCaseSet("   ").isEmpty(), "blank should yield empty set");

        Set<String> set = CsvUtil.splitToLowerCaseSet("B, a, b, A, C");
        assertEquals(List.of("b", "a", "c"), List.copyOf(set));

        Set<String> quoted = CsvUtil.splitToLowerCaseSet("\"A\" , \"b\" , \"A\"");
        // Quotes are preserved then lowercased; duplicates removed based on full token.
        assertEquals(List.of("\"a\"", "\"b\""), List.copyOf(quoted));
    }
}
