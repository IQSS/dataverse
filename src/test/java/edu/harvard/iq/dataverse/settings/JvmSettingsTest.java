package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JvmSettingsTest {
    @Test
    @JvmSetting(key = JvmSettings.VERSION, value = "foobar")
    void lookupSetting() {
        assertEquals("foobar", JvmSettings.VERSION.lookup());
        assertEquals("foobar", JvmSettings.VERSION.lookupOptional().orElse(""));
    }
    
    /*
     * TODO: add more tests here for features like old names, patterned settings etc when adding
     *       these in other pull requests adding new settings making use of these features.
     */
}