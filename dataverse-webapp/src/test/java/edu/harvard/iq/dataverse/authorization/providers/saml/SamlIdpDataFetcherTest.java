package edu.harvard.iq.dataverse.authorization.providers.saml;

import edu.harvard.iq.dataverse.persistence.user.SamlIdentityProvider;
import edu.harvard.iq.dataverse.persistence.user.SamlIdentityProviderRepository;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SamlIdpDataFetcherTest {

    private SamlIdpDataFetcher fetcher;

    @Mock
    private SamlIdentityProviderRepository repository;

    private SamlIdpDataFetcher.XmlFetcher xmlFetcher;

    @BeforeEach
    void setUp() {
        SamlIdentityProvider provider = new SamlIdentityProvider(1L, "eid", "url", "IdP");
        provider.setLastTimeOfXmlDownload(Timestamp.from(Instant.ofEpochMilli(0L)));
        provider.setConfigurationXml("<conf></conf>");
        Mockito.when(repository.findByEntityId(Mockito.anyString()))
                .thenReturn(Optional.of(provider));
        xmlFetcher = _url -> IOUtils.resourceToString("/saml/idp-metadata.xml", StandardCharsets.UTF_8);
    }

    // -------------------- TESTS --------------------

    @Test
    void fetchAndUpdateConfigurationXmlIfNeeded__shouldUpdate() throws Exception {
        // given
        fetcher = new SamlIdpDataFetcher(repository, Clock.fixed(Instant.ofEpochMilli(1_000_000_000L), ZoneOffset.UTC), xmlFetcher);

        // when
        Document parsed = fetcher.fetchAndUpdateConfigurationXmlIfNeeded("");

        // then
        Mockito.verify(repository, Mockito.times(1)).findByEntityId(Mockito.anyString());
        Mockito.verify(repository, Mockito.times(1)).save(Mockito.any());
        assertThat(parsed.getDocumentElement().getTagName()).isEqualTo("md:EntityDescriptor");
    }

    @Test
    void fetchAndUpdateConfigurationXmlIfNeeded__shouldNotUpdate() throws Exception {
        // given
        fetcher = new SamlIdpDataFetcher(repository, Clock.fixed(Instant.ofEpochMilli(1L), ZoneOffset.UTC), xmlFetcher);

        // when
        Document parsed = fetcher.fetchAndUpdateConfigurationXmlIfNeeded("");

        // then
        Mockito.verify(repository, Mockito.times(1)).findByEntityId(Mockito.anyString());
        Mockito.verify(repository, Mockito.never()).save(Mockito.any());
        assertThat(parsed.getDocumentElement().getTagName()).isEqualTo("conf");
    }
}