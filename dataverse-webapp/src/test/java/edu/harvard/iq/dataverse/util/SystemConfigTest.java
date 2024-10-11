package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SystemConfigTest {

    @Mock
    private SettingsServiceBean settingsService;

    @InjectMocks
    private SystemConfig systemConfig;

    @BeforeEach
    public void setup() {
        Mockito.when(settingsService.getValueForKey(SettingsServiceBean.Key.SiteUrl)).thenReturn("http://www.google.com:1234");
    }

    @Test
    void getDataverseSiteUrl() {
        // when
        String siteUrl = systemConfig.getDataverseSiteUrl();

        // then
        Assertions.assertEquals(siteUrl, "http://www.google.com:1234");
    }

    @Test
    void getDataverseServer() {
        // when
        String siteUrl = systemConfig.getDataverseServer();

        // then
        Assertions.assertEquals(siteUrl, "www.google.com");
    }
}