package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@LocalJvmSettings
class UserServiceBeanLastApiUseTest {

    private static final Instant NOW = Instant.parse("2026-11-02T12:00:00Z");

    private static Timestamp secondsAgo(long seconds) {
        return Timestamp.from(NOW.minusSeconds(seconds));
    }

    @Test
    void alwaysUpdatesWithoutFeatureFlag() {
        assertTrue(UserServiceBean.isLastApiUseTimeUpdateNeeded(secondsAgo(1), NOW));
        assertTrue(UserServiceBean.isLastApiUseTimeUpdateNeeded(null, NOW));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "delay-lastapiuse-update")
    void usesDefaultIntervalWithFeatureFlag() {
        assertTrue(UserServiceBean.isLastApiUseTimeUpdateNeeded(null, NOW));
        assertFalse(UserServiceBean.isLastApiUseTimeUpdateNeeded(secondsAgo(60), NOW));
        assertFalse(UserServiceBean.isLastApiUseTimeUpdateNeeded(secondsAgo(3599), NOW));
        assertTrue(UserServiceBean.isLastApiUseTimeUpdateNeeded(secondsAgo(3600), NOW));
        assertTrue(UserServiceBean.isLastApiUseTimeUpdateNeeded(secondsAgo(86400), NOW));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "delay-lastapiuse-update")
    @JvmSetting(key = JvmSettings.API_LAST_API_USE_INTERVAL, value = "300")
    void usesConfiguredIntervalWithFeatureFlag() {
        assertFalse(UserServiceBean.isLastApiUseTimeUpdateNeeded(secondsAgo(299), NOW));
        assertTrue(UserServiceBean.isLastApiUseTimeUpdateNeeded(secondsAgo(300), NOW));
    }

    @Test
    @JvmSetting(key = JvmSettings.API_LAST_API_USE_INTERVAL, value = "300")
    void ignoresIntervalWithoutFeatureFlag() {
        assertTrue(UserServiceBean.isLastApiUseTimeUpdateNeeded(secondsAgo(10), NOW));
    }
}
