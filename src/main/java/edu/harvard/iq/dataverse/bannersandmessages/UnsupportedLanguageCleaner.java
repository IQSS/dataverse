package edu.harvard.iq.dataverse.bannersandmessages;

import edu.harvard.iq.dataverse.bannersandmessages.banners.dto.DataverseBannerDto;
import edu.harvard.iq.dataverse.bannersandmessages.messages.dto.DataverseTextMessageDto;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Set;

@Stateless
public class UnsupportedLanguageCleaner {

    private SettingsWrapper settingsWrapper;

    // -------------------- CONSTRUCTORS --------------------

    public UnsupportedLanguageCleaner() {
    }

    @Inject
    public UnsupportedLanguageCleaner(SettingsWrapper settingsWrapper) {
        this.settingsWrapper = settingsWrapper;
    }

    // -------------------- LOGIC --------------------

    public DataverseTextMessageDto removeMessageLanguagesNotPresentInDataverse(DataverseTextMessageDto textMessageDto) {
        Set<String> dataverseLocales = settingsWrapper.getConfiguredLocales().keySet();

        textMessageDto.getDataverseLocalizedMessage()
                .removeIf(localizedMessageDto -> !dataverseLocales.contains(localizedMessageDto.getLocale()));

        return textMessageDto;
    }

    public DataverseBannerDto removeBannersLanguagesNotPresentInDataverse(DataverseBannerDto dataverseBannerDto) {
        Set<String> dataverseLocales = settingsWrapper.getConfiguredLocales().keySet();

        dataverseBannerDto.getDataverseLocalizedBanner()
                .removeIf(localizedBannerDto -> !dataverseLocales.contains(localizedBannerDto.getLocale()));

        return dataverseBannerDto;
    }
}
