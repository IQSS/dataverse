package edu.harvard.iq.dataverse.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ListSplitUtilTest {

    @Test
    @DisplayName("split preserves empty tokens and quotes")
    void testSplitBasic() {
        List<String> tokens = ListSplitUtil.split("  a ,  b, \"c\" , , d  ");
        assertEquals(List.of("a", "b", "\"c\"", "", "d"), tokens);
    }

    @Test
    @DisplayName("splitToLowerCaseSet lowercases and de-dups (order not asserted)")
    void testSplitToLowerCaseSet() {
        assertTrue(ListSplitUtil.splitToLowerCaseSet(null).isEmpty(), "null should yield empty set");
        assertTrue(ListSplitUtil.splitToLowerCaseSet("   ").isEmpty(), "blank should yield empty set");
        Set<String> set = ListSplitUtil.splitToLowerCaseSet("B, a, b, A, C");
        assertEquals(Set.of("b", "a", "c"), set);

        Set<String> quoted = ListSplitUtil.splitToLowerCaseSet("\"A\" , \"b\" , \"A\"");
        assertEquals(Set.of("\"a\"", "\"b\""), quoted);
    }
}
