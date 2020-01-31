package edu.harvard.iq.dataverse.search.response;

import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static org.junit.Assert.assertEquals;

public class SolrSearchResultTest {

    @Test
    public void testJson() {

        boolean showRelevance = false;
        boolean showEntityIds = false;
        boolean showApiUrls = false;

        SolrSearchResult result01 = new SolrSearchResult("myQuery", "myName");
        result01.setType(SearchObjectType.DATAVERSES);
        JsonObjectBuilder actual01 = result01.json(showRelevance, showEntityIds, showApiUrls);
        JsonObject actual = actual01.build();
        System.out.println("actual: " + actual);

        JsonObjectBuilder expResult = Json.createObjectBuilder();
        expResult.add("type", SearchConstants.DATAVERSE);
        JsonObject expected = expResult.build();
        System.out.println("expect: " + expected);

        assertEquals(expected, actual);

    }
}
