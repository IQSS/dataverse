package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
        // given
        String url = "http://localhost:8983/solr/collection1";

        // when
        clientService.init();

        // then
        SolrClient client = clientService.getSolrClient();
        assertNotNull(client);
        assertInstanceOf(ConcurrentUpdateHttp2SolrClient.class, client);
        assertEquals(url, clientService.getSolrUrl());
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
        SolrClient client = clientService.getSolrClient();
        assertNotNull(client);
        assertInstanceOf(ConcurrentUpdateHttp2SolrClient.class, client);
        assertEquals(url, clientService.getSolrUrl());
    }
}