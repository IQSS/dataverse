package edu.harvard.iq.dataverse.externalvocabulary;

import jakarta.json.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ExternalVocabularyProviderRegistry {

    private final Map<String, ExternalVocabularyProvider> providers = new HashMap<>();
    private final HttpJsonExternalVocabularyProvider httpJsonProvider = new HttpJsonExternalVocabularyProvider();

    public ExternalVocabularyProviderRegistry() {
        register(new SkosmosExternalVocabularyProvider());
        register(new OrcidExternalVocabularyProvider());
    }

    public Optional<ExternalVocabularyProvider> find(String protocol) {
        if (protocol == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(providers.get(protocol.toLowerCase()));
    }

    public Optional<ExternalVocabularyProvider> find(JsonObject config) {
        Optional<ExternalVocabularyProvider> provider = find(config.getString("protocol", ""));
        if (provider.isPresent()) {
            return provider;
        }
        if (httpJsonProvider.supports(config)) {
            return Optional.of(httpJsonProvider);
        }
        return Optional.empty();
    }

    private void register(ExternalVocabularyProvider provider) {
        providers.put(provider.getProtocol().toLowerCase(), provider);
    }
}
