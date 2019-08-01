package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UrlManagerServiceBeanTest {

    @Mock
    private SystemConfig systemConfig;

    @InjectMocks
    private UrlManagerServiceBean urlManagerServiceBean;

    @BeforeEach
    void setUp() {
        Mockito.when(systemConfig.getDataverseSiteUrl()).thenReturn("https://www.google.com:1234");
    }

    @Test
    void getHostnamePlusBaseUrlPath() {
        // when
        String hostnamePlusBaseUrlPath = urlManagerServiceBean.getHostnamePlusBaseUrlPath();
        // then
        Assert.assertEquals(hostnamePlusBaseUrlPath, "https://www.google.com:1234/dvn/api/data-deposit/v1.1/swordv2");
    }
}