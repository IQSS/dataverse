package edu.harvard.iq.dataverse.settings.source;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enable using an old name for a new config name.
 * Usages will be logged and this source will ALWAYS stand back if the new name is used anywhere.
 */
public final class AliasConfigSource implements ConfigSource {
    
    private static final Logger logger = Logger.getLogger(AliasConfigSource.class.getName());
    private static final String ALIASES_PROP_FILE = "META-INF/microprofile-aliases.properties";
    
    private final ConcurrentHashMap<String, List<String>> aliases = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Pattern, List<String>> varArgAliases = new ConcurrentHashMap<>();
    
    public AliasConfigSource() {
        try {
            // read properties from class path file
            Properties aliasProps = readAliases(ALIASES_PROP_FILE);
            // store in our aliases map
            importAliases(aliasProps);
        } catch (IOException e) {
            // Usually it's an anti-pattern to catch the exception here, but skipping the file
            // should be fine here, as it's optional.
            logger.log(Level.INFO, "Could not read from "+ALIASES_PROP_FILE+". Skipping MPCONFIG alias setup.", e);
        }
    
        // Store all old names from JvmSettings
        importJvmSettings(JvmSettings.getAliasedSettings());
    }
    
    private void importJvmSettings(List<JvmSettings> aliasedSettings) {
        // First add all simple aliases not containing placeholders
        aliasedSettings.stream()
            .filter(s -> ! s.needsVarArgs())
            .forEach(setting -> aliases.put(setting.getScopedKey(), setting.getOldNames()));
        
        // Aliases with placeholders need to be compiled into a regex
        aliasedSettings.stream()
            .filter(JvmSettings::needsVarArgs)
            .forEach(setting -> varArgAliases.put(setting.getPatternizedKey(), setting.getOldNames()));
    }
    
    
    private Properties readAliases(String filePath) throws IOException {
        // get resource from classpath
        ClassLoader classLoader = this.getClass().getClassLoader();
        URL aliasesResource = classLoader.getResource(filePath);
        
        // Prevent errors if file not found or could not be loaded
        if (aliasesResource == null) {
            throw new IOException("Could not find or load, class loader returned null");
        }
    
        // load properties from file resource (parsing included)
        Properties aliasProps = new Properties();
        try (InputStream propStream = aliasesResource.openStream()) {
            aliasProps.load(propStream);
        }
        return aliasProps;
    }
    
    private void importAliases(Properties aliasProps) {
        aliasProps.forEach((key, value) -> aliases.put(key.toString(), List.of(value.toString())));
    }
    
    @Override
    public Map<String, String> getProperties() {
        // No, just no. We're not going to drop a list of stuff. We're only de-aliasing on calls to getValue()
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