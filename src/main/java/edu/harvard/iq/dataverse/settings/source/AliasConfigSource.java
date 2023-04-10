package edu.harvard.iq.dataverse.settings.source;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    
    // Has visibility "package" to be usable from test class!
    void addAlias(String newName, List<String> oldNames) {
        this.aliases.put(newName, oldNames);
    }
    
    // Has visibility "package" to be usable from test class!
    void addAlias(Pattern newNamePattern, List<String> oldNamePatterns) {
        this.varArgAliases.put(newNamePattern, oldNamePatterns);
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
        // Any other config source can override us, except the microprofile-config.properties source (ordinal = 100)
        // We use *the same* ordinal (which is also the default ordinal). As our source is named "Alias",
        // it will be served first as "A" = 65 comes before other letters (uses String::compareTo).
        return ConfigSource.DEFAULT_ORDINAL;
    }
    
    @Override
    public String getValue(String key) {
        
        // If the key is null or not starting with the prefix ("dataverse"), we are not going to jump through loops,
        // avoiding computation overhead
        if (key == null || ! key.startsWith(JvmSettings.PREFIX.getScopedKey())) {
            return null;
        }
        
        List<String> oldNames = new ArrayList<>();
        
        // Retrieve simple cases without placeholders
        if (this.aliases.containsKey(key)) {
            oldNames.addAll(this.aliases.get(key));
        // Or try with regex patterns
        } else {
            // Seek for the given key within all the patterns for placeholder containing settings,
            // returning a Matcher to extract the variable arguments as regex match groups.
            Optional<Matcher> foundMatcher = varArgAliases.keySet().stream()
                .map(pattern -> pattern.matcher(key))
                .filter(Matcher::matches)
                .findFirst();
            
            // Extract the matched groups and construct all old setting names with them
            if (foundMatcher.isPresent()) {
                Matcher matcher = foundMatcher.get();
                
                List<String> varArgs = new ArrayList<>();
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    varArgs.add(matcher.group(i));
                }
                Object[] args = varArgs.toArray();
                
                this.varArgAliases
                    .get(matcher.pattern())
                    .forEach(oldNamePattern -> oldNames.add(String.format(oldNamePattern, args)));
            }
        }
    
        // Return the first non-empty result
        // NOTE: When there are multiple old names in use, they would conflict anyway. Upon deletion of one of the
        //       old settings the other becomes visible and triggers the warning again. There might even be different
        //       old settings in different sources, which might conflict, too (see ordinal value).
        // NOTE: As the default is an empty oldNames array, loop will only be executed if anything was found before.
        for (String oldName : oldNames) {
            Optional<String> value = ConfigProvider.getConfig().getOptionalValue(oldName, String.class);
        
            if (value.isPresent()) {
                logger.log(
                    Level.WARNING,
                    "Detected deprecated config option {0} in use. Please update your config to use {1}.",
                    new String[]{oldName, key}
                );
                return value.get();
            }
        }
        
        // Sane default: nothing found.
        return null;
    }
    
    @Override
    public String getName() {
        return "Alias";
    }
}