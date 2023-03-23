package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeatureFlagsTest {

    @Test
    void testFlagDisabled() {
        assertFalse(FeatureFlags.TEST_MODE.enabled());
    }
    
    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "on", varArgs = "test_mode")
    void testFlagEnabled() {
        assertTrue(FeatureFlags.TEST_MODE.enabled());
    }

}