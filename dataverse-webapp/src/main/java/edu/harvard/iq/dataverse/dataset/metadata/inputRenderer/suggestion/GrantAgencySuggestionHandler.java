package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.Suggestion;
import edu.harvard.iq.dataverse.persistence.dataset.suggestion.GrantSuggestion;

import javax.ejb.Stateless;
import javax.inject.Inject;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Stateless
public class GrantAgencySuggestionHandler implements SuggestionHandler {

    private GrantSuggestionDao grantSuggestionDao;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public GrantAgencySuggestionHandler() {
    }

    @Inject
    public GrantAgencySuggestionHandler(GrantSuggestionDao grantSuggestionDao) {
        this.grantSuggestionDao = grantSuggestionDao;
    }

    // -------------------- LOGIC --------------------

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns class name.
     */
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation doesn't support filters. Always
     * returns empty map.
     */
    @Override
    public List<String> getAllowedFilters() {
        return Collections.emptyList();
    }

    @Override
    public List<Suggestion> generateSuggestions(Map<String, String> filters, String suggestionSourceFieldValue) {

        return grantSuggestionDao.fetchSuggestions(filters, GrantSuggestion.SUGGESTION_NAME_COLUMN, suggestionSourceFieldValue.trim(), 10)
                .stream().map(suggestionString -> new Suggestion(suggestionString))
                .collect(toList());
    }
}
