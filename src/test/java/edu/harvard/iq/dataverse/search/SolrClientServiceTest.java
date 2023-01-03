package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class SolrClientServiceTest {
    
    @Mock
    SettingsServiceBean settingsServiceBean;
    @InjectMocks
    SystemConfig systemConfig;
    SolrClientService clientService = new SolrClientService();
    
    @BeforeEach
    void setUp() {
        clientService.systemConfig = systemConfig;
    }
    
    @Test
    void testInitWithDefaults() {
        // given
        String url = "http://localhost:8983/solr/collection1";
        
        // when
        clientService.init();
        
        // then
        HttpSolrClient client = (HttpSolrClient) clientService.getSolrClient();
        assertEquals(url, client.getBaseURL());
    }
    
    @Test
    @JvmSetting(key = JvmSettings.SOLR_HOST, value = "foobar")
    @JvmSetting(key = JvmSettings.SOLR_PORT, value = "1234")
    @JvmSetting(key = JvmSettings.SOLR_CORE, value = "test")
    void testInitWithConfig() {
        // given
        String url = "http://foobar:1234/solr/test";
        
        // when
        clientService.init();
        
        // then
        HttpSolrClient client = (HttpSolrClient) clientService.getSolrClient();
        assertEquals(url, client.getBaseURL());
    }
}