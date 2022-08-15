package edu.harvard.iq.dataverse.settings.source;

import edu.harvard.iq.dataverse.settings.Setting;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(OrderAnnotation.class)
class DbSettingConfigSourceTest {

    DbSettingConfigSource dbSource = new DbSettingConfigSource();
    @Mock
    SettingsServiceBean settingsSvc;
    
    @Test
    @Order(1)
    void testEmptyIfNoSettingsService() {
        assertEquals(null, dbSource.getValue("foobar"));
        assertDoesNotThrow(DbSettingConfigSource::updateProperties);
    }
    
    @Test
    @Order(2)
    void testDataRetrieval() {
        Set<Setting> settings = new HashSet<>(Arrays.asList(new Setting(":FooBar", "hello"), new Setting(":FooBarI18N", "de", "hallo")));
        Mockito.when(settingsSvc.listAll()).thenReturn(settings);
    
        DbSettingConfigSource.injectSettingsService(settingsSvc);
        
        assertEquals("hello", dbSource.getValue("dataverse.settings.fromdb.FooBar"));
        assertEquals("hallo", dbSource.getValue("dataverse.settings.fromdb.FooBarI18N.de"));
    }

}