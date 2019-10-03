package edu.harvard.iq.dataverse.settings;

import javax.enterprise.inject.Vetoed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Locations of properties setting files that need to be
 * loaded on application startup.
 * 
 * @author madryk
 */
@Vetoed
public class FileSettingLocations {
    
    public enum SettingLocationType {
        CLASSPATH,
        FILESYSTEM
    }

    private List<SettingLocation> settingLocations = new ArrayList<>();
    
    // -------------------- GETTERS --------------------
    
    public List<SettingLocation> getSettingLocations() {
        return Collections.unmodifiableList(settingLocations);
    }

    // -------------------- LOGIC --------------------
    
    public void addLocation(SettingLocationType locationType, String path, boolean isOptional) {
        settingLocations.add(new SettingLocation(locationType, path, isOptional));
    }


    // -------------------- INNER CLASSES --------------------
    
    public static class SettingLocation {
        private SettingLocationType locationType;
        private String path;
        private boolean isOptional;
        
        // -------------------- CONSTRUCTORS --------------------
        
        private SettingLocation(SettingLocationType locationType, String path, boolean isOptional) {
            super();
            this.locationType = locationType;
            this.path = path;
            this.isOptional = isOptional;
        }
        
        // -------------------- GETTERS --------------------
        
        public SettingLocationType getLocationType() {
            return locationType;
        }
        public String getPath() {
            return path;
        }
        public boolean isOptional() {
            return isOptional;
        }
        
    }
}
