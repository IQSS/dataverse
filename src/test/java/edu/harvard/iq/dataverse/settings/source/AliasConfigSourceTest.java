package edu.harvard.iq.dataverse.settings.source;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AliasConfigSourceTest {
    
    @BeforeAll
    static void setUp() {
        System.setProperty("former.hello", "my-fair-lady");
    }
    
    @AfterAll
    static void tearDown() {
        System.clearProperty("former.hello");
    }
    
    @Test
    void testAliasFromJvmSettings() {
        assertEquals("my-fair-lady", JvmSettings.TEST_ALIAS_CONFIG_SOURCE.lookup());
    }
    
}