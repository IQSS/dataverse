package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.settings.FileSettingLocations.SettingLocation;
import edu.harvard.iq.dataverse.settings.FileSettingLocations.SettingLocationType;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Service responsible for loading and serving application settings
 * defined in property files.
 *
 * @author madryk
 */
@Startup
@Singleton
public class FileBasedSettingsFetcher {

    private static final String BUILTIN_USERS_KEY_SETTING = "BuiltinUsers.KEY";

    private FileSettingLocations fileSettingLocations;


    private Map<String, String> settings = new HashMap<>();


    // -------------------- CONSTRUCTORS --------------------
    
    public FileBasedSettingsFetcher() {
        // JEE requirement
    }
    
    @Inject
    public FileBasedSettingsFetcher(FileSettingLocations fileSettingLocations) {
        this.fileSettingLocations = fileSettingLocations;
    }
    
    // -------------------- LOGIC --------------------

    /**
     * Loads settings from files defined in {@link FileSettingLocations}<br/>
     * <br/>
     * If some setting is defined in multiple files, then
     * the last occurrence of this setting will take precedence
     * (setting from 2nd file overrides setting from 1st).
     */
    @PostConstruct
    public void loadSettings() {
        
        Properties allProperties = new Properties();
        
        for (SettingLocation settingLocation: fileSettingLocations.getSettingLocations()) {
            
            Properties properties = new Properties();
            
            if (settingLocation.getLocationType() == SettingLocationType.CLASSPATH) {
                properties = loadClasspathProperties(settingLocation.getPath(), settingLocation.isOptional());
            } else if (settingLocation.getLocationType() == SettingLocationType.FILESYSTEM) {
                properties = loadFilesystemProperties(settingLocation.getPath(), settingLocation.isOptional());
            } else {
                throw new RuntimeException("Not supported setting location type: " + settingLocation.getLocationType());
            }
            
            allProperties.putAll(properties);
        }

        allProperties.forEach((key, value) -> this.settings.put(
                convertPropertiesKeyToSettingName((String) key),
                sanitizeSettingValue((String) value)));
    }

    /**
     * Returns setting value with the given key
     */
    public String getSetting(String key) {
        String setting = settings.get(key);
        return setting == null ? StringUtils.EMPTY : setting;
    }

    /**
     * Returns all defined settings
     */
    public Map<String, String> getAllSettings() {
        return Collections.unmodifiableMap(settings);
    }


    // -------------------- PRIVATE --------------------

    private Properties loadClasspathProperties(String classpath, boolean isOptional) {
        Properties properties = new Properties();
        try (InputStream propertiesInputStream = this.getClass().getResourceAsStream(classpath)) {
            
            if (propertiesInputStream == null) {
                
                if (!isOptional) {
                    throw new RuntimeException("Required properties file not found: " + classpath);
                }
                return properties;
            }
            
            properties.load(propertiesInputStream);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read properties from classpath: " + classpath, e);
        }
        return properties;
    }
    
    private Properties loadFilesystemProperties(String path, boolean isOptional) {
        Properties properties = new Properties();
        File propertiesFile = new File(path);

        if (!(propertiesFile.exists() && propertiesFile.isFile())) {
            if (!isOptional) {
                throw new RuntimeException("Required properties file not found: " + path);
            }
            return properties;
        }
        
        try (InputStream customPropInputStream = new FileInputStream(propertiesFile)) {
            properties.load(customPropInputStream);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read properties from: " + path, e);
        }
        
        return properties;
    }
    
    private String convertPropertiesKeyToSettingName(String key) {
        if (key.equals(BUILTIN_USERS_KEY_SETTING)) { // For some reason this setting doesn't have ':' prefix
            return BUILTIN_USERS_KEY_SETTING;
        }
        return ":" + key;
    }

    private String sanitizeSettingValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        return value;
    }
}
