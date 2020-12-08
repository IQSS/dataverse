package edu.harvard.iq.dataverse.settings.source;

import edu.harvard.iq.dataverse.settings.Setting;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DbSettingConfigSource implements ConfigSource {
    
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
        return getProperties().getOrDefault(key, null);
    }
    
    @Override
    public String getName() {
        return "DataverseDB";
    }
}
