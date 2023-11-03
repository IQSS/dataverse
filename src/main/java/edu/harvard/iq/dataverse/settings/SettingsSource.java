package edu.harvard.iq.dataverse.settings;

import java.util.Optional;

public interface SettingsSource {

    /**
     * Lookup this setting (it will fail if not present).
     *
     * @param settingKey The setting string key in the setting source
     * @return The setting as a String
     * @throws java.util.NoSuchElementException - if the property is not defined or is defined as an empty string
     */
    String lookup(String settingKey);

    /**
     * Lookup this setting as an optional setting.
     *
     * @param settingKey The setting string key in the setting source
     * @return The setting as String wrapped in a (potentially empty) Optional
     */
    Optional<String> lookupOptional(String settingKey);

    /**
     * Lookup this setting (it will fail if not present).
     *
     * @param settingKey The setting string key in the setting source
     * @param klass      The target type class to convert the setting to if found and not null
     * @param <T>        Target type to convert the setting to (you can create custom converters)
     * @return The setting as an instance of {@link T}
     * @throws java.util.NoSuchElementException When the property is not defined or is defined as an empty string.
     * @throws IllegalArgumentException         When the settings value could not be converted to target type.
     */
    <T> T lookup(String settingKey, Class<T> klass);

    /**
     * Lookup this setting vas an optional setting.
     *
     * @param settingKey The setting string key in the setting source
     * @param klass      The target type class to convert the setting to if found and not null
     * @param <T>        Target type to convert the setting to (you can create custom converters)
     * @return The setting as an instance of {@link Optional<T>} or an empty Optional
     * @throws IllegalArgumentException When the settings value could not be converted to target type.
     */
    <T> Optional<T> lookupOptional(String settingKey, Class<T> klass);

    /**
     * Lookup a required setting containing placeholders for arguments like a name and return as plain String.
     * To use type conversion, use {@link #lookup(String, Class, String...)}.
     *
     * @param settingKey The setting string key in the setting source
     * @param arguments  The var args to replace the placeholders of this setting.
     * @return The value of the setting.
     * @throws java.util.NoSuchElementException When the setting has not been set in any source or is an empty string.
     * @throws IllegalArgumentException         When using it on a setting without placeholders.
     * @throws IllegalArgumentException         When not providing as many arguments as there are placeholders.
     */
    String lookup(String settingKey, String... arguments);

    /**
     * Lookup an optional setting containing placeholders for arguments like a name and return as plain String.
     * To use type conversion, use {@link #lookupOptional(String, Class, String...)}.
     *
     * @param settingKey The setting string key in the setting source
     * @param arguments  The var args to replace the placeholders of this setting.
     * @return The value as an instance of {@link Optional<String>} or an empty Optional
     * @throws IllegalArgumentException When using it on a setting without placeholders.
     * @throws IllegalArgumentException When not providing as many arguments as there are placeholders.
     */
    Optional<String> lookupOptional(String settingKey, String... arguments);

    /**
     * Lookup a required setting containing placeholders for arguments like a name and return as converted type.
     * To avoid type conversion, use {@link #lookup}.
     *
     * @param settingKey The setting string key in the setting source
     * @param klass      The target type class.
     * @param arguments  The var args to replace the placeholders of this setting.
     * @param <T>        Target type to convert the setting to (you can create custom converters)
     * @return The value of the setting, converted to the given type.
     * @throws java.util.NoSuchElementException When the setting has not been set in any source or is an empty string.
     * @throws IllegalArgumentException         When using it on a setting without placeholders.
     * @throws IllegalArgumentException         When not providing as many arguments as there are placeholders.
     * @throws IllegalArgumentException         When the settings value could not be converted to the target type.
     */
    <T> T lookup(String settingKey, Class<T> klass, String... arguments);

    /**
     * Lookup an optional setting containing placeholders for arguments like a name and return as converted type.
     * To avoid type conversion, use {@link #lookupOptional(String, String...)}.
     *
     * @param settingKey The setting string key in the setting source
     * @param klass      The target type class.
     * @param arguments  The var args to replace the placeholders of this setting.
     * @param <T>        Target type to convert the setting to (you can create custom converters)
     * @return The value as an instance of {@link Optional<T>} or an empty Optional
     * @throws IllegalArgumentException When using it on a setting without placeholders.
     * @throws IllegalArgumentException When not providing as many arguments as there are placeholders.
     * @throws IllegalArgumentException When the settings value could not be converted to the target type.
     */
    <T> Optional<T> lookupOptional(String settingKey, Class<T> klass, String... arguments);
}
