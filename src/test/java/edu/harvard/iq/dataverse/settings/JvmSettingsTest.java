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
    @SystemProperty(key = "doi.username", value = "test")
    void lookupSettingViaAlias() {
        assertEquals("test", JvmSettings.DATACITE_USERNAME.lookup());
    }
    
    @Test
    @SystemProperty(key = "doi.baseurlstring", value = "test")
    void lookupSettingViaAliasWithDefaultInMPCFile() {
        assertEquals("test", JvmSettings.DATACITE_MDS_API_URL.lookup());
    }
    
    @Test
    @SystemProperty(key = "doi.dataciterestapiurlstring", value = "foo")
    @SystemProperty(key = "doi.mdcbaseurlstring", value = "bar")
    void lookupSettingViaAliasWithDefaultInMPCFileAndTwoAliases() {
        assertEquals("foo", JvmSettings.DATACITE_REST_API_URL.lookup());
    }

}