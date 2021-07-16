package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.Suggestion;
import edu.harvard.iq.dataverse.persistence.dataset.suggestion.GrantSuggestion;

import javax.ejb.Stateless;
import javax.inject.Inject;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Stateless
public class GrantAgencyAcronymSuggestionHandler implements SuggestionHandler {

    private GrantSuggestionDao grantSuggestionDao;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public GrantAgencyAcronymSuggestionHandler() {
    }

    @Inject
    public GrantAgencyAcronymSuggestionHandler(GrantSuggestionDao grantSuggestionDao) {
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
     * This implementation support filter by suggestionName
     * (it can be interpreted as localized grant agency name)
     */
    @Override
    public List<String> getAllowedFilters() {
        return Lists.newArrayList(GrantSuggestion.SUGGESTION_NAME_COLUMN);
    }

    @Override
    public List<Suggestion> generateSuggestions(Map<String, String> filters, String suggestionSourceFieldValue) {

        return grantSuggestionDao.fetchSuggestions(filters, GrantSuggestion.GRANT_AGENCY_ACRONYM_COLUMN, suggestionSourceFieldValue.trim(), 10)
                .stream().map(suggestionString -> new Suggestion(suggestionString))
                .collect(toList());
    }

}
