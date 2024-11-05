package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import javax.faces.context.FacesContext;
import java.util.Map;
import java.util.Optional;

public class SuggestionAutocompleteHelper {
    /**
     * Processes a suggestion query based on the provided autocomplete ID.
     * This method retrieves the current JSF context and fetches request parameters.
     * It filters the parameters to find the first one that ends with the specified
     * autocomplete ID followed by "_query". The result is returned as an
     * {@link Optional} containing the corresponding value, or empty if no match is found.
     *
     * @param autoCompleteId the ID used to identify the autocomplete query
     * @return an {@link Optional} containing the first matching suggestion query
     *         value if found, or an empty {@link Optional} if no such parameter exists
     */
    public static Optional<String> processSuggestionQuery(String autoCompleteId) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        Map<String, String> parameterMap = ctx.getExternalContext().getRequestParameterMap();
        return parameterMap.keySet().stream()
                .filter(key -> key.endsWith(autoCompleteId + "_query"))
                .map(parameterMap::get)
                .findFirst();
    }
}
