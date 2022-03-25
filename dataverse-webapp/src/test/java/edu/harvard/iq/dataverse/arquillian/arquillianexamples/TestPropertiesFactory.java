package edu.harvard.iq.dataverse.arquillian.arquillianexamples;

import edu.harvard.iq.dataverse.settings.FileSettingLocations;
import edu.harvard.iq.dataverse.settings.FileSettingLocations.PathType;
import edu.harvard.iq.dataverse.settings.FileSettingLocations.SettingLocationType;
import edu.harvard.iq.dataverse.settings.FileSettingLocationsFactory;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Specializes;

public class TestPropertiesFactory extends FileSettingLocationsFactory {

    @Override
    @Produces @Specializes
    public FileSettingLocations buildSettingLocations() {
        return new FileSettingLocations()
                .addLocation(1, SettingLocationType.CLASSPATH,
                        "/config/dataverse.default.properties", PathType.DIRECT, false)
                .addLocation(2, SettingLocationType.CLASSPATH,
                        "/config/dataverse.test.properties", PathType.DIRECT, true);
    }
}
