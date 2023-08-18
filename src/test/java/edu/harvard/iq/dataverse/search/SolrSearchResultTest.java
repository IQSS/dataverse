package edu.harvard.iq.dataverse.search;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// ****************************************************************************************
// The following tests test the setPublicationStatuses method aiming for 100% prime 
// path coverage. Various combinations of statuses are used to execute and cover every 
// possible branch of the method under test.
//
// More details here: https://github.com/IQSS/dataverse/pull/5705
// ****************************************************************************************

public class SolrSearchResultTest {

    String unpublishedFlag;
    String publishedFlag;
    String draftFlag;
    String inReviewFlag;
    String deaccessionedFlag;
    String invalidFlag;

    List<String> statuses;

    SolrSearchResult solrSearchResult;

    @BeforeEach
    public void before() {
        this.unpublishedFlag = IndexServiceBean.getUNPUBLISHED_STRING();
        this.publishedFlag = IndexServiceBean.getPUBLISHED_STRING();
        this.draftFlag = IndexServiceBean.getDRAFT_STRING();
        this.inReviewFlag = IndexServiceBean.getIN_REVIEW_STRING();
        this.deaccessionedFlag = IndexServiceBean.getDEACCESSIONED_STRING();
        this.invalidFlag = "abc";
        this.statuses = new ArrayList<String>();
        this.solrSearchResult = new SolrSearchResult("myQuery", "myName");
    }

    @AfterEach
    public void after() {
        this.unpublishedFlag = null;
        this.publishedFlag = null;
        this.draftFlag = null;
        this.inReviewFlag = null;
        this.deaccessionedFlag = null;
        this.invalidFlag = null;
        this.statuses = null;
        this.solrSearchResult = null;
    }

    @Test
    public void testSetPublicationStatuses1() {
        // path [1,4,5,6,8,12,14,6,8,13,14,6,8,11,14,6,8,12,14,6,7]
        this.statuses.add(this.inReviewFlag);
        this.statuses.add(this.deaccessionedFlag);
        this.statuses.add(this.draftFlag);
        this.statuses.add(this.inReviewFlag);

        this.solrSearchResult.setPublicationStatuses(this.statuses);

        assertEquals(this.statuses, solrSearchResult.getPublicationStatuses());
        assertTrue(this.solrSearchResult.isInReviewState());
        assertTrue(this.solrSearchResult.isDeaccessionedState());
        assertTrue(this.solrSearchResult.isDraftState());
    }

    @Test
    public void testSetPublicationStatuses2() {
        // path [1,4,5,6,8,9,14,6,8,10,14,6,8,9,14,6,7]
        this.statuses.add(this.unpublishedFlag);
        this.statuses.add(this.publishedFlag);
        this.statuses.add(this.unpublishedFlag);

        this.solrSearchResult.setPublicationStatuses(this.statuses);

        assertEquals(this.statuses, solrSearchResult.getPublicationStatuses());
        assertTrue(this.solrSearchResult.isUnpublishedState());
        assertTrue(this.solrSearchResult.isPublishedState());
    }

    @Test
    public void testSetPublicationStatuses3() {
        // path [1,4,5,6,8,14,6,8,14,6,7]
        this.statuses.add(this.invalidFlag);
        this.statuses.add(this.invalidFlag);

        this.solrSearchResult.setPublicationStatuses(this.statuses);

        assertEquals(this.statuses, solrSearchResult.getPublicationStatuses());
    }

    @Test
    public void testSetPublicationStatuses4() {
        // path [1,2,3]
        this.statuses = null;

        this.solrSearchResult.setPublicationStatuses(this.statuses);

        assertEquals(new ArrayList<>(), solrSearchResult.getPublicationStatuses());
    }

    @Test
    public void testSetPublicationStatuses5() {
        // path [1,4,5,6,8,14,6,7]
        this.statuses.add(this.invalidFlag);

        this.solrSearchResult.setPublicationStatuses(this.statuses);

        assertEquals(this.statuses, solrSearchResult.getPublicationStatuses());
    }

