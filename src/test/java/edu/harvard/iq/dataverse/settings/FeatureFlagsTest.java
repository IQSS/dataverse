package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeatureFlagsTest {

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "false", varArgs = "api-session-auth")
    void testFlagDisabled() {
        assertFalse(FeatureFlags.API_SESSION_AUTH.enabled());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth")
    void testFlagEnabled() {
        assertTrue(FeatureFlags.API_SESSION_AUTH.enabled());
    }
}
