package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import edu.harvard.iq.dataverse.persistence.dataset.suggestion.GrantSuggestion;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class GrantSuggestionConnectorTest {


    @Test
    public void grantSuggestionConnector_ContainsAllSupportedTypes() {
        //given
        GrantSuggestionConnector grantSuggestionConnector = new GrantSuggestionConnector();

        //when
        String grantNumberAgency = grantSuggestionConnector.dsftToGrantEntityName("grantNumberAgency");
        String grantNumberAgencyShortName = grantSuggestionConnector.dsftToGrantEntityName("grantNumberAgencyShortName");
        String grantNumberProgram = grantSuggestionConnector.dsftToGrantEntityName("grantNumberProgram");

        //then
        Assert.assertEquals(grantNumberAgency, GrantSuggestion.getSuggestionNameFieldName());
        Assert.assertEquals(grantNumberAgencyShortName, GrantSuggestion.getGrantAgencyAcronymFieldName());
        Assert.assertEquals(grantNumberProgram, GrantSuggestion.getFundingProgramFieldName());
    }
}