package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@Stateless
public class GrantSuggestionHandler implements SuggestionHandler {

    private GrantSuggestionDao grantSuggestionDao;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public GrantSuggestionHandler() {
    }

    @Inject
    public GrantSuggestionHandler(GrantSuggestionDao grantSuggestionDao) {
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
     * This implementation generates suggestions with filters if there is something in map, or without filters if map is empty.
     */
    @Override
    public List<String> generateSuggestions(Map<String, String> filters, String suggestionSourceFieldName, String suggestionSourceFieldValue) {

        return grantSuggestionDao.fetchSuggestions(filters, suggestionSourceFieldName.trim(), suggestionSourceFieldValue.trim(), 10);
    }
}
