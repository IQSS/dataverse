package edu.harvard.iq.dataverse.search;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SearchFilesServiceBeanTest {

    @Test
    public void testGetSortBy() {
        assertEquals(new SortBy(SearchFields.RELEVANCE, SortBy.DESCENDING), SearchFilesServiceBean.getSortBy(null));
    }
}
