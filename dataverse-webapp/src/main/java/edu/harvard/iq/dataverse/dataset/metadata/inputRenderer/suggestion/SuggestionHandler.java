package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import java.util.List;
import java.util.Map;

public interface SuggestionHandler {

    /**
     * Returns unique name of action handler.
     */
    String getName();

    List<String> generateSuggestions(Map<String, String> filteredBy, String suggestionSourceFieldName, String suggestionSourceFieldValue);
}
