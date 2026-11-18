package edu.harvard.iq.dataverse.harvest.server;

import edu.harvard.iq.dataverse.search.SearchFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OAIServiceTest {

    private final OAISetServiceBean oaiSetServiceBean = new OAISetServiceBean();

    @Test
    public void testOAISetQueryRestrictions() {
        String query = "authorName:king";
        String restrictedQuery = oaiSetServiceBean.addQueryRestrictions(query);
        
        assertTrue(restrictedQuery.contains("-" + SearchFields.LOCALLY_FAIR + ":true"), 
            "Query should contain Locally FAIR restriction: " + restrictedQuery);
    }
}
