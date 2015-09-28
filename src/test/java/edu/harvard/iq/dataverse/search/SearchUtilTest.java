/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DatasetFieldConstant;
import java.sql.Timestamp;
import java.util.Arrays;
import org.apache.solr.common.SolrInputDocument;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class SearchUtilTest {

    public SearchUtilTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        SearchUtil searchUtil = new SearchUtil();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSanitizeQuery() {
        System.out.println("sanitizeQuery");
        assertEquals(null, SearchUtil.sanitizeQuery(null));
        assertEquals("", SearchUtil.sanitizeQuery(""));
        assertEquals("doi\\:10.5072/FK2/4QEJQV", SearchUtil.sanitizeQuery("doi:10.5072/FK2/4QEJQV"));
        assertEquals("datasetPersistentIdentifier:doi\\:10.5072/FK2/4QEJQV", SearchUtil.sanitizeQuery("datasetPersistentIdentifier:doi:10.5072/FK2/4QEJQV"));
        assertEquals("doi\\:4QEJQV", SearchUtil.sanitizeQuery("doi:4QEJQV"));
        assertEquals("hdl\\:1902.1/21919", SearchUtil.sanitizeQuery("hdl:1902.1/21919"));
        assertEquals("datasetPersistentIdentifier:hdl\\:1902.1/21919", SearchUtil.sanitizeQuery("datasetPersistentIdentifier:hdl:1902.1/21919"));
    }

    @Test
    public void testCreateSolrDoc() {
        assertEquals(null, SearchUtil.createSolrDoc(null));
        Long datasetVersionId = 345678l;
        SolrInputDocument solrInputDocument = SearchUtil.createSolrDoc(new DvObjectSolrDoc("12345", "dataset_12345", datasetVersionId, "myNameOrTitleNotUsedHere", Arrays.asList(IndexServiceBean.getPublicGroupString())));
        System.out.println(solrInputDocument.toString());
        assertEquals(SearchFields.ID + "=" + IndexServiceBean.solrDocIdentifierDataset + "12345" + IndexServiceBean.discoverabilityPermissionSuffix, solrInputDocument.get(SearchFields.ID).toString());
        assertEquals(SearchFields.DEFINITION_POINT + "=dataset_12345", solrInputDocument.get(SearchFields.DEFINITION_POINT).toString());
        assertEquals(SearchFields.DEFINITION_POINT_DVOBJECT_ID + "=12345", solrInputDocument.get(SearchFields.DEFINITION_POINT_DVOBJECT_ID).toString());
        assertEquals(SearchFields.DISCOVERABLE_BY + "=" + Arrays.asList(IndexServiceBean.getPublicGroupString()), solrInputDocument.get(SearchFields.DISCOVERABLE_BY).toString());
    }

    @Test
    public void testGetTimestampOrNull() {
        assertNull(SearchUtil.getTimestampOrNull(null));
        assertEquals("1970-01-12T10:20:54Z", SearchUtil.getTimestampOrNull(new Timestamp(987654321l)));
    }

    @Test
    public void testGetSortBy() throws Exception {

        SortBy sortByUnspecified = SearchUtil.getSortBy(null, null);
        assertEquals(SearchFields.RELEVANCE, sortByUnspecified.getField());
        assertEquals(SortBy.DESCENDING, sortByUnspecified.getOrder());

        SortBy sortByName = SearchUtil.getSortBy("name", null);
        assertEquals(SearchFields.NAME_SORT, sortByName.getField());
        assertEquals(SortBy.ASCENDING, sortByName.getOrder());

        SortBy sortByDate = SearchUtil.getSortBy("date", null);
        assertEquals(SearchFields.RELEASE_OR_CREATE_DATE, sortByDate.getField());
        assertEquals(SortBy.DESCENDING, sortByDate.getOrder());

        SortBy sortByAuthorName = SearchUtil.getSortBy(DatasetFieldConstant.authorName, null);
        assertEquals(DatasetFieldConstant.authorName, sortByAuthorName.getField());
        assertEquals(SortBy.ASCENDING, sortByAuthorName.getOrder());

        try {
            SortBy sortByExceptionExpected = SearchUtil.getSortBy(null, "unsortable");
        } catch (Exception ex) {
            assertEquals(Exception.class, ex.getClass());
        }
    }

    @Test
    public void testdetermineFinalQuery() {
        assertEquals("*", SearchUtil.determineFinalQuery(null));
        assertEquals("*", SearchUtil.determineFinalQuery(""));
        assertEquals("foo", SearchUtil.determineFinalQuery("foo"));
    }
}
