package edu.harvard.iq.dataverse.settings.source;

import edu.harvard.iq.dataverse.settings.Setting;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * A caching wrapper around SettingServiceBean to provide database settings to MicroProfile Config API.
 * Please be aware that this class relies on dependency injection during the application startup.
 * Values will not be available before and a severe message will be logged to allow monitoring (potential race conditions)
 * The settings will be cached for at least one minute, avoiding unnecessary database calls.
 */
public class DbSettingConfigSource implements ConfigSource {
    
    private static final Logger logger = Logger.getLogger(DbSettingConfigSource.class.getCanonicalName());
    private static final ConcurrentHashMap<String, String> properties = new ConcurrentHashMap<>();
    private static Instant lastUpdate;
    private static SettingsServiceBean settingsSvc;
    static final String PREFIX = "dataverse.settings.fromdb";
    
    /**
     * Let the SettingsServiceBean be injected by DbSettingConfigHelper with PostConstruct
     * @param injected
     */
    public static void injectSettingsService(SettingsServiceBean injected) {
       settingsSvc = injected;
       updateProperties();
    }
    
    /**
     * Retrieve settings from the database via service and update cache.
     */
    public static void updateProperties() {
        // skip if the service has not been injected yet
        if (settingsSvc == null)
            return;
        
        Set<Setting> dbSettings = settingsSvc.listAll();
        dbSettings.forEach(s -> properties.put(PREFIX+"."+s.getName()+ (s.getLang() == null ? "" : "."+s.getLang()), s.getContent()));
        lastUpdate = Instant.now();
    }
    
    @Override
    public Map<String, String> getProperties() {
        // if the cache is at least XX number of seconds old, update before serving data.
        if (lastUpdate == null || Instant.now().minus(Duration.ofSeconds(60)).isBefore(lastUpdate)) {
            updateProperties();
        }
        return properties;
    }
    
    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }
    
    @Override
    public int getOrdinal() {
        return 50;
    }
    
    @Override
    public String getValue(String key) {
        // log usages for which this has been designed, but not yet ready to serve...
        if (settingsSvc == null && key.startsWith(PREFIX)) {
            logger.severe("MPCONFIG DbSettingConfigSource not ready yet, but requested for '"+key+"'.");
        }
        return getProperties().getOrDefault(key, null);
    }
    
    @Override
    public String getName() {
        return "DataverseDB";
    }
}
