package edu.harvard.iq.dataverse.settings.source;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class AliasConfigSource implements ConfigSource {
    
    private static final Logger logger = Logger.getLogger(AliasConfigSource.class.getName());
    
    private final ConcurrentHashMap<String, String> aliases = new ConcurrentHashMap<>();
    private final String ALIASES_PROP_FILE = "aliases-config.properties";
    
    public AliasConfigSource() {
        try {
            // get resource from classpath
            ClassLoader classLoader = this.getClass().getClassLoader();
            URL aliasesResource = classLoader.getResource(ALIASES_PROP_FILE);
            
            // load properties from file resource (parsing included)
            Properties aliasProps = new Properties();
            aliasProps.load(aliasesResource.openStream());
    
            // store in our aliases map
            aliasProps.forEach((key, value) -> aliases.put(key.toString(), value.toString()));
        } catch (IOException e) {
            logger.info("Could not read from "+ALIASES_PROP_FILE+". Skipping MPCONFIG alias setup.");
        }
    }
    
    @Override
    public Map<String, String> getProperties() {
        // No, just no. We're not going to drop a list of stuff. We're only
        // dealiasing on getValue();
        return new HashMap<>();
    }
    
    @Override
    public Set<String> getPropertyNames() {
        // It does not make sense to retrieve the aliased names...
        return new HashSet<>();
    }
    
    @Override
    public int getOrdinal() {
        // Any other config source can override us.
        // As soon as someone is starting to use the new property name, this alias becomes pointless.
        return Integer.MIN_VALUE;
    }
    
    @Override
    public String getValue(String key) {
        String value = null;
        if (this.aliases.contains(key)) {
            String oldKey = this.aliases.get(key);
            value = ConfigProvider.getConfig().getOptionalValue(oldKey, String.class).orElse(null);
            
            if (value != null) {
                logger.warning("Detected deprecated config option '"+oldKey+"' in use. Please update your config to use '"+key+"'.");
            }
        }
        return value;
    }
    
    @Override
    public String getName() {
        return "Alias";
    }
}