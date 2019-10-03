package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.settings.FileSettingLocations.SettingLocationType;

import javax.enterprise.inject.Produces;

public class FileSettingLocationsFactory {

    // -------------------- LOGIC --------------------
    
    /**
     * Returns setting locations used in production. That is:
     * <p>
     * 1) Properties file in classpath: {@code /config/dataverse.default.properties }<br/>
     * 2) External properties file: {@code ${user.home}/.dataverse/dataverse.properties }<br/>
     */
    @Produces
    public FileSettingLocations buildSettingLocations() {
        FileSettingLocations settingLocations = new FileSettingLocations();
        settingLocations.addLocation(SettingLocationType.CLASSPATH, "/config/dataverse.default.properties", false);
        settingLocations.addLocation(SettingLocationType.FILESYSTEM, System.getProperty("user.home") + "/.dataverse/dataverse.properties", true);
        return settingLocations;
    }
}
