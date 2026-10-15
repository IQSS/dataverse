package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@LocalJvmSettings
class ConfigCheckServiceReusableComponentsTest {

    private final ConfigCheckService sut = new ConfigCheckService();

    @Test
    void nothingEnabledIsQuiet() {
        assertDoesNotThrow(sut::checkReusableComponentsSetup);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "react-tree-view")
    void flagWithoutSessionAuthOrBaseUrlOnlyWarns() {
        assertDoesNotThrow(sut::checkReusableComponentsSetup);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "react-uploader")
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth")
    @JvmSetting(key = JvmSettings.REUSABLE_COMPONENTS_BASE_URL, value = "javascript:x")
    void unsafeBaseUrlOnlyWarns() {
        assertDoesNotThrow(sut::checkReusableComponentsSetup);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "react-uploader")
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth")
    @JvmSetting(key = JvmSettings.REUSABLE_COMPONENTS_BASE_URL, value = "/reusable-components")
    void completeSetupIsQuiet() {
        assertDoesNotThrow(sut::checkReusableComponentsSetup);
    }
}
