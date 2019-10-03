package edu.harvard.iq.dataverse.search;

import org.apache.solr.common.SolrDocument;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DvObjectsSolrAssert {

    
    // -------------------- LOGIC --------------------
    
    public static void assertDataverseSolrDocument(SolrDocument actualDataverseSolrDoc,
            Long expectedEntityId, String expectedAlias, String expectedName) {
        
        assertEquals("dataverses", actualDataverseSolrDoc.getFieldValue("dvObjectType"));
        assertEquals("dataverse_" + expectedEntityId, actualDataverseSolrDoc.getFieldValue("id"));
        assertEquals(expectedEntityId, actualDataverseSolrDoc.getFieldValue("entityId"));
        assertEquals(expectedAlias, actualDataverseSolrDoc.getFieldValue("identifier"));
        assertEquals(expectedAlias, actualDataverseSolrDoc.getFieldValue("dvAlias"));
        assertEquals(expectedName, actualDataverseSolrDoc.getFieldValue("dvName"));
        assertEquals(expectedName, actualDataverseSolrDoc.getFieldValue("name"));
        
    }
    
    public static void assertDataversePermSolrDocument(SolrDocument actualDataversePermSolrDoc,
            Long expectedDefinitionPointId, List<Long> expectedDiscoverableByUserIds) {
        
        assertEquals("dataverse_" + expectedDefinitionPointId + "_permission", actualDataversePermSolrDoc.getFieldValue("id"));
        assertEquals("dataverse_" + expectedDefinitionPointId, actualDataversePermSolrDoc.getFieldValue("definitionPointDocId"));
        assertEquals(String.valueOf(expectedDefinitionPointId), actualDataversePermSolrDoc.getFieldValue("definitionPointDvObjectId"));
        
        Collection<Object> solrDocDiscoverableBy = actualDataversePermSolrDoc.getFieldValues("discoverableBy");
        
        for (Long expectedDiscoverableByUserId: expectedDiscoverableByUserIds) {
            assertThat(solrDocDiscoverableBy, hasItem("group_user" + expectedDiscoverableByUserId));
        }
        assertThat(solrDocDiscoverableBy, hasSize(expectedDiscoverableByUserIds.size()));
        
    }
    
}
