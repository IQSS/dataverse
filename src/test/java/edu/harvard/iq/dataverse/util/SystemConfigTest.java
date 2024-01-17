package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

@LocalJvmSettings
@ExtendWith(MockitoExtension.class)
class SystemConfigTest {
    
    @InjectMocks
    SystemConfig systemConfig = new SystemConfig();
    @Mock
    SettingsServiceBean settingsService;
    
    @Test
    @JvmSetting(key = JvmSettings.SOLR_HOST, value = "foobar")
    @JvmSetting(key = JvmSettings.SOLR_PORT, value = "1234")
    void testGetSolrHostColonPortNoDBEntry() {
        // given
        String hostPort = "foobar:1234";
        
        // when
        doReturn(null).when(settingsService).getValueForKey(SettingsServiceBean.Key.SolrHostColonPort);
        String result = systemConfig.getSolrHostColonPort();
        
        // then
        assertEquals(hostPort, result);
    }
    
    @Test
    @JvmSetting(key = JvmSettings.SOLR_HOST, value = "foobar")
    @JvmSetting(key = JvmSettings.SOLR_PORT, value = "1234")
    void testGetSolrHostColonPortWithDBEntry() {
        // given
        String dbEntry = "hello:4321";
        
        // when
        doReturn(dbEntry).when(settingsService).getValueForKey(SettingsServiceBean.Key.SolrHostColonPort);
        String result = systemConfig.getSolrHostColonPort();
        
        // then
        assertEquals(dbEntry, result);
    }
    
    @Test
    void testGetSolrHostColonPortDefault() {
        // given
        String hostPort = "localhost:8983";
    
        // when
        doReturn(null).when(settingsService).getValueForKey(SettingsServiceBean.Key.SolrHostColonPort);
        String result = systemConfig.getSolrHostColonPort();
    
        // then
        assertEquals(hostPort, result);
    }
    
    @Test
    void testGetVersion() {
        // given
        String version = "100.100";
        System.setProperty(JvmSettings.VERSION.getScopedKey(), version);
        
        // when
        String result = systemConfig.getVersion(false);
        
        // then
        assertEquals(version, result);
    }
    
    @Test
    @JvmSetting(key = JvmSettings.VERSION, value = "100.100")
    @JvmSetting(key = JvmSettings.BUILD, value = "FOOBAR")
    void testGetVersionWithBuild() {
        // when
        String result = systemConfig.getVersion(true);
    
        // then
        assertTrue(result.startsWith("100.100"), "'" + result + "' not starting with 100.100");
        assertTrue(result.contains("build"));
        
        // Cannot test this here - there might be the bundle file present which is not under test control
        //assertTrue(result.endsWith("FOOBAR"), "'" + result + "' not ending with FOOBAR");
    }
    
    @Test
    void testGetLongLimitFromStringOrDefault_withNullInput() {
        long defaultValue = 5L;
        long actualResult = SystemConfig.getLongLimitFromStringOrDefault(null, defaultValue);
        assertEquals(defaultValue, actualResult);
    }

    @Test
    void testGetIntLimitFromStringOrDefault_withNullInput() {
        int defaultValue = 5;
        int actualResult = SystemConfig.getIntLimitFromStringOrDefault(null, defaultValue);
        assertEquals(defaultValue, actualResult);
    }

    @ParameterizedTest
    @CsvSource({
            ", 5",
            "test, 5",
            "-10, -10",
            "0, 0",
            "10, 10"
    })
    void testGetLongLimitFromStringOrDefault_withStringInputs(String inputString, long expectedResult) {
        long actualResult = SystemConfig.getLongLimitFromStringOrDefault(inputString, 5L);
        assertEquals(expectedResult, actualResult);
    }

    @ParameterizedTest
    @CsvSource({
            ", 5",
            "test, 5",
            "-10, -10",
            "0, 0",
            "10, 10"
    })
    void testGetIntLimitFromStringOrDefault_withStringInputs(String inputString, int expectedResult) {
        int actualResult = SystemConfig.getIntLimitFromStringOrDefault(inputString, 5);
        assertEquals(expectedResult, actualResult);
    }

    @Test
    void testGetThumbnailSizeLimit() {
        assertEquals(3000000l, SystemConfig.getThumbnailSizeLimit("Image"));
        assertEquals(1000000l, SystemConfig.getThumbnailSizeLimit("PDF"));
        assertEquals(0l, SystemConfig.getThumbnailSizeLimit("NoSuchType"));
    }

}
