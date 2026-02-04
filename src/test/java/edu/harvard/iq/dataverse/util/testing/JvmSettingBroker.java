package edu.harvard.iq.dataverse.util.testing;

import java.io.IOException;

/**
 * Provide an interface to access and manipulate {@link edu.harvard.iq.dataverse.settings.JvmSettings}
 * at some place (local, remote, different ways to access, etc.).
 * Part of the {@link JvmSettingExtension} extension to allow JUnit5 tests to manipulate these
 * settings, enabling to test different code paths and so on.
 * @implNote Keep in mind to use methods that do not require restarts or similar to set or delete a setting.
 *           This must be changeable on the fly, otherwise it will be useless for testing.
 *           Yes, non-hot-reloadable settings may be a problem. The code should be refactored in these cases.
 */
public interface JvmSettingBroker {
    
    /**
     * Receive the value of a {@link edu.harvard.iq.dataverse.settings.JvmSettings} given as its {@link String}
     * representation. The reason for this is that we may have inserted variable names already.
     * @param key The JVM setting to receive as key, e.g. "dataverse.fqdn".
     * @return The value of the setting if present or null.
     * @throws IOException When communication goes sideways.
     */
    String getJvmSetting(String key) throws IOException;
    
    /**
     * Set the value of a {@link edu.harvard.iq.dataverse.settings.JvmSettings} (given as its {@link String}
     * representation). The reason for this is that we may have inserted variable names already.
     * @param key The JVM setting to receive as key, e.g. "dataverse.fqdn".
     * @param value The JVM setting's value we want to have it set to.
     * @throws IOException When communication goes sideways.
     */
    void setJvmSetting(String key, String value) throws IOException;
    
    /**
     * Remove the value of a {@link edu.harvard.iq.dataverse.settings.JvmSettings} (given as its {@link String}
     * representation). For some tests, one might want to clear a certain setting again and potentially have it set
     * back afterward. The reason for this is that we may have inserted variable names already.
     * @param key The JVM setting to receive as key, e.g. "dataverse.fqdn".
     * @throws IOException When communication goes sideways.
     */
    String deleteJvmSetting(String key) throws IOException;
    
}