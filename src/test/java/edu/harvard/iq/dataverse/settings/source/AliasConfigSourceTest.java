package edu.harvard.iq.dataverse.settings.source;

import edu.harvard.iq.dataverse.util.testing.SystemProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AliasConfigSourceTest {
    
    static AliasConfigSource source = new AliasConfigSource();
    
    static final String VALUE = "test1234";
    
    static final String SIMPLE_NEW = "dataverse.test.foobar";
    static final String SIMPLE_OLD = "dataverse.former.hello";
    static final String SIMPLE_MULTI_NEW = "dataverse.test.multi";
    static final String SIMPLE_MULTI_OLD = "former.2";
    
    static final Pattern PATTERNED_NEW_PATTERN = Pattern.compile("dataverse\\.(.+?)\\.next\\.test");
    static final String PATTERNED_NEW_NAME = "dataverse.single.next.test";
    static final String PATTERNED_OLD_PATTERN = "dataverse.%s.test.foobar";
    static final String PATTERNED_OLD_NAME = "dataverse.single.test.foobar";
    
    static final Pattern DUAL_PATTERNED_NEW_PATTERN = Pattern.compile("dataverse\\.(.+?)\\.two\\.(.+?)\\.foobar");
    static final String DUAL_PATTERNED_NEW_NAME = "dataverse.single.two.test.foobar";
    static final String DUAL_PATTERNED_OLD_PATTERN = "dataverse.%s.test.%s.foobar";
    static final String DUAL_PATTERNED_OLD_NAME = "dataverse.single.test.test.foobar";
    
    static final Pattern PATTERNED_MULTI_NEW_PATTERN = Pattern.compile("dataverse\\.(.+?)\\.next\\.test2");
    static final String PATTERNED_MULTI_NEW_NAME = "dataverse.multi.next.test2";
    static final String PATTERNED_MULTI_OLD_PATTERN = "dataverse.%s.test.foobar2";
    static final String PATTERNED_MULTI_OLD_NAME = "dataverse.multi.test.foobar2";
    
    static final Pattern DUALSINGLE_PATTERNED_NEW_PATTERN = Pattern.compile("dataverse\\.(.+?)\\.(.+?)\\.test2");
    static final String DUALSINGLE_PATTERNED_NEW_NAME = "dataverse.multi.next.test2";
    static final String DUALSINGLE_PATTERNED_OLD_PATTERN = "dataverse.%s.test.foobar2";
    static final String DUALSINGLE_PATTERNED_OLD_NAME = "dataverse.multi.test.foobar2";
    
    @BeforeAll
    static void setUp() {
        source.addAlias(SIMPLE_NEW, List.of(SIMPLE_OLD));
        source.addAlias(SIMPLE_MULTI_NEW, List.of("former.1", SIMPLE_MULTI_OLD, "former.3"));
        source.addAlias(PATTERNED_NEW_PATTERN, List.of(PATTERNED_OLD_PATTERN));
        source.addAlias(DUAL_PATTERNED_NEW_PATTERN, List.of(DUAL_PATTERNED_OLD_PATTERN));
        source.addAlias(PATTERNED_MULTI_NEW_PATTERN, List.of("dataverse.%s.test1.foobar1", PATTERNED_MULTI_OLD_PATTERN, "dataverse.test.%s.test"));
        source.addAlias(DUALSINGLE_PATTERNED_NEW_PATTERN, List.of(DUALSINGLE_PATTERNED_OLD_PATTERN));
    }
    
    @Test
    void testNullIfNotInScope() {
        assertNull(source.getValue(null));
        assertNull(source.getValue("test.out.of.scope"));
    }
    
    @Test
    @SystemProperty(key = SIMPLE_OLD, value = VALUE)
    void testSimpleAlias() {
        assertEquals(VALUE, source.getValue(SIMPLE_NEW));
    }
    
    @Test
    @SystemProperty(key = SIMPLE_MULTI_OLD, value = VALUE)
    void testSimpleMultipleAlias() {
        assertEquals(VALUE, source.getValue(SIMPLE_MULTI_NEW));
    }
    
    @Test
    @SystemProperty(key = PATTERNED_OLD_NAME, value = VALUE)
    void testPatternedAlias() {
        assertEquals(VALUE, source.getValue(PATTERNED_NEW_NAME));
    }
    
    @Test
    @SystemProperty(key = DUAL_PATTERNED_OLD_NAME, value = VALUE)
    void testDualPatternedAlias() {
        assertEquals(VALUE, source.getValue(DUAL_PATTERNED_NEW_NAME));
    }
    
    @Test
    @SystemProperty(key = PATTERNED_MULTI_OLD_NAME, value = VALUE)
    void testPatternedMultipleAlias() {
        assertEquals(VALUE, source.getValue(PATTERNED_MULTI_NEW_NAME));
    }
    
    @Test
    @SystemProperty(key = DUALSINGLE_PATTERNED_OLD_NAME, value = VALUE)
    void testDualSinglePatternedAlias() {
        assertEquals(VALUE, source.getValue(DUALSINGLE_PATTERNED_NEW_NAME));
    }
}