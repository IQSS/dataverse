package edu.harvard.iq.dataverse.settings;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Stateless
@Named
public class PublicSettingsServiceBean implements SettingsSource {
    public static Map<PublicSettings, SettingKey> SETTING_SOURCE_KEYS = new HashMap<>();
    public static Map<PublicSettings, SettingsSource> SETTING_SOURCES = new HashMap<>();

    @Inject
    DatabaseSettingsSource databaseSettingsSource;

    @Inject
    JvmSettingsSource jvmSettingsSource;

    static {
        SETTING_SOURCE_KEYS.put(PublicSettings.FQDN, JvmSettings.FQDN);
    }

    {
        SETTING_SOURCES.put(PublicSettings.FQDN, jvmSettingsSource);
    }

    public enum PublicSettings {
        FQDN(SettingGroup.DATAVERSE, "fqdn");

        public enum SettingGroup {
            DATAVERSE("dataverse");

            SettingGroup parentGroup;
            final String groupName;

            SettingGroup(SettingGroup parentGroup, String groupName) {
                this.parentGroup = parentGroup;
                this.groupName = groupName;
            }

            SettingGroup(String groupName) {
                this.groupName = groupName;
            }

            public SettingGroup getParentGroup() {
                return parentGroup;
            }

            public String getGroupName() {
                return groupName;
            }
        }

        final SettingGroup settingGroup;
        final String settingName;

        PublicSettings(SettingGroup settingGroup, String settingName) {
            this.settingGroup = settingGroup;
            this.settingName = settingName;
        }
    }

    @Override
    public String lookup(String settingKey) {
        return null;
    }

    @Override
    public Optional<String> lookupOptional(String settingKey) {
        return Optional.empty();
    }

    @Override
    public <T> T lookup(String settingKey, Class<T> klass) {
        return null;
    }

    @Override
    public <T> Optional<T> lookupOptional(String settingKey, Class<T> klass) {
        return Optional.empty();
    }

    @Override
    public String lookup(String settingKey, String... arguments) {
        return null;
    }

    @Override
    public Optional<String> lookupOptional(String settingKey, String... arguments) {
        return Optional.empty();
    }

    @Override
    public <T> T lookup(String settingKey, Class<T> klass, String... arguments) {
        return null;
    }

    @Override
    public <T> Optional<T> lookupOptional(String settingKey, Class<T> klass, String... arguments) {
        return Optional.empty();
    }
}
