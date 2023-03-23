package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeatureFlagsTest {

    @Test
    void testFlagDisabled() {
        assertFalse(FeatureFlags.API_SESSION_AUTH.enabled());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "on", varArgs = "api-session-auth")
    void testFlagEnabled() {
        assertTrue(FeatureFlags.API_SESSION_AUTH.enabled());
    }
}
