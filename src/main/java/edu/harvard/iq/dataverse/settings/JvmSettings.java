package edu.harvard.iq.dataverse.settings;

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
    
    // SOLR INDEX SETTINGS
    SCOPE_SOLR(PREFIX, "solr"),
    SOLR_HOST(SCOPE_SOLR, "host"),
    SOLR_PORT(SCOPE_SOLR, "port"),
    SOLR_PROT(SCOPE_SOLR, "protocol"),
    SOLR_CORE(SCOPE_SOLR, "core"),
    SOLR_PATH(SCOPE_SOLR, "path"),
    
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
}
