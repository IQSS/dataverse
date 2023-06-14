package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
class VersionPidModeTest {
    
    @Test
    @JvmSetting(key = JvmSettings.PID_VERSIONS_MODE, value = "allow-minor")
    void setToValidValue() {
        assertEquals(VersionPidMode.ALLOW_MINOR, JvmSettings.PID_VERSIONS_MODE.lookup(VersionPidMode.class));
    }
    
    @Test
    @JvmSetting(key = JvmSettings.PID_VERSIONS_MODE, value = "ALLOW-major")
    void setToOtherValidValue() {
        assertEquals(VersionPidMode.ALLOW_MAJOR, JvmSettings.PID_VERSIONS_MODE.lookup(VersionPidMode.class));
    }
    
    @Test
    @JvmSetting(key = JvmSettings.PID_VERSIONS_MODE, value = "foobar")
    void setToInvalidValue() {
        assertThrows(NoSuchElementException.class, () -> JvmSettings.PID_VERSIONS_MODE.lookup(VersionPidMode.class));
    }
    
}