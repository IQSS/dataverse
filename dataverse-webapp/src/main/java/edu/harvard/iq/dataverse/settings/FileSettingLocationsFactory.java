package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.settings.FileSettingLocations.SettingLocationType;

import javax.enterprise.inject.Produces;

import static edu.harvard.iq.dataverse.settings.FileSettingLocations.*;

public class FileSettingLocationsFactory {

    // -------------------- LOGIC --------------------

    /**
     * Returns setting locations used in production. That is (at least):
     * <p>
     * 1) Properties file in classpath: {@code /config/dataverse.default.properties }<br/>
     * 2) External properties file: {@code ${user.home}/.dataverse/dataverse.properties }<br/>
     */
    @Produces
    public FileSettingLocations buildSettingLocations() {
        return new FileSettingLocations()
                .addLocation(1, SettingLocationType.CLASSPATH,
                        "/config/dataverse.default.properties", PathType.DIRECT, false)
                .addLocation(2,SettingLocationType.FILESYSTEM,
                        System.getProperty("user.home") + "/.dataverse/dataverse.properties", PathType.DIRECT, true)
                .addLocation(3, SettingLocationType.FILESYSTEM,
                        ":SamlPropertiesPath", PathType.PROPERTY, false)
                .addFallbackLocation(3, SettingLocationType.CLASSPATH,
                        "/config/saml.properties", PathType.DIRECT);
    }
}
