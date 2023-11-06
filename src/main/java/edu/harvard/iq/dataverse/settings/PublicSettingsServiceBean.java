package edu.harvard.iq.dataverse.settings;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.HashMap;
import java.util.Map;


@Stateless
@Named
public class PublicSettingsServiceBean {

    @Inject
    DatabaseSettingsSource databaseSettingsSource;

    @Inject
    JvmSettingsSource jvmSettingsSource;

    public static Map<PublicSettings, SettingKey> SETTING_SOURCE_KEYS = new HashMap<>();
    public static Map<PublicSettings, SettingsSource<?>> SETTING_SOURCES = new HashMap<>();

    static {
        SETTING_SOURCE_KEYS.put(PublicSettings.FQDN, JvmSettings.FQDN);
    }

    {
        SETTING_SOURCES.put(PublicSettings.FQDN, jvmSettingsSource);
    }

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

    public enum PublicSettings {
        FQDN(SettingGroup.DATAVERSE, "fqdn", String.class);

        private final SettingGroup settingGroup;
        private final String settingName;
        private final Class<?> returnType;
        private final Object defaultValue;

        PublicSettings(SettingGroup settingGroup, String settingName, Class<?> returnType) {
            this.settingGroup = settingGroup;
            this.settingName = settingName;
            this.returnType = returnType;
            this.defaultValue = null;
        }

        PublicSettings(SettingGroup settingGroup, String settingName, Object defaultValue, Class<?> returnType) {
            this.settingGroup = settingGroup;
            this.settingName = settingName;
            this.returnType = returnType;
            this.defaultValue = defaultValue;
        }

        public SettingGroup getSettingGroup() {
            return settingGroup;
        }

        public String getSettingName() {
            return settingName;
        }

        public Class<?> getReturnType() {
            return returnType;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }
    }

    public Object lookup(String settingGroupName, String settingName) {
        SettingGroup settingGroup;
        try {
            settingGroup = SettingGroup.valueOf(settingGroupName);
        } catch (IllegalArgumentException e) {
            return null;
        }
        PublicSettings publicSetting = getPublicSettingByValues(settingGroup, settingName);
        if (publicSetting != null) {
            return lookup(publicSetting);
        }
        return null;
    }

    private PublicSettings getPublicSettingByValues(SettingGroup settingGroup, String settingName) {
        for (PublicSettings publicSetting : PublicSettings.values()) {
            if (publicSetting.getSettingGroup().equals(settingGroup) && publicSetting.getSettingName().equals(settingName)) {
                return publicSetting;
            }
        }
        return null;
    }

    private Object lookup(PublicSettings publicSetting) {
        SettingsSource settingsSource = SETTING_SOURCES.get(publicSetting);
        return settingsSource.lookup(SETTING_SOURCE_KEYS.get(publicSetting), publicSetting.getReturnType(), publicSetting.getDefaultValue());
    }
}
