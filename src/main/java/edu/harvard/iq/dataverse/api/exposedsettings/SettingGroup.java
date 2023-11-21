package edu.harvard.iq.dataverse.api.exposedsettings;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;

import java.util.Arrays;
import java.util.List;

import static edu.harvard.iq.dataverse.api.exposedsettings.Setting.*;

public class SettingGroup extends SettingItem {
    public static final String GROUP_NAME_DATAVERSE = "dataverse";
    public static final String GROUP_NAME_DATASET = "dataset";
    public static final String GROUP_NAME_DATAFILE = "datafile";
    public static final String GROUP_NAME_API = "api";
    private final List<SettingItem> itemList;
    private static SettingGroup dataverseSettingGroup;

    private SettingGroup(String name, List<SettingItem> itemList) {
        super(name);
        this.itemList = itemList;
    }

    public static SettingGroup getDataverseSettingGroup(SystemConfig systemConfig, SettingsServiceBean settingsService) {
        if (dataverseSettingGroup == null) {
            dataverseSettingGroup = new SettingGroup(
                    GROUP_NAME_DATAVERSE,
                    List.of(
                            new Setting<>(SETTING_NAME_FQDN, JvmSettings.FQDN.lookup()),
                            new Setting<>(SETTING_NAME_IS_PUBLIC_INSTALL, systemConfig.isPublicInstall()),
                            new SettingGroup(GROUP_NAME_API, List.of(
                                    new Setting<>(SETTING_NAME_API_TERMS_OF_USE, systemConfig.getApiTermsOfUse()),
                                    new Setting<>(SETTING_NAME_API_ALLOW_INCOMPLETE_METADATA, JvmSettings.API_ALLOW_INCOMPLETE_METADATA.lookupOptional(Boolean.class).orElse(false))
                            )
                            ),
                            new SettingGroup(GROUP_NAME_DATASET, List.of(
                                    new Setting<>(SETTING_NAME_DATASET_PUBLISH_POPUP_CUSTOM_TEXT, settingsService.getValueForKey(SettingsServiceBean.Key.DatasetPublishPopupCustomText)),
                                    new Setting<>(SETTING_NAME_DATASET_ALLOWED_CURATION_LABELS, settingsService.getValueForKey(SettingsServiceBean.Key.AllowedCurationLabels)),
                                    new Setting<>(SETTING_NAME_DATASET_ZIP_DOWNLOAD_LIMIT, SystemConfig.getLongLimitFromStringOrDefault(settingsService.getValueForKey(SettingsServiceBean.Key.ZipDownloadLimit), SystemConfig.defaultZipDownloadLimit))
                            )
                            ),
                            new SettingGroup(GROUP_NAME_DATAFILE, List.of(
                                    new Setting<>(SETTING_NAME_DATAFILE_MAX_EMBARGO_DURATION_IN_MONTHS, settingsService.getValueForKey(SettingsServiceBean.Key.MaxEmbargoDurationInMonths))
                            ))
                    ));
        }
        return dataverseSettingGroup;
    }

    public String getName() {
        return name;
    }

    private SettingItem getItemByName(String name) {
        for (SettingItem item : itemList) {
            if (item.getName().equals(name)) {
                return item;
            }
        }
        return null;
    }

    public List<SettingItem> getItemList() {
        return this.itemList;
    }

    public SettingItem getItem(String[] orderedNamesRoute) {
        String subItemName = orderedNamesRoute[0];
        if (orderedNamesRoute.length == 1) {
            return getItemByName(subItemName);
        }
        for (SettingItem settingItem : itemList) {
            if (settingItem.getName().equals(subItemName)) {
                return ((SettingGroup) settingItem).getItem(Arrays.copyOfRange(orderedNamesRoute, 1, orderedNamesRoute.length));
            }
        }
        return null;
    }
}
