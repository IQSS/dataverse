package edu.harvard.iq.dataverse.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class SearchFilesServiceBeanTest {

    @Test
    public void testGetSortBy() {
        assertEquals(new SortBy(SearchFields.RELEVANCE, SortBy.DESCENDING), SearchFilesServiceBean.getSortBy(null));
    }
}
