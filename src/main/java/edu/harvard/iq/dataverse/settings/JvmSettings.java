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
