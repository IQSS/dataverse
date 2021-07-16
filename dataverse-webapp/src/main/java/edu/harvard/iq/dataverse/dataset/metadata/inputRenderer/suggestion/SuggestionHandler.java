package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.Suggestion;

import java.util.List;
import java.util.Map;

public interface SuggestionHandler {

    /**
     * Returns unique name of action handler.
     */
    String getName();

    /**
     * Returns name of filters that this handler understands
     * and can handle.
     * <p>
     * Returned values can be used as keys in <code>filteredBy</code>
     * map param of {@link #generateSuggestions(Map, String)}
     */
    List<String> getAllowedFilters();

    /**
     * Returns suggested values based on the given params
     * 
     * @param filteredBy - suggestions will be filtered based on this map
     * @param query - suggestions should match this param in some way 
     * @return suggestions
     */
    List<Suggestion> generateSuggestions(Map<String, String> filteredBy, String query);
}
