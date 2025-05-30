package edu.harvard.iq.dataverse.settings;

import org.eclipse.microprofile.config.ConfigProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
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
 * Further future extensions of this enum class include
 * - adding predicates for validation and
 * - adding data manipulation for aliased config names.
 *
 * To create a setting, simply add it within a scope:
 * {@link JvmSettings#JvmSettings(JvmSettings, String)}
 *
 * Settings that might get renamed may provide their old names as aliases:
 * {@link JvmSettings#JvmSettings(JvmSettings, String, String...)}
 *
 * Some scopes or settings may need one or more placeholders, simply don't give
 * a key in these cases:
 * {@link JvmSettings#JvmSettings(JvmSettings)}
 *
 */
public enum JvmSettings {
    // the upmost root scope - every setting shall start with it.
    PREFIX("dataverse"),
    
    // GENERAL SETTINGS
    VERSION(PREFIX, "version"),
    BUILD(PREFIX, "build"),
    FQDN(PREFIX, "fqdn"),
    SITE_URL(PREFIX, "siteUrl"),
    
    // FILES SETTINGS
    SCOPE_FILES(PREFIX, "files"),
    FILES_DIRECTORY(SCOPE_FILES, "directory"),
    UPLOADS_DIRECTORY(SCOPE_FILES, "uploads"),
    DOCROOT_DIRECTORY(SCOPE_FILES, "docroot"),
    GUESTBOOK_AT_REQUEST(SCOPE_FILES, "guestbook-at-request"),
    GLOBUS_CACHE_MAXAGE(SCOPE_FILES, "globus-cache-maxage"),
    GLOBUS_TASK_MONITORING_SERVER(SCOPE_FILES, "globus-monitoring-server"),
    SCOPE_FEATURED_ITEMS(SCOPE_FILES, "featured-items"),
    FEATURED_ITEMS_IMAGE_MAXSIZE(SCOPE_FEATURED_ITEMS, "image-maxsize"),
    FEATURED_ITEMS_IMAGE_UPLOADS_DIRECTORY(SCOPE_FEATURED_ITEMS, "image-uploads"),
    HIDE_SCHEMA_DOT_ORG_DOWNLOAD_URLS(SCOPE_FILES, "hide-schema-dot-org-download-urls"),

    //STORAGE DRIVER SETTINGS
    SCOPE_DRIVER(SCOPE_FILES),
    DISABLE_S3_TAGGING(SCOPE_DRIVER, "disable-tagging"),
    
    // SOLR INDEX SETTINGS
    SCOPE_SOLR(PREFIX, "solr"),
    SOLR_HOST(SCOPE_SOLR, "host"),
    SOLR_PORT(SCOPE_SOLR, "port"),
    SOLR_PROT(SCOPE_SOLR, "protocol"),
    SOLR_CORE(SCOPE_SOLR, "core"),
    SOLR_PATH(SCOPE_SOLR, "path"),
    MIN_FILES_TO_USE_PROXY(SCOPE_SOLR, "min-files-to-use-proxy"),


    // INDEX CONCURENCY
    SCOPE_SOLR_CONCURENCY(SCOPE_SOLR, "concurrency"),
    MAX_ASYNC_INDEXES(SCOPE_SOLR_CONCURENCY, "max-async-indexes"),
    
    // RSERVE CONNECTION
    SCOPE_RSERVE(PREFIX, "rserve"),
    RSERVE_HOST(SCOPE_RSERVE, "host"),
    RSERVE_PORT(SCOPE_RSERVE, "port", "dataverse.ingest.rserve.port"),
    RSERVE_USER(SCOPE_RSERVE, "user"),
    RSERVE_PASSWORD(SCOPE_RSERVE, "password"),
    RSERVE_TEMPDIR(SCOPE_RSERVE, "tempdir"),
    
    // API SETTINGS
    SCOPE_API(PREFIX, "api"),
    API_SIGNING_SECRET(SCOPE_API, "signing-secret"),
    API_ALLOW_INCOMPLETE_METADATA(SCOPE_API, "allow-incomplete-metadata"),

    // SIGNPOSTING SETTINGS
    SCOPE_SIGNPOSTING(PREFIX, "signposting"),
    SIGNPOSTING_LEVEL1_AUTHOR_LIMIT(SCOPE_SIGNPOSTING, "level1-author-limit"),
    SIGNPOSTING_LEVEL1_ITEM_LIMIT(SCOPE_SIGNPOSTING, "level1-item-limit"),

    // FEATURE FLAGS SETTINGS
    SCOPE_FLAGS(PREFIX, "feature"),
    // This is a special placeholder-type setting entry, to be filled in by FeatureFlag entries during lookup.
    // Avoids adding flag entries twice.
    FEATURE_FLAG(SCOPE_FLAGS),
    
    // METADATA SETTINGS
    SCOPE_METADATA(PREFIX, "metadata"),
    MDB_SYSTEM_METADATA_KEYS(SCOPE_METADATA, "block-system-metadata-keys"),
    MDB_SYSTEM_KEY_FOR(MDB_SYSTEM_METADATA_KEYS),

    // PERSISTENT IDENTIFIER SETTINGS
    SCOPE_PID(PREFIX, "pid"),
    PID_PROVIDERS(SCOPE_PID, "providers"),
    PID_DEFAULT_PROVIDER(SCOPE_PID, "default-provider"),
    SCOPE_PID_PROVIDER(SCOPE_PID),
    PID_PROVIDER_TYPE(SCOPE_PID_PROVIDER, "type"),
    PID_PROVIDER_LABEL(SCOPE_PID_PROVIDER, "label"),
    PID_PROVIDER_AUTHORITY(SCOPE_PID_PROVIDER, "authority"),
    PID_PROVIDER_SHOULDER(SCOPE_PID_PROVIDER, "shoulder"),
    PID_PROVIDER_IDENTIFIER_GENERATION_STYLE(SCOPE_PID_PROVIDER, "identifier-generation-style"),
    PID_PROVIDER_DATAFILE_PID_FORMAT(SCOPE_PID_PROVIDER, "datafile-pid-format"),
    PID_PROVIDER_MANAGED_LIST(SCOPE_PID_PROVIDER, "managed-list"),
    PID_PROVIDER_EXCLUDED_LIST(SCOPE_PID_PROVIDER, "excluded-list"),

        
    // PROVIDER EZID - these settings were formerly kept together with DataCite ones
    SCOPE_PID_EZID(SCOPE_PID_PROVIDER, "ezid"),
    EZID_API_URL(SCOPE_PID_EZID, "api-url"),
    EZID_USERNAME(SCOPE_PID_EZID, "username"),
    EZID_PASSWORD(SCOPE_PID_EZID, "password"),
    
    // PROVIDER DATACITE
    SCOPE_PID_DATACITE(SCOPE_PID_PROVIDER, "datacite"),
    DATACITE_MDS_API_URL(SCOPE_PID_DATACITE, "mds-api-url"),
    DATACITE_REST_API_URL(SCOPE_PID_DATACITE, "rest-api-url"),
    DATACITE_USERNAME(SCOPE_PID_DATACITE, "username"),
    DATACITE_PASSWORD(SCOPE_PID_DATACITE, "password"),

    // PROVIDER CROSSREF
    SCOPE_PID_CROSSREF(SCOPE_PID_PROVIDER, "crossref"),
    CROSSREF_URL(SCOPE_PID_CROSSREF, "url"),
    CROSSREF_REST_API_URL(SCOPE_PID_CROSSREF, "rest-api-url"),
    CROSSREF_USERNAME(SCOPE_PID_CROSSREF, "username"),
    CROSSREF_PASSWORD(SCOPE_PID_CROSSREF, "password"),
    CROSSREF_DEPOSITOR(SCOPE_PID_CROSSREF, "depositor"),
    CROSSREF_DEPOSITOR_EMAIL(SCOPE_PID_CROSSREF, "depositor-email"),

    // PROVIDER PERMALINK
    SCOPE_PID_PERMALINK(SCOPE_PID_PROVIDER, "permalink"),
    PERMALINK_BASE_URL(SCOPE_PID_PERMALINK, "base-url"),
    PERMALINK_SEPARATOR(SCOPE_PID_PERMALINK, "separator"),
    
    // PROVIDER HANDLE
    SCOPE_PID_HANDLENET(SCOPE_PID_PROVIDER, "handlenet"),
    HANDLENET_INDEX(SCOPE_PID_HANDLENET, "index"),
    HANDLENET_INDEPENDENT_SERVICE(SCOPE_PID_HANDLENET, "independent-service"),
    HANDLENET_AUTH_HANDLE(SCOPE_PID_HANDLENET, "auth-handle"),
    SCOPE_PID_HANDLENET_KEY(SCOPE_PID_HANDLENET, "key"),
    HANDLENET_KEY_PATH(SCOPE_PID_HANDLENET_KEY, "path"),
    HANDLENET_KEY_PASSPHRASE(SCOPE_PID_HANDLENET_KEY, "passphrase"),

    /*
     * The deprecated legacy settings below are from when you could only have a
     * single PIDProvider. They mirror the settings above, but are global,not within
     * the SCOPE_PID_PROVIDER of an individual provider.
     */
    /**
     * DEPRECATED PROVIDER DATACITE
     * 
     * @deprecated - legacy single provider setting providing backward compatibility
     */
    @Deprecated(forRemoval = true, since = "2024-02-13")
    SCOPE_LEGACY_PID_DATACITE(SCOPE_PID, "datacite"),
    LEGACY_DATACITE_MDS_API_URL(SCOPE_LEGACY_PID_DATACITE, "mds-api-url", "doi.baseurlstring"),
    LEGACY_DATACITE_REST_API_URL(SCOPE_LEGACY_PID_DATACITE, "rest-api-url", "doi.dataciterestapiurlstring",
            "doi.mdcbaseurlstring"),
    LEGACY_DATACITE_USERNAME(SCOPE_LEGACY_PID_DATACITE, "username", "doi.username"),
    LEGACY_DATACITE_PASSWORD(SCOPE_LEGACY_PID_DATACITE, "password", "doi.password"),

    /**
     * DEPRECATED PROVIDER EZID
     * 
     * @deprecated - legacy single provider setting providing backward compatibility
     */
    @Deprecated(forRemoval = true, since = "2024-02-13")
    SCOPE_LEGACY_PID_EZID(SCOPE_PID, "ezid"), LEGACY_EZID_API_URL(SCOPE_LEGACY_PID_EZID, "api-url"),
    LEGACY_EZID_USERNAME(SCOPE_LEGACY_PID_EZID, "username"), LEGACY_EZID_PASSWORD(SCOPE_LEGACY_PID_EZID, "password"),

    /**
     * DEPRECATED PROVIDER PERMALINK
     * 
     * @deprecated - legacy single provider setting providing backward compatibility
     */
    @Deprecated(forRemoval = true, since = "2024-02-13")
    SCOPE_LEGACY_PID_PERMALINK(SCOPE_PID, "permalink"),
    LEGACY_PERMALINK_BASEURL(SCOPE_LEGACY_PID_PERMALINK, "base-url", "perma.baseurlstring"),

    /**
     * DEPRECATED PROVIDER HANDLE
     * 
     * @deprecated - legacy single provider setting providing backward compatibility
     */
    @Deprecated(forRemoval = true, since = "2024-02-13")
    SCOPE_LEGACY_PID_HANDLENET(SCOPE_PID, "handlenet"),
    LEGACY_HANDLENET_INDEX(SCOPE_LEGACY_PID_HANDLENET, "index", "dataverse.handlenet.index"),
    @Deprecated(forRemoval = true, since = "2024-02-13")
    SCOPE_LEGACY_PID_HANDLENET_KEY(SCOPE_LEGACY_PID_HANDLENET, "key"),
    LEGACY_HANDLENET_KEY_PATH(SCOPE_LEGACY_PID_HANDLENET_KEY, "path", "dataverse.handlenet.admcredfile"),
    LEGACY_HANDLENET_KEY_PASSPHRASE(SCOPE_LEGACY_PID_HANDLENET_KEY, "passphrase", "dataverse.handlenet.admprivphrase"),

    // SPI SETTINGS
    SCOPE_SPI(PREFIX, "spi"),
    SCOPE_EXPORTERS(SCOPE_SPI, "exporters"),
    EXPORTERS_DIRECTORY(SCOPE_EXPORTERS, "directory"),
    SCOPE_PIDPROVIDERS(SCOPE_SPI, "pidproviders"),
    PIDPROVIDERS_DIRECTORY(SCOPE_PIDPROVIDERS, "directory"),
    
    // MAIL SETTINGS
    SCOPE_MAIL(PREFIX, "mail"),
    SYSTEM_EMAIL(SCOPE_MAIL, "system-email"),
    SUPPORT_EMAIL(SCOPE_MAIL, "support-email"),
    CC_SUPPORT_ON_CONTACT_EMAIL(SCOPE_MAIL, "cc-support-on-contact-email"),
    MAIL_DEBUG(SCOPE_MAIL, "debug"),
    // Mail Transfer Agent settings
    SCOPE_MAIL_MTA(SCOPE_MAIL, "mta"),
    MAIL_MTA_AUTH(SCOPE_MAIL_MTA, "auth"),
    MAIL_MTA_USER(SCOPE_MAIL_MTA, "user"),
    MAIL_MTA_PASSWORD(SCOPE_MAIL_MTA, "password"),
    MAIL_MTA_SUPPORT_UTF8(SCOPE_MAIL_MTA, "allow-utf8-addresses"),
    // Placeholder setting for a large list of extra settings
    MAIL_MTA_SETTING(SCOPE_MAIL_MTA),
    
    // AUTH SETTINGS
    SCOPE_AUTH(PREFIX, "auth"),
    // AUTH: OIDC SETTINGS
    SCOPE_OIDC(SCOPE_AUTH, "oidc"),
    OIDC_ENABLED(SCOPE_OIDC, "enabled"),
    OIDC_TITLE(SCOPE_OIDC, "title"),
    OIDC_SUBTITLE(SCOPE_OIDC, "subtitle"),
    OIDC_AUTH_SERVER_URL(SCOPE_OIDC, "auth-server-url"),
    OIDC_CLIENT_ID(SCOPE_OIDC, "client-id"),
    OIDC_CLIENT_SECRET(SCOPE_OIDC, "client-secret"),
    SCOPE_OIDC_PKCE(SCOPE_OIDC, "pkce"),
    OIDC_PKCE_ENABLED(SCOPE_OIDC_PKCE, "enabled"),
    OIDC_PKCE_METHOD(SCOPE_OIDC_PKCE, "method"),
    OIDC_PKCE_CACHE_MAXSIZE(SCOPE_OIDC_PKCE, "max-cache-size"),
    OIDC_PKCE_CACHE_MAXAGE(SCOPE_OIDC_PKCE, "max-cache-age"),

    // UI SETTINGS
    SCOPE_UI(PREFIX, "ui"),
    UI_ALLOW_REVIEW_INCOMPLETE(SCOPE_UI, "allow-review-for-incomplete"),
    UI_SHOW_VALIDITY_FILTER(SCOPE_UI, "show-validity-filter"),
    UI_SHOW_VALIDITY_LABEL_WHEN_PUBLISHED(SCOPE_UI, "show-validity-label-when-published"),

    // NetCDF SETTINGS
    SCOPE_NETCDF(PREFIX, "netcdf"),
    GEO_EXTRACT_S3_DIRECT_UPLOAD(SCOPE_NETCDF, "geo-extract-s3-direct-upload"),

    // BAGIT SETTINGS
    SCOPE_BAGIT(PREFIX, "bagit"),
    SCOPE_BAGIT_SOURCEORG(SCOPE_BAGIT, "sourceorg"),
    BAGIT_SOURCE_ORG_NAME(SCOPE_BAGIT_SOURCEORG, "name"),
    BAGIT_SOURCEORG_ADDRESS(SCOPE_BAGIT_SOURCEORG, "address"),
    BAGIT_SOURCEORG_EMAIL(SCOPE_BAGIT_SOURCEORG, "email"),

    // STORAGE USE SETTINGS
    SCOPE_STORAGEUSE(PREFIX, "storageuse"),
    STORAGEUSE_DISABLE_UPDATES(SCOPE_STORAGEUSE, "disable-storageuse-increments"),
    
    //CSL CITATION SETTINGS
    SCOPE_CSL(PREFIX, "csl"),
    CSL_COMMON_STYLES(SCOPE_CSL, "common-styles"),
    
    // LOCALCONTEXTS
    SCOPE_LOCALCONTEXTS(PREFIX, "localcontexts"),
    LOCALCONTEXTS_URL(SCOPE_LOCALCONTEXTS, "url"),
    LOCALCONTEXTS_API_KEY(SCOPE_LOCALCONTEXTS, "api-key"),
    ;

    private static final String SCOPE_SEPARATOR = ".";
    public static final String PLACEHOLDER_KEY = "%s";
    private static final Pattern OLD_NAME_PLACEHOLDER_PATTERN = Pattern.compile("%(\\d\\$)?s");
    
    private final String key;
    private final String scopedKey;
    private final JvmSettings parent;
    private final List<String> oldNames;
    private final int placeholders;
    
    /**
     * Create a root scope.
     * @param key The scopes name.
     */
    JvmSettings(String key) {
        this.key = key;
        this.scopedKey = key;
        this.parent = null;
        this.oldNames = List.of();
        this.placeholders = 0;
    }
    
    /**
     * Create a scope or setting with a placeholder for a variable argument in it.
     * Used to create "configurable objects" with certain attributes using dynamic, programmatic lookup.
     *
     * Any placeholder present in a settings full scoped key will be replaced when looked up
     * via {@link #lookup(Class, String...)}.
     *
     * @param scope The parent scope.
     */
    JvmSettings(JvmSettings scope) {
        this.key = PLACEHOLDER_KEY;
        this.scopedKey = scope.scopedKey + SCOPE_SEPARATOR + this.key;
        this.parent = scope;
        this.oldNames = List.of();
        this.placeholders = scope.placeholders + 1;
    }
    
    /**
     * Create a scope or setting with name it and associate with a parent scope.
     * @param scope The parent scope.
     * @param key The name of this scope or setting.
     */
    JvmSettings(JvmSettings scope, String key) {
        this.key = key;
        this.scopedKey = scope.scopedKey + SCOPE_SEPARATOR + key;
        this.parent = scope;
        this.oldNames = List.of();
        this.placeholders = scope.placeholders;
    }
    
    /**
     * Create a setting with name it and associate with a parent scope.
     * (Could also be a scope, but old names for scopes aren't the way this is designed.)
     *
     * When old names are given, these need to be given as fully scoped setting names! (Otherwise
     * it would not be possible to switch between completely different scopes.)
     *
     * @param scope The parent scope of this setting.
     * @param key The name of the setting.
     * @param oldNames Any previous names this setting was known as.
     *                 Must be given as fully scopes names, not just the old unscoped key/name.
     *                 Used by {@link edu.harvard.iq.dataverse.settings.source.AliasConfigSource} to allow backward
     *                 compatible, non-breaking deprecation and switching to new setting names.
     */
    JvmSettings(JvmSettings scope, String key, String... oldNames) {
        this.key = key;
        this.scopedKey = scope.scopedKey + SCOPE_SEPARATOR + key;
        this.parent = scope;
        this.oldNames = Arrays.stream(oldNames).collect(Collectors.toUnmodifiableList());
        this.placeholders = scope.placeholders;
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
     * This method should only be used by {@link edu.harvard.iq.dataverse.settings.source.AliasConfigSource}.
     * In case of a setting containing placeholder(s), it will check any old names given in the definition
     * for presence of at least one placeholder plus it doesn't use more placeholders than available.
     * (Old names containing placeholders for settings without any are checked, too.)
     *
     * Violations will result in a {@link IllegalArgumentException} and will be noticed during any test execution.
     * A developer must fix the old name definition before shipping the code.
     *
     * @return List of old names, may be empty, but never null.
     * @throws IllegalArgumentException When an old name has no or too many placeholders for this setting.
     */
    public List<String> getOldNames() {
        if (needsVarArgs()) {
            for (String name : oldNames) {
                long matches = OLD_NAME_PLACEHOLDER_PATTERN.matcher(name).results().count();
                
                if (matches == 0) {
                    throw new IllegalArgumentException("JvmSettings." + this.name() + "'s old name '" +
                        name + "' needs at least one placeholder");
                } else if (matches > this.placeholders) {
                    throw new IllegalArgumentException("JvmSettings." + this.name() + "'s old name '" +
                        name + "' has more placeholders than the current name");
                }
            }
        } else if (! this.oldNames.stream().noneMatch(OLD_NAME_PLACEHOLDER_PATTERN.asPredicate())) {
            throw new IllegalArgumentException("JvmSettings." + this.name() + " has no placeholder but old name requires it");
        }
        
        return oldNames;
    }
    
    /**
     * Retrieve the scoped key for this setting. Scopes are separated by dots.
     * If the setting contains placeholders, these will be represented as {@link #PLACEHOLDER_KEY}.
     *
     * @return The scoped key (or the key if no scope). Example: dataverse.subscope.subsubscope.key
     */
    public String getScopedKey() {
        return this.scopedKey;
    }
    
    public Pattern getPatternizedKey() {
        return Pattern.compile(
            getScopedKey()
                .replace(SCOPE_SEPARATOR, "\\.")
                .replace(PLACEHOLDER_KEY, "(.+?)"));
    }
    
    
    /**
     * Does this setting carry and placeholders for variable arguments?
     * @return True if so, False otherwise.
     */
    public boolean needsVarArgs() {
        return this.placeholders > 0;
    }
    
    /**
     * Return the number of placeholders / variable arguments are necessary to lookup this setting.
     * An exact match in the number of arguments will be necessary for a successful lookup.
     * @return Number of placeholders for this scoped setting.
     */
    public int numberOfVarArgs() {
        return placeholders;
    }
    
    /**
     * Lookup this setting via MicroProfile Config as a required option (it will fail if not present).
     * @throws java.util.NoSuchElementException - if the property is not defined or is defined as an empty string
     * @return The setting as a String
     */
    public String lookup() {
        return lookup(String.class);
    }
    
    /**
     * Lookup this setting via MicroProfile Config as an optional setting.
     * @return The setting as String wrapped in a (potentially empty) Optional
     */
    public Optional<String> lookupOptional() {
        return lookupOptional(String.class);
    }
    
    /**
     * Lookup this setting via MicroProfile Config as a required option (it will fail if not present).
     *
     * @param klass The target type class to convert the setting to if found and not null
     * @return The setting as an instance of {@link T}
     * @param <T> Target type to convert the setting to (you can create custom converters)
     *
     * @throws java.util.NoSuchElementException When the property is not defined or is defined as an empty string.
     * @throws IllegalArgumentException When the settings value could not be converted to target type.
     */
    public <T> T lookup(Class<T> klass) {
        if (needsVarArgs()) {
            throw new IllegalArgumentException("Cannot lookup a setting containing placeholders with this method.");
        }
        
        // This must be done with the full-fledged lookup, as we cannot store the config in an instance or static
        // variable, as the alias config source depends on this enum (circular dependency). This is easiest
        // avoided by looking up the static cached config at the cost of a method invocation.
        return ConfigProvider.getConfig().getValue(this.getScopedKey(), klass);
    }
    
    /**
     * Lookup this setting via MicroProfile Config as an optional setting.
     *
     * @param klass The target type class to convert the setting to if found and not null
     * @param <T> Target type to convert the setting to (you can create custom converters)
     * @return The setting as an instance of {@link Optional<T>} or an empty Optional
     *
     * @throws IllegalArgumentException When the settings value could not be converted to target type.
     */
    public <T> Optional<T> lookupOptional(Class<T> klass) {
        if (needsVarArgs()) {
            throw new IllegalArgumentException("Cannot lookup a setting containing variable arguments with this method.");
        }
        
        // This must be done with the full-fledged lookup, as we cannot store the config in an instance or static
        // variable, as the alias config source depends on this enum (circular dependency). This is easiest
        // avoided by looking up the static cached config at the cost of a method invocation.
        return ConfigProvider.getConfig().getOptionalValue(this.getScopedKey(), klass);
    }
    
    /**
     * Lookup a required setting containing placeholders for arguments like a name and return as plain String.
     * To use type conversion, use {@link #lookup(Class, String...)}.
     *
     * @param arguments The var args to replace the placeholders of this setting.
     * @return The value of the setting.
     *
     * @throws java.util.NoSuchElementException When the setting has not been set in any MPCONFIG source or is an empty string.
     * @throws IllegalArgumentException When using it on a setting without placeholders.
     * @throws IllegalArgumentException When not providing as many arguments as there are placeholders.
     */
    public String lookup(String... arguments) {
        return lookup(String.class, arguments);
    }
    
    /**
     * Lookup an optional setting containing placeholders for arguments like a name and return as plain String.
     * To use type conversion, use {@link #lookupOptional(Class, String...)}.
     *
     * @param arguments The var args to replace the placeholders of this setting.
     * @return The value as an instance of {@link Optional<String>} or an empty Optional
     *
     * @throws IllegalArgumentException When using it on a setting without placeholders.
     * @throws IllegalArgumentException When not providing as many arguments as there are placeholders.
     */
    public Optional<String> lookupOptional(String... arguments) {
        return lookupOptional(String.class, arguments);
    }
    
    /**
     * Lookup a required setting containing placeholders for arguments like a name and return as converted type.
     * To avoid type conversion, use {@link #lookup(String...)}.
     *
     * @param klass The target type class.
     * @param arguments The var args to replace the placeholders of this setting.
     * @param <T> Target type to convert the setting to (you can create custom converters)
     * @return The value of the setting, converted to the given type.
     *
     * @throws java.util.NoSuchElementException When the setting has not been set in any MPCONFIG source or is an empty string.
     * @throws IllegalArgumentException When using it on a setting without placeholders.
     * @throws IllegalArgumentException When not providing as many arguments as there are placeholders.
     * @throws IllegalArgumentException When the settings value could not be converted to the target type.
     */
    public <T> T lookup(Class<T> klass, String... arguments) {
        if (needsVarArgs()) {
            if (arguments == null || arguments.length != placeholders) {
                throw new IllegalArgumentException("You must specify " + placeholders + " placeholder lookup arguments.");
            }
            return ConfigProvider.getConfig().getValue(this.insert(arguments), klass);
        }
        throw new IllegalArgumentException("Cannot lookup a setting without variable arguments with this method.");
    }
    
    /**
     * Lookup an optional setting containing placeholders for arguments like a name and return as converted type.
     * To avoid type conversion, use {@link #lookupOptional(String...)}.
     *
     * @param klass The target type class.
     * @param arguments The var args to replace the placeholders of this setting.
     * @param <T> Target type to convert the setting to (you can create custom converters)
     * @return The value as an instance of {@link Optional<T>} or an empty Optional
     *
     * @throws IllegalArgumentException When using it on a setting without placeholders.
     * @throws IllegalArgumentException When not providing as many arguments as there are placeholders.
     * @throws IllegalArgumentException When the settings value could not be converted to the target type.
     */
    public <T> Optional<T> lookupOptional(Class<T> klass, String... arguments) {
        if (needsVarArgs()) {
            if (arguments == null || arguments.length != placeholders) {
                throw new IllegalArgumentException("You must specify " + placeholders + " placeholder lookup arguments.");
            }
            return ConfigProvider.getConfig().getOptionalValue(this.insert(arguments), klass);
        }
        throw new IllegalArgumentException("Cannot lookup a setting without variable arguments with this method.");
    }
    
    /**
     * Inject arguments into the placeholders of this setting. Will not do anything when no placeholders present.
     *
     * @param arguments The variable arguments to be inserted for the placeholders.
     * @return The formatted setting name.
     */
    public String insert(String... arguments) {
        return String.format(this.getScopedKey(), (Object[]) arguments);
    }
    
}
