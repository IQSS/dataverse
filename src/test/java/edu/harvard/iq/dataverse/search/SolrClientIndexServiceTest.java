package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@LocalJvmSettings
@ExtendWith(MockitoExtension.class)
class SolrClientIndexServiceTest {

    @Mock
    SettingsServiceBean settingsServiceBean;
    @InjectMocks
    SystemConfig systemConfig;
    SolrClientIndexService clientService = new SolrClientIndexService();

    @BeforeEach
    void setUp() {
        clientService.systemConfig = systemConfig;
    }

    @Test
    void testInitWithDefaults() {
        // when
        clientService.init();

        // then
        ConcurrentUpdateHttp2SolrClient client = (ConcurrentUpdateHttp2SolrClient) clientService.getSolrClient();
        assertNotNull(client);
    }

}