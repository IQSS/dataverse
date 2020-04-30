package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.suggestion.GrantSuggestion;

import java.util.HashMap;
import java.util.Map;

/**
 * Class designed to be a connector between {@link edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType}
 * and {@link GrantSuggestion}.
 */
public class GrantSuggestionConnector {

    private final Map<String,String> suggestionsEntityConnector = new HashMap<>();

    // -------------------- CONSTRUCTORS --------------------

    public GrantSuggestionConnector() {
        suggestionsEntityConnector.put(DatasetFieldConstant.grantNumberAgency, GrantSuggestion.getSuggestionNameFieldName());
        suggestionsEntityConnector.put(DatasetFieldConstant.grantNumberAgencyShortName, GrantSuggestion.getGrantAgencyAcronymFieldName());
        suggestionsEntityConnector.put(DatasetFieldConstant.grantNumberProgram, GrantSuggestion.getFundingProgramFieldName());
    }

    // -------------------- LOGIC --------------------

    public String dsftToGrantEntityName(String suggestionName) {
        return suggestionsEntityConnector.get(suggestionName);
    }
}
