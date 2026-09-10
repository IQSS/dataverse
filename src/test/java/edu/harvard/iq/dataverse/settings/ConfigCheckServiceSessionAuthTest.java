package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@LocalJvmSettings
class ConfigCheckServiceSessionAuthTest {

    private static final String SITE_URL = "https://demo.dataverse.org";

    private final List<LogRecord> logRecords = new ArrayList<>();
    private final Logger logger = Logger.getLogger(ConfigCheckService.class.getCanonicalName());
    private Handler captureHandler;
    private ConfigCheckService sut;

    @BeforeEach
    void setUp() {
        sut = new ConfigCheckService();
        captureHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logRecords.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(captureHandler);
    }

    @AfterEach
    void tearDown() {
        logger.removeHandler(captureHandler);
    }

    private long countAtLevel(Level level) {
        return logRecords.stream().filter(r -> r.getLevel().equals(level)).count();
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "false", varArgs = "api-session-auth-hardening")
    void testCheckSessionAuthHardening_flagDisabledIsSilent() {
        sut.checkSessionAuthHardening();

        assertEquals(0, logRecords.size());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth")
    @JvmSetting(key = JvmSettings.SITE_URL, value = SITE_URL)
    void testCheckSessionAuthHardening_fullyConfiguredIsSilent() {
        sut.checkSessionAuthHardening();

        assertEquals(0, logRecords.size());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "false", varArgs = "api-session-auth")
    @JvmSetting(key = JvmSettings.SITE_URL, value = SITE_URL)
    void testCheckSessionAuthHardening_warnsWhenSessionAuthIsOff() {
        sut.checkSessionAuthHardening();

        assertEquals(1, countAtLevel(Level.WARNING));
        assertTrue(logRecords.get(0).getMessage().contains("API_SESSION_AUTH"));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth")
    @JvmSetting(key = JvmSettings.SITE_URL, value = "not a url")
    void testCheckSessionAuthHardening_severeWhenSiteUrlIsUnusable() {
        sut.checkSessionAuthHardening();

        assertEquals(1, countAtLevel(Level.SEVERE));
    }
}
