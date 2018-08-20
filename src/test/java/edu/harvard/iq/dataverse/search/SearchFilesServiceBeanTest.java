package edu.harvard.iq.dataverse.search;


import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SearchFilesServiceBeanTest {

    
    @Test
    public void testGetSortBy() {
        assertEquals(new SortBy(SearchFields.RELEVANCE, SortBy.DESCENDING), SearchFilesServiceBean.getSortBy(null));
    }
}
