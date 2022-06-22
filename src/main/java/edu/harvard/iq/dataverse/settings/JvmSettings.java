package edu.harvard.iq.dataverse.settings;

import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Optional;

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
 * - adding aliases when renaming settings,
 * - adding predicates for validation and
 * - offering injecting parameters into keys (as used with the file access subsystem).
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
    
    ;
    
    private final String key;
    private final JvmSettings parent;
    
    JvmSettings(String key) {
        this.key = key;
        this.parent = null;
    }
    
    JvmSettings(JvmSettings scope, String key) {
        this.key = key;
        this.parent = scope;
    }
    
    /**
     * Retrieve the scoped key for this setting. Scopes are separated by dots.
     *
     * @return The scoped key (or the key if no scope). Example: dataverse.subscope.subsubscope.key
     */
    public String getScopedKey() {
        if (this.parent != null) {
            return parent.getScopedKey() + "." + this.key;
        } else {
            return this.key;
        }
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
