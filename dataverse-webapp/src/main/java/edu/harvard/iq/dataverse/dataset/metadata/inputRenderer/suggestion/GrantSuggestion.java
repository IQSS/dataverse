package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import javax.ejb.Stateless;
import java.util.Map;
import java.util.Set;

@Stateless
public class GrantSuggestion implements SuggestionHandler {

    private GrantSuggestionDao grantSuggestionDao;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public GrantSuggestion() {
    }

    public GrantSuggestion(GrantSuggestionDao grantSuggestionDao) {
        this.grantSuggestionDao = grantSuggestionDao;
    }

    // -------------------- LOGIC --------------------

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public Set<String> generateSuggestions(Map<String, String> filteredBy, String suggestionSourceField) {
        return null;
    }
}
