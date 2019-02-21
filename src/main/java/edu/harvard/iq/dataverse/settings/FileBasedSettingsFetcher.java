package edu.harvard.iq.dataverse.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.commons.lang3.StringUtils;

/**
 * Service responsible for loading and serving application settings
 * defined in property files. 
 * 
 * @author madryk
 */
@Startup
@Singleton
public class FileBasedSettingsFetcher {

	private final static String DEFAULT_PROPERTIES_CLASSPATH_LOCATION = "/config/dataverse.default.properties";
	private final static String CUSTOM_PROPERTIES_LOCATION = System.getProperty("user.home") + "/.dataverse/dataverse.properties";
	
	private static final String BUILTIN_USERS_KEY_SETTING = "BuiltinUsers.KEY";
	
	
	private Map<String, String> settings = new HashMap<>();
	
	// -------------------- LOGIC --------------------
	
	/**
	 * Loads settings from files:<br/>
	 * 1) Properties file in classpath: {@code /dataverse.default.properties }<br/>
	 * 2) External properties file: {@code ${user.home}/.dataverse/dataverse.properties }<br/>
	 * <br/>
	 * If some setting is defined in multiple files above, then
	 * the last occurrence of this setting will take precedence
	 * (setting from 2nd file overrides setting from 1st).
	 */
	@PostConstruct
	public void loadSettings() {
		Properties properties = new Properties();
		try (InputStream propertiesInputStream = this.getClass().getResourceAsStream(DEFAULT_PROPERTIES_CLASSPATH_LOCATION)) {
			properties.load(propertiesInputStream);
		} catch (IOException e) {
			throw new RuntimeException("Unable to read properties from classpath: " + DEFAULT_PROPERTIES_CLASSPATH_LOCATION, e);
		}
		
		File customProperties = new File(CUSTOM_PROPERTIES_LOCATION);
		if (customProperties.exists() && customProperties.isFile()) {
			try (InputStream customPropInputStream = new FileInputStream(customProperties)) {
				Properties customProp = new Properties();
				customProp.load(customPropInputStream);
				properties.putAll(customProp);
			} catch (IOException e) {
				throw new RuntimeException("Unable to read properties from: " + CUSTOM_PROPERTIES_LOCATION, e);
			}
		}
		
		properties.forEach((key, value) -> this.settings.put(
				convertPropertiesKeyToSettingName((String)key),
				sanitizeSettingValue((String)value)));
	}
	
	/**
	 * Returns setting value with the given key
	 */
	public String getSetting(String key) {
		return settings.get(key);
	}
	
	/**
	 * Returns all defined settings
	 */
	public Map<String, String> getAllSettings() {
		return Collections.unmodifiableMap(settings);
	}
	
	
	// -------------------- PRIVATE --------------------
	
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
