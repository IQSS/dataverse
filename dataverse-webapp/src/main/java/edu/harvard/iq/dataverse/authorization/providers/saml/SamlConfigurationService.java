package edu.harvard.iq.dataverse.authorization.providers.saml;

import com.onelogin.saml2.settings.IdPMetadataParser;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.settings.SettingsBuilder;
import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.user.SamlIdentityProvider;
import edu.harvard.iq.dataverse.persistence.user.SamlIdentityProviderRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import io.vavr.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Stateless
public class SamlConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(SamlConfigurationService.class);

    private static final String HOST_PLACEHOLDER = "\\{HOST}";

    private SettingsServiceBean settingsService;
    private SamlIdentityProviderRepository samlIdpRepository;
    private AuthenticationServiceBean authenticationService;
    private SamlIdpDataFetcher samlIdpDataFetcher;

    private BiFunction<String, String, Map<String, Object>> parser = (String idpMetadataUrl, String idpEntityId) -> {
        try {
            Document document = samlIdpDataFetcher.fetchAndUpdateConfigurationXmlIfNeeded(idpEntityId);
            return IdPMetadataParser.parseXML(document, idpEntityId);
        } catch (Exception e) {
            logger.warn("Error while fetching or parsing Idp settings", e);
            throw new IllegalStateException("Cannot fetch Idp settings", e);
        }
    };

        // -------------------- CONSTRUCTORS --------------------

    public SamlConfigurationService() { }

    @Inject
    public SamlConfigurationService(SettingsServiceBean settingsService, SamlIdentityProviderRepository samlIdpRepository,
                                    AuthenticationServiceBean authenticationService, SamlIdpDataFetcher samlIdpDataFetcher) {
        this.settingsService = settingsService;
        this.samlIdpRepository = samlIdpRepository;
        this.authenticationService = authenticationService;
        this.samlIdpDataFetcher = samlIdpDataFetcher;
    }

    SamlConfigurationService(SettingsServiceBean settingsService, SamlIdentityProviderRepository samlIdpRepository,
                                    AuthenticationServiceBean authenticationService, SamlIdpDataFetcher samlIdpDataFetcher,
                                    BiFunction<String, String, Map<String, Object>> parser) {
        this(settingsService, samlIdpRepository, authenticationService, samlIdpDataFetcher);
        this.parser = parser;
    }

    // -------------------- LOGIC --------------------

    public boolean isSamlLoginEnabled() {
        AuthenticationProvider samlProvider = authenticationService.getAuthenticationProvider("saml");
        return samlProvider != null;
    }

    public Saml2Settings buildSettings(SamlIdentityProvider provider) {
        return buildSettings(readSpSettings(), parser.apply(provider.getMetadataUrl(), provider.getEntityId()));
    }

    public Saml2Settings buildSettings(String idpEntityId) {
        SamlIdentityProvider provider = samlIdpRepository.findByEntityId(idpEntityId)
                .orElseThrow(() -> new RuntimeException("There is no provider with id =" + idpEntityId));
        return buildSettings(provider);
    }

    public Saml2Settings buildSpSettings() {
        return buildSettings(readSpSettings(), Collections.emptyMap());
    }

    public List<SamlIdentityProvider> getRegisteredProviders() {
        return samlIdpRepository.findAll();
    }

    public SamlIdentityProvider getProviderById(Long providerId) {
        return samlIdpRepository.findById(providerId).orElse(null);
    }

    // -------------------- PRIVATE --------------------

    private Saml2Settings buildSettings(Map<String, Object> spSettings, Map<String, Object> idpSettings) {
        HashMap<String, Object> settings = new HashMap<>(spSettings);
        settings.putAll(idpSettings);
        return new SettingsBuilder().fromValues(settings).build();
    }

    private Map<String, Object> readSpSettings() {
        Map<String, String> spSettings = settingsService.getFileBasedSettingsForPrefix(":onelogin");
        return spSettings.entrySet().stream()
                .map(e -> Tuple.of(normalizeKey(e.getKey()), substitutePlaceholders(e.getValue())))
                .collect(HashMap::new, (m, e) -> m.put(e._1(), e._2()), Map::putAll);
    }

    private String substitutePlaceholders(String value) {
        return value != null
                ? value.replaceAll(HOST_PLACEHOLDER, settingsService.getValueForKey(SettingsServiceBean.Key.SiteUrl))
                : null;
    }

    private String normalizeKey(String key) {
        return key.startsWith(":") ? key.substring(1) : key;
    }
}
