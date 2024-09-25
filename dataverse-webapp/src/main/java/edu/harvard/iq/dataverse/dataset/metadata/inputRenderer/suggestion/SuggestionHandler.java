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
     * @return true if this handler is dependent on siblings input values.
     * If it is dependant it will use other input values
     * to filter out while generating suggestions.
     * Sibling means the group of inputs that shares common parent input.
     * F.e.: when input grant is parent and have children as follow: grantAgency,
     * grantRor, grantAcronym it means that whenever one of its children will have value
     * it will be taken to filter out values during generating suggestions.
     * Of course its need to be configured via <code>getAllowedFilters() method</code>
     *
     * @see this#getAllowedFilters()
     */
    boolean isDependentOnSiblings();

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
