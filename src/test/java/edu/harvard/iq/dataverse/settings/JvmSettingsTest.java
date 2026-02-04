package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import edu.harvard.iq.dataverse.util.testing.SystemProperty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@LocalJvmSettings
class JvmSettingsTest {
    @Test
    @JvmSetting(key = JvmSettings.VERSION, value = "foobar")
    void lookupSetting() {
        assertEquals("foobar", JvmSettings.VERSION.lookup());
        assertEquals("foobar", JvmSettings.VERSION.lookupOptional().orElse(""));
    }
    
    @Test
    @SystemProperty(key = "dataverse.pid.datacite.datacite.username", value = "test")
    void lookupPidProviderSetting() {
        assertEquals("test", JvmSettings.DATACITE_USERNAME.lookup("datacite"));
    }
    
    @Test
    @SystemProperty(key = "dataverse.ingest.rserve.port", value = "1234")
    void lookupSettingViaAliasWithDefaultInMPCFile() {
        assertEquals("1234", JvmSettings.RSERVE_PORT.lookup());
    }

}