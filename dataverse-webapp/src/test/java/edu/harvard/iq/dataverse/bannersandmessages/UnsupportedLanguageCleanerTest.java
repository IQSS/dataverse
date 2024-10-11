package edu.harvard.iq.dataverse.bannersandmessages;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.bannersandmessages.banners.dto.DataverseBannerDto;
import edu.harvard.iq.dataverse.bannersandmessages.banners.dto.DataverseLocalizedBannerDto;
import edu.harvard.iq.dataverse.bannersandmessages.messages.dto.DataverseLocalizedMessageDto;
import edu.harvard.iq.dataverse.bannersandmessages.messages.dto.DataverseTextMessageDto;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UnsupportedLanguageCleanerTest {

    @Mock
    private SettingsWrapper settingsWrapper;

    private UnsupportedLanguageCleaner languageCleaner;

    @BeforeEach
    public void setUp() {
        languageCleaner = new UnsupportedLanguageCleaner(settingsWrapper);

        when(settingsWrapper.getConfiguredLocales()).thenReturn(ImmutableMap.of("en", "English"));
    }

    // -------------------- TESTS --------------------

    @Test
    public void shouldRemoveMessageLanguagesNotPresentInDataverse() {
        //given
        DataverseTextMessageDto textDto = new DataverseTextMessageDto();
        textDto.setDataverseLocalizedMessage(createLocalizedMessages());

        //when
        Assertions.assertEquals(textDto.getDataverseLocalizedMessage().size(), 2L);
        languageCleaner.removeMessageLanguagesNotPresentInDataverse(textDto);

        //then
        Assertions.assertEquals(textDto.getDataverseLocalizedMessage().size(), 1L);
    }

    @Test
    public void shouldRemoveBannersLanguagesNotPresentInDataverse() {
        //given
        DataverseBannerDto bannerDto = new DataverseBannerDto();
        bannerDto.setDataverseLocalizedBanner(createLocalizedBanners());

        //when
        Assertions.assertEquals(bannerDto.getDataverseLocalizedBanner().size(), 2L);
        languageCleaner.removeBannersLanguagesNotPresentInDataverse(bannerDto);

        //then
        Assertions.assertEquals(bannerDto.getDataverseLocalizedBanner().size(), 1L);
    }

    // -------------------- PRIVATE --------------------

    private List<DataverseLocalizedMessageDto> createLocalizedMessages() {
        DataverseLocalizedMessageDto englishLocalMessage = new DataverseLocalizedMessageDto("en", "test", "English");
        DataverseLocalizedMessageDto polishLocalMessage = new DataverseLocalizedMessageDto("pl", "test", "Polski");
        return Lists.newArrayList(englishLocalMessage, polishLocalMessage);
    }

    private List<DataverseLocalizedBannerDto> createLocalizedBanners() {
        DataverseLocalizedBannerDto englishBanner = new DataverseLocalizedBannerDto("en");
        DataverseLocalizedBannerDto polishBanner = new DataverseLocalizedBannerDto("pl");
        return Lists.newArrayList(englishBanner, polishBanner);
    }
}