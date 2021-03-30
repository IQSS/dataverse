package edu.harvard.iq.dataverse.settings;

import java.util.Optional;

/**
 * This enum class contains scopes for configuration values, allowing for
 * an aligned use of prefixes and naming conventions. Scopes enhance readability
 * and self-documentation of settings.
 *
 * This datastructer is kinda tree-like, as it supports the use of parent
 * scopes in a flat hierarchy. Currently it does not implement retrieving
 * the scopes as a tree structure, which might be done in the future.
 */
public enum ConfigScope {
    
    // TOP SCOPES
    API("api"),
    AUTH("auth"),
    DOWNLOAD("download"),
    EXPORT("export"),
    HARVEST("harvest"),
    INDEX("index"),
    INGEST("ingest"),
    MAIL("mail"),
    METRICS("metrics"),
    PID("pid"),
    UI("ui"),
    UNSCOPED("unscoped"),
    UPLOAD("upload"),
    
    // SUB SCOPES
    API_ACCESS(API, "access"),
    
    AUTH_BUILTIN(AUTH, "builtin"),
    AUTH_BUILTIN_API(AUTH_BUILTIN, "api"),
    AUTH_BUILTIN_PV(AUTH_BUILTIN, "pv"),
    AUTH_OAUTH(AUTH, "oauth"),
    AUTH_SHIB(AUTH, "shib"),
    
    EXPORT_BAGIT(EXPORT, "bagit"),
    
    HARVEST_IMPORT(HARVEST, "import"),
    
    INDEX_FULLTEXT(INDEX, "fulltext"),
    
    MAIL_SYSTEM(MAIL, "system"),
    
    PID_FILES(PID, "files"),
    PID_HDL(PID, "hdl"),
    
    UI_DATASET(UI, "dataset"),
    UI_FOOTER(UI, "footer"),
    UI_HEADER(UI, "header"),
    
    UPLOAD_LIMITS(UPLOAD, "limits")
    ;
    
    private final String prefix = "dataverse";
    private final String scope;
    private final Optional<ConfigScope> parentScope;
    
    ConfigScope(String scope) {
        this.scope = scope;
        this.parentScope = Optional.empty();
    }
    ConfigScope(ConfigScope parentScope, String scope) {
        this.scope = scope;
        this.parentScope = Optional.ofNullable(parentScope);
    }
    
    // ACTIONS
    
    /**
     * Generate the full qualified scope name via recursion.
     * @return FQSN
     */
    public String toScope() {
        return parentScope.map(ConfigScope::toScope).orElse(prefix) + "." + scope;
    }
    
    /**
     * Create a fully scoped key from a key name and the scope given.
     * @param key The key name to use
     * @return The scoped key
     */
    public String getScopedKey(String key) { return toScope()+"."+key; }
    
    // GETTERS
    public String getPrefix() {
        return prefix;
    }
    public String getScope() {
        return scope;
    }
    public Optional<ConfigScope> getParentScope() {
        return parentScope;
    }
    
    @Override
    public String toString() {
        return "MpConfig{" +
            "prefix='" + prefix + '\'' +
            ", scope='" + scope + '\'' +
            ", parentScope=" + parentScope +
            '}';
    }
}