    @Test
    public void testSetPublicationStatuses6() {
        // path [1,4,5,6,8,11,14,6,7]
        this.statuses.add(this.draftFlag);

        this.solrSearchResult.setPublicationStatuses(this.statuses);

        assertEquals(this.statuses, solrSearchResult.getPublicationStatuses());
        assertTrue(this.solrSearchResult.isDraftState());
    }

    @Test
    public void testSetPublicationStatuses7() {
        // path [1,4,5,6,8,10,14,6,7]
        this.statuses.add(this.publishedFlag);

        this.solrSearchResult.setPublicationStatuses(this.statuses);

        assertEquals(this.statuses, solrSearchResult.getPublicationStatuses());
        assertTrue(this.solrSearchResult.isPublishedState());
    }

    @Test
    public void testSetPublicationStatuses8() {
        // path [1,4,5,6,8,13,14,6,7]
        this.statuses.add(this.deaccessionedFlag);

        this.solrSearchResult.setPublicationStatuses(this.statuses);

        assertEquals(this.statuses, solrSearchResult.getPublicationStatuses());
        assertTrue(this.solrSearchResult.isDeaccessionedState());
    }

    @Test
    public void testSetPublicationStatuses9() {
        // path [1,4,5,6,7]
        this.solrSearchResult.setPublicationStatuses(this.statuses);

        assertEquals(this.statuses, solrSearchResult.getPublicationStatuses());
    }

    @Test
    public void testSetPublicationStatuses10() {
        // path [1,4,5,6,8,13,14,6,8,10,14,6,7]
        this.statuses.add(this.deaccessionedFlag);
        this.statuses.add(this.publishedFlag);

        this.solrSearchResult.setPublicationStatuses(this.statuses);

        assertEquals(this.statuses, solrSearchResult.getPublicationStatuses());
        assertTrue(this.solrSearchResult.isDeaccessionedState());
        assertTrue(this.solrSearchResult.isPublishedState());
    }

    @Test
    public void testSetPublicationStatuses11() {
        // path [1,4,5,6,8,13,14,6,8,9,14,6,7]
        this.statuses.add(this.deaccessionedFlag);
        this.statuses.add(this.unpublishedFlag);

        this.solrSearchResult.setPublicationStatuses(this.statuses);

        assertEquals(this.statuses, solrSearchResult.getPublicationStatuses());
        assertTrue(this.solrSearchResult.isDeaccessionedState());
        assertTrue(this.solrSearchResult.isUnpublishedState());
    }

    @Test
    public void testSetPublicationStatuses12() {
        // path [1,4,5,6,8,10,14,6,8,13,14,6,7]
        this.statuses.add(this.publishedFlag);
        this.statuses.add(this.deaccessionedFlag);

        this.solrSearchResult.setPublicationStatuses(this.statuses);

        assertEquals(this.statuses, solrSearchResult.getPublicationStatuses());
        assertTrue(this.solrSearchResult.isPublishedState());
        assertTrue(this.solrSearchResult.isDeaccessionedState());
    }

    @Test
    public void testSetPublicationStatuses13() {
        // path [1,4,5,6,8,10,14,6,8,10,14,6,7]
        this.statuses.add(this.publishedFlag);
        this.statuses.add(this.publishedFlag);

        this.solrSearchResult.setPublicationStatuses(this.statuses);

        assertEquals(this.statuses, solrSearchResult.getPublicationStatuses());
        assertTrue(this.solrSearchResult.isPublishedState());
    }

    @Test
    public void testSetPublicationStatuses14() {
        // path [1,4,5,6,8,9,14,6,8,13,14,6,7]
        this.statuses.add(this.unpublishedFlag);
        this.statuses.add(this.deaccessionedFlag);

        this.solrSearchResult.setPublicationStatuses(this.statuses);

        assertEquals(this.statuses, solrSearchResult.getPublicationStatuses());
        assertTrue(this.solrSearchResult.isUnpublishedState());
        assertTrue(this.solrSearchResult.isDeaccessionedState());
    }

    @Test
    public void testJson() {

        boolean showRelevance = false;
        boolean showEntityIds = false;
        boolean showApiUrls = false;

        SolrSearchResult result01 = new SolrSearchResult("myQuery", "myName");
        result01.setType(SearchConstants.DATAVERSES);
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
