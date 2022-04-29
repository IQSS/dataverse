package edu.harvard.iq.dataverse.authorization.providers.saml;

import com.onelogin.saml2.util.Util;
import edu.harvard.iq.dataverse.persistence.user.SamlIdentityProvider;
import edu.harvard.iq.dataverse.persistence.user.SamlIdentityProviderRepository;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Date;
import java.util.stream.Collectors;

@Stateless
public class SamlIdpDataFetcher {
    private static final long REFRESH_INTERVAL = 3 * 60 * 60 * 1000; // 3 hours in milliseconds

    private SamlIdentityProviderRepository idpRepository;
    private Clock clock;

    private XmlFetcher xmlFetcher = url -> {
        URL metadataUrl = new URL(url);
        String xml;
        try (InputStream inputStream = metadataUrl.openStream();
             Reader reader = new InputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            xml = bufferedReader.lines().collect(Collectors.joining());
        }
        return xml;
    };

    // -------------------- CONSTRUCTORS --------------------

    public SamlIdpDataFetcher() { }

    @Inject
    public SamlIdpDataFetcher(SamlIdentityProviderRepository idpRepository) {
        this.idpRepository = idpRepository;
        this.clock = Clock.systemDefaultZone();
    }

    SamlIdpDataFetcher(SamlIdentityProviderRepository idpRepository, Clock clock, XmlFetcher xmlFetcher) {
        this.idpRepository = idpRepository;
        this.clock = clock;
        this.xmlFetcher = xmlFetcher;
    }

    // -------------------- LOGIC --------------------

    public Document fetchAndUpdateConfigurationXmlIfNeeded(String idpEntityId) throws Exception {
        SamlIdentityProvider identityProvider = idpRepository.findByEntityId(idpEntityId)
                .orElseThrow(() -> new RuntimeException(String.format("Provider with entity id [%s] not found", idpEntityId)));
        if (StringUtils.isBlank(identityProvider.getConfigurationXml())
                || shouldRefresh(identityProvider.getLastTimeOfXmlDownload())) {
            String url = identityProvider.getMetadataUrl();
            String xml = xmlFetcher.fetchXml(url);
            identityProvider.setConfigurationXml(xml);
            identityProvider.setLastTimeOfXmlDownload(new Timestamp(clock.millis()));
            idpRepository.save(identityProvider);
        }
        return Util.convertStringToDocument(identityProvider.getConfigurationXml());
    }

    // -------------------- PRIVATE --------------------

    private boolean shouldRefresh(Timestamp lastTimeOfXmlDownload) {
        return  lastTimeOfXmlDownload == null
                || (clock.millis() - lastTimeOfXmlDownload.getTime()) > REFRESH_INTERVAL;
    }

    // -------------------- INNER CLASSES --------------------

    public interface XmlFetcher {
        String fetchXml(String url) throws Exception;
    }
}
