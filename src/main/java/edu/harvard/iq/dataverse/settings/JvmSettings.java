package edu.harvard.iq.dataverse.settings;

import org.eclipse.microprofile.config.ConfigProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Enum to store each and every JVM-based setting as a reference,
 * much like the enum {@link SettingsServiceBean.Key} for DB settings.
 *
 * To be able to have more control over JVM settings names,
 * avoid typos, maybe create lists of settings and so on,
 * this enum will provide the place to add any old and new
 * settings that are destined to be made at the JVM level.
 *
 * Further extensions of this class include
 * - adding predicates for validation and
 * - offering injecting parameters into keys (as used with the file access subsystem) and
 * - adding data manipulation for aliased config names.
 */
public enum JvmSettings {
    // the upmost root scope - every setting shall start with it.
    PREFIX("dataverse"),
    
    // GENERAL SETTINGS
    VERSION(PREFIX, "version"),
    BUILD(PREFIX, "build"),
    
    // FILES SETTINGS
    SCOPE_FILES(PREFIX, "files"),
    FILES_DIRECTORY(SCOPE_FILES, "directory"),
    
    // SOLR INDEX SETTINGS
    SCOPE_SOLR(PREFIX, "solr"),
    SOLR_HOST(SCOPE_SOLR, "host"),
    SOLR_PORT(SCOPE_SOLR, "port"),
    SOLR_PROT(SCOPE_SOLR, "protocol"),
    SOLR_CORE(SCOPE_SOLR, "core"),
    SOLR_PATH(SCOPE_SOLR, "path"),
    
    ;
    
    private static final String SCOPE_SEPARATOR = ".";
    
    private final String key;
    private final String scopedKey;
    private final JvmSettings parent;
    private final List<String> oldNames;
    
    JvmSettings(String key) {
        this.key = key;
        this.scopedKey = key;
        this.parent = null;
        this.oldNames = List.of();
    }
    
    JvmSettings(JvmSettings scope, String key) {
        this.key = key;
        this.scopedKey = scope.scopedKey + SCOPE_SEPARATOR + key;
        this.parent = scope;
        this.oldNames = List.of();
    }
    
    JvmSettings(JvmSettings scope, String key, String... oldNames) {
        this.key = key;
        this.scopedKey = scope.scopedKey + SCOPE_SEPARATOR + key;
        this.parent = scope;
        this.oldNames = Arrays.stream(oldNames).collect(Collectors.toUnmodifiableList());
    }
    
    private static final List<JvmSettings> aliased = new ArrayList<>();
    static {
        for (JvmSettings setting : JvmSettings.values()) {
            if (!setting.oldNames.isEmpty()) {
                aliased.add(setting);
            }
        }
    }
    
    /**
     * Get all settings having old names to include them in {@link edu.harvard.iq.dataverse.settings.source.AliasConfigSource}
     * @return List of settings with old alias names. Can be empty, but will not be null.
     */
    public static List<JvmSettings> getAliasedSettings() {
        return Collections.unmodifiableList(aliased);
    }
    
    /**
     * Return a list of old names to be used as aliases for backward compatibility.
     * Will return empty list if no old names present.
     *
     * @return List of old names, may be empty, but never null.
     */
    public List<String> getOldNames() {
        return oldNames;
    }
    
    /**
     * Retrieve the scoped key for this setting. Scopes are separated by dots.
     *
     * @return The scoped key (or the key if no scope). Example: dataverse.subscope.subsubscope.key
     */
    public String getScopedKey() {
        return this.scopedKey;
    }
    
    /**
     * Lookup this setting via MicroProfile Config as a required option (it will fail if not present).
     * @throws java.util.NoSuchElementException - if the property is not defined or is defined as an empty string
     * @return The setting as a String
     */
    public String lookup() {
        // This must be done with the full-fledged lookup, as we cannot store the config in an instance or static
        // variable, as the alias config source depends on this enum (circular dependency). This is easiest
        // avoided by looking up the static cached config at the cost of a method invocation.
        return ConfigProvider.getConfig().getValue(this.getScopedKey(), String.class);
    }
    
    /**
     * Lookup this setting via MicroProfile Config as an optional setting.
     * @return The setting as String wrapped in a (potentially empty) Optional
     */
    public Optional<String> lookupOptional() {
        // This must be done with the full-fledged lookup, as we cannot store the config in an instance or static
        // variable, as the alias config source depends on this enum (circular dependency). This is easiest
        // avoided by looking up the static cached config at the cost of a method invocation.
        return ConfigProvider.getConfig().getOptionalValue(this.getScopedKey(), String.class);
    }
}
