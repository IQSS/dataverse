package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.search.SortBy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileSortFieldAndOrderTest {

    @Test
    public void testSortFiles() {

        FileSortFieldAndOrder bothUnspecified = new FileSortFieldAndOrder(null, null);
        assertEquals("label", bothUnspecified.getSortField());
        assertEquals(SortBy.ASCENDING, bothUnspecified.getSortOrder());

        FileSortFieldAndOrder unspecifiedFieldAsc = new FileSortFieldAndOrder(null, SortBy.ASCENDING);
        assertEquals("label", unspecifiedFieldAsc.getSortField());
        assertEquals(SortBy.ASCENDING, unspecifiedFieldAsc.getSortOrder());

        FileSortFieldAndOrder unspecifiedFieldDesc = new FileSortFieldAndOrder(null, SortBy.DESCENDING);
        assertEquals("label", unspecifiedFieldDesc.getSortField());
        assertEquals(SortBy.DESCENDING, unspecifiedFieldDesc.getSortOrder());

        FileSortFieldAndOrder unspecifiedFieldJunkOrder = new FileSortFieldAndOrder(null, "junk");
        assertEquals("label", unspecifiedFieldJunkOrder.getSortField());
        assertEquals(SortBy.ASCENDING, unspecifiedFieldJunkOrder.getSortOrder());

        FileSortFieldAndOrder labelAsc = new FileSortFieldAndOrder("label", null);
        assertEquals("label", labelAsc.getSortField());
        assertEquals(SortBy.ASCENDING, labelAsc.getSortOrder());

        FileSortFieldAndOrder createDate = new FileSortFieldAndOrder("dataFile.createDate", null);
        assertEquals("dataFile.createDate", createDate.getSortField());
        assertEquals(SortBy.ASCENDING, createDate.getSortOrder());

        FileSortFieldAndOrder junkField = new FileSortFieldAndOrder("junk", null);
        assertEquals("label", junkField.getSortField());
        assertEquals(SortBy.ASCENDING, junkField.getSortOrder());

        FileSortFieldAndOrder junkFieldDesc = new FileSortFieldAndOrder("junk", SortBy.DESCENDING);
        assertEquals("label", junkFieldDesc.getSortField());
        assertEquals(SortBy.DESCENDING, junkFieldDesc.getSortOrder());

        FileSortFieldAndOrder sizeNull = new FileSortFieldAndOrder("dataFile.filesize", null);
        assertEquals("dataFile.filesize", sizeNull.getSortField());
        assertEquals(SortBy.ASCENDING, sizeNull.getSortOrder());

        FileSortFieldAndOrder contentType = new FileSortFieldAndOrder("dataFile.contentType", null);
        assertEquals("dataFile.contentType", contentType.getSortField());
        assertEquals(SortBy.ASCENDING, contentType.getSortOrder());

        FileSortFieldAndOrder contentTypeAsc = new FileSortFieldAndOrder("dataFile.contentType", SortBy.ASCENDING);
        assertEquals("dataFile.contentType", contentTypeAsc.getSortField());
        assertEquals(SortBy.ASCENDING, contentTypeAsc.getSortOrder());

        FileSortFieldAndOrder contentTypeDesc = new FileSortFieldAndOrder("dataFile.contentType", SortBy.DESCENDING);
        assertEquals("dataFile.contentType", contentTypeDesc.getSortField());
        assertEquals(SortBy.DESCENDING, contentTypeDesc.getSortOrder());

    }

}
