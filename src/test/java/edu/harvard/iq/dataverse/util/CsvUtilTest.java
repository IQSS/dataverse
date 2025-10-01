package edu.harvard.iq.dataverse.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CsvUtilTest {

    @Test
    @DisplayName("split preserves empty tokens and quotes")
    void testSplitBasic() {
        List<String> tokens = CsvUtil.split("  a ,  b, \"c\" , , d  ");
        assertEquals(List.of("a", "b", "\"c\"", "", "d"), tokens);
    }

    @Test
    @DisplayName("splitToLowerCaseSet lowercases and de-dups (order not asserted)")
    void testSplitToLowerCaseSet() {
        assertTrue(CsvUtil.splitToLowerCaseSet(null).isEmpty(), "null should yield empty set");
        assertTrue(CsvUtil.splitToLowerCaseSet("   ").isEmpty(), "blank should yield empty set");
        Set<String> set = CsvUtil.splitToLowerCaseSet("B, a, b, A, C");
        assertEquals(Set.of("b", "a", "c"), set);

        Set<String> quoted = CsvUtil.splitToLowerCaseSet("\"A\" , \"b\" , \"A\"");
        assertEquals(Set.of("\"a\"", "\"b\""), quoted);
    }
}
