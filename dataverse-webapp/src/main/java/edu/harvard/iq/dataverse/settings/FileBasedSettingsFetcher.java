package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.settings.FileSettingLocations.SettingLocation;
import edu.harvard.iq.dataverse.settings.FileSettingLocations.SettingLocationType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    private static final Logger logger = LoggerFactory.getLogger(FileBasedSettingsFetcher.class);

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
        ArrayList<SettingLocation> settingLocations = new ArrayList<>(fileSettingLocations.getSettingLocations());
        settingLocations.sort(Comparator.comparingInt(SettingLocation::getOrder));
        Map<Integer, SettingLocation> fallbackLocations = fileSettingLocations.getFallbackLocations();
        for (SettingLocation settingLocation : fileSettingLocations.getSettingLocations()) {
            Optional<Properties> properties = loadProperties(settingLocation);
            if (!properties.isPresent()) {
                SettingLocation fallback = fallbackLocations.get(settingLocation.getOrder());
                logger.warn("Cannot load properties from primary location: " + settingLocation
                        + ". Fallback: " + fallback);
                properties = loadProperties(fallback);
            }
            if (!properties.isPresent() && !settingLocation.isOptional()) {
                logger.error("Cannot load properties for location: " + settingLocation);
                throw new RuntimeException("Cannot load mandatory properties");
            }
            properties.orElseGet(Properties::new)
                    .forEach((key, value) -> this.settings.put(
                        convertPropertiesKeyToSettingName((String) key),
                        sanitizeSettingValue((String) value)));
        }
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

    private Optional<Properties> loadProperties(SettingLocation settingLocation) {
        if (settingLocation == null) {
            logger.warn("Null location received. Returning empty result.");
            return Optional.empty();
        }
        switch (settingLocation.getLocationType()) {
            case FILESYSTEM:
                return loadPropertiesFromFile(settingLocation);
            case CLASSPATH:
                return loadPropertiesFromClasspath(settingLocation);
            default:
                throw new RuntimeException("Not supported setting location type: " + settingLocation.getLocationType());
        }
    }

    private Optional<Properties> loadPropertiesFromClasspath(SettingLocation settingLocation) {
        Properties properties = new Properties();
        String classpath = readPath(settingLocation);
        if (StringUtils.isBlank(classpath)) {
            logger.warn("Blank classpath for property file location of " + settingLocation);
            return Optional.empty();
        }
        try (InputStream propertiesInputStream = this.getClass().getResourceAsStream(classpath)) {
            if (propertiesInputStream == null) {
                logger.error("Empty stream while trying to read properties from " + classpath);
                return Optional.empty();
            }
            properties.load(propertiesInputStream);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read properties from classpath: " + classpath, e);
        }
        return Optional.of(properties);
    }

    private Optional<Properties> loadPropertiesFromFile(SettingLocation settingLocation) {
        Properties properties = new Properties();
        String path = readPath(settingLocation);
        if (StringUtils.isBlank(path)) {
            logger.warn("Blank path for property file location of " + settingLocation);
            return Optional.empty();
        }
        File propertiesFile = new File(path);
        if (!(propertiesFile.exists() && propertiesFile.isFile())) {
            logger.error("Empty or nonexisting file encountered while trying to read properties from " + path);
            return Optional.empty();
        }
        try (InputStream customPropInputStream = new FileInputStream(propertiesFile)) {
            properties.load(customPropInputStream);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read properties from: " + path, e);
        }
        return Optional.of(properties);
    }

    private String readPath(SettingLocation settingLocation) {
        switch (settingLocation.getPathType()) {
            case DIRECT:
                return settingLocation.getPath();
            case PROPERTY:
                return settings.get(settingLocation.getPath());
            default:
                throw new RuntimeException("Not supported setting path type: " + settingLocation.getPathType());
        }
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
