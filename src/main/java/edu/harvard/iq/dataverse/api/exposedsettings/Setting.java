package edu.harvard.iq.dataverse.api.exposedsettings;

public class Setting<T> extends SettingItem {
    /**
     * Setting names for group {@link edu.harvard.iq.dataverse.api.exposedsettings.SettingGroup#GROUP_NAME_DATAVERSE}
     */
    public static final String SETTING_NAME_FQDN = "fqdn";
    public static final String SETTING_NAME_IS_PUBLIC_INSTALL = "isPublicInstall";

    /**
     * Setting names for group {@link edu.harvard.iq.dataverse.api.exposedsettings.SettingGroup#GROUP_NAME_API}
     */
    public static final String SETTING_NAME_API_TERMS_OF_USE = "apiTermsOfUse";
    public static final String SETTING_NAME_API_ALLOW_INCOMPLETE_METADATA = "apiAllowIncompleteMetadata";

    /**
     * Setting names for group {@link edu.harvard.iq.dataverse.api.exposedsettings.SettingGroup#GROUP_NAME_DATASET}
     */
    public static final String SETTING_NAME_DATASET_ZIP_DOWNLOAD_LIMIT = "datasetZipDownloadLimit";
    public static final String SETTING_NAME_DATASET_PUBLISH_POPUP_CUSTOM_TEXT = "datasetPublishPopupCustomText";
    public static final String SETTING_NAME_DATASET_ALLOWED_CURATION_LABELS = "datasetAllowedCurationLabels";

    private final T value;

    public Setting(String name, T value) {
        super(name);
        this.value = value;
    }

    public T getValue() {
        return value;
    }
}
