package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;
import edu.harvard.iq.dataverse.search.query.SortBy;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SearchFilesServiceBeanTest {

    @Test
    public void testGetSortBy() {
        assertEquals(new SortBy(SearchFields.RELEVANCE, SortOrder.desc), SearchFilesServiceBean.getSortBy(null));
    }
}
