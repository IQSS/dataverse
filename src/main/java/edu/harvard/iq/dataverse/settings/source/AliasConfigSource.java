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

/**
 * Enable using an old name for a new config name.
 * Usages will be logged and this source will ALWAYS stand back if the new name is used anywhere.
 *
 * By using a DbSettingConfigSource value (dataverse.settings.fromdb.XXX) as old name, we can
 * alias a new name to an old db setting, enabling backward compatibility.
 */
public final class AliasConfigSource implements ConfigSource {
    
    private static final Logger logger = Logger.getLogger(AliasConfigSource.class.getName());
    
    private final ConcurrentHashMap<String, String> aliases = new ConcurrentHashMap<>();
    private final String ALIASES_PROP_FILE = "META-INF/microprofile-aliases.properties";
    
    public AliasConfigSource() {
        try {
            // read properties from class path file
            Properties aliasProps = readAliases(ALIASES_PROP_FILE);
            // store in our aliases map
            importAliases(aliasProps);
        } catch (IOException e) {
            logger.info("Could not read from "+ALIASES_PROP_FILE+". Skipping MPCONFIG alias setup.");
        }
    }
    
    Properties readAliases(String filePath) throws IOException {
        // get resource from classpath
        ClassLoader classLoader = this.getClass().getClassLoader();
        URL aliasesResource = classLoader.getResource(filePath);
    
        // load properties from file resource (parsing included)
        Properties aliasProps = new Properties();
        try {
            aliasProps.load(aliasesResource.openStream());
        } catch (NullPointerException e) {
            throw new IOException(e.getMessage());
        }
        return aliasProps;
    }
    
    void importAliases(Properties aliasProps) {
        aliasProps.forEach((key, value) -> aliases.put(key.toString(), value.toString()));
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
        if (this.aliases.containsKey(key)) {
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