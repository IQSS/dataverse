package edu.harvard.iq.dataverse.externalvocabulary;

import jakarta.json.JsonObject;
import java.util.List;
import java.util.Optional;

public interface ExternalVocabularyProvider {

    String getProtocol();

    List<ExternalVocabularyTerm> search(String query, JsonObject config, ExternalVocabularyContext context)
            throws ExternalVocabularyException;

    Optional<ExternalVocabularyTerm> resolve(String uri, JsonObject config, ExternalVocabularyContext context)
            throws ExternalVocabularyException;
}
