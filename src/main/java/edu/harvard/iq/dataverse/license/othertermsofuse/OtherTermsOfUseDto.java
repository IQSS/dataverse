package edu.harvard.iq.dataverse.license.othertermsofuse;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import org.primefaces.model.StreamedContent;

public class OtherTermsOfUseDto {

    private Key isActiveSettingKey;

    private String universalDisplayName;

    private boolean active;

    private StreamedContent badge;

    // -------------------- CONSTRUCTORS --------------------

    public OtherTermsOfUseDto(Key isActiveSettingKey, String universalDisplayName, boolean active, StreamedContent badge) {
        this.isActiveSettingKey = isActiveSettingKey;
        this.universalDisplayName = universalDisplayName;
        this.active = active;
        this.badge = badge;
    }


    // -------------------- GETTERS --------------------

    /**
     * Key that represents value in the properties file or db.
     */
    public Key getIsActiveSettingKey() {
        return isActiveSettingKey;
    }

    /**
     * Display name property, so we can have the same name regardless of selected language.
     */
    public String getUniversalDisplayName() {
        return universalDisplayName;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Other terms of use image.
     */
    public StreamedContent getBadge() {
        return badge;
    }

    // -------------------- SETTERS --------------------

    public void setUniversalDisplayName(String universalDisplayName) {
        this.universalDisplayName = universalDisplayName;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
