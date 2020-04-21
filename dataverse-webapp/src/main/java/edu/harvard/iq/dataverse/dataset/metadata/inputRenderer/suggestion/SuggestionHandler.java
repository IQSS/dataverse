package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import java.util.Map;
import java.util.Set;

public interface SuggestionHandler {

    /**
     * Returns unique name of action handler.
     */
    String getName();

    Set<String> generateSuggestions(Map<String, String> filteredBy, String suggestionSourceField);
}
