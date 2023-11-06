package edu.harvard.iq.dataverse.settings;

public interface SettingsSource<S extends SettingKey> {
    <T> T lookup(S settingKey, Class<T> klass, T defaultValue);
}
