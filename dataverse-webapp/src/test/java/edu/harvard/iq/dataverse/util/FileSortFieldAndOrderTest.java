package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FileSortFieldAndOrderTest {

    @Test
    public void testSortFiles() {

        FileSortFieldAndOrder bothUnspecified = new FileSortFieldAndOrder(null, null);
        assertEquals("displayOrder", bothUnspecified.getSortField());
        assertEquals(SortOrder.asc, bothUnspecified.getSortOrder());

        FileSortFieldAndOrder unspecifiedFieldAsc = new FileSortFieldAndOrder(null, SortOrder.asc);
        assertEquals("displayOrder", unspecifiedFieldAsc.getSortField());
        assertEquals(SortOrder.asc, unspecifiedFieldAsc.getSortOrder());

        FileSortFieldAndOrder unspecifiedFieldDesc = new FileSortFieldAndOrder(null, SortOrder.desc);
        assertEquals("displayOrder", unspecifiedFieldDesc.getSortField());
        assertEquals(SortOrder.desc, unspecifiedFieldDesc.getSortOrder());

        FileSortFieldAndOrder labelAsc = new FileSortFieldAndOrder("label", null);
        assertEquals("label", labelAsc.getSortField());
        assertEquals(SortOrder.asc, labelAsc.getSortOrder());

        FileSortFieldAndOrder createDate = new FileSortFieldAndOrder("dataFile.createDate", null);
        assertEquals("dataFile.createDate", createDate.getSortField());
        assertEquals(SortOrder.asc, createDate.getSortOrder());

        FileSortFieldAndOrder junkField = new FileSortFieldAndOrder("junk", null);
        assertEquals("label", junkField.getSortField());
        assertEquals(SortOrder.asc, junkField.getSortOrder());

        FileSortFieldAndOrder junkFieldDesc = new FileSortFieldAndOrder("junk", SortOrder.desc);
        assertEquals("label", junkFieldDesc.getSortField());
        assertEquals(SortOrder.desc, junkFieldDesc.getSortOrder());

        FileSortFieldAndOrder sizeNull = new FileSortFieldAndOrder("dataFile.filesize", null);
        assertEquals("dataFile.filesize", sizeNull.getSortField());
        assertEquals(SortOrder.asc, sizeNull.getSortOrder());

        FileSortFieldAndOrder contentType = new FileSortFieldAndOrder("dataFile.contentType", null);
        assertEquals("dataFile.contentType", contentType.getSortField());
        assertEquals(SortOrder.asc, contentType.getSortOrder());

        FileSortFieldAndOrder contentTypeAsc = new FileSortFieldAndOrder("dataFile.contentType", SortOrder.asc);
        assertEquals("dataFile.contentType", contentTypeAsc.getSortField());
        assertEquals(SortOrder.asc, contentTypeAsc.getSortOrder());

        FileSortFieldAndOrder contentTypeDesc = new FileSortFieldAndOrder("dataFile.contentType", SortOrder.desc);
        assertEquals("dataFile.contentType", contentTypeDesc.getSortField());
        assertEquals(SortOrder.desc, contentTypeDesc.getSortOrder());

    }

    @Test
    public void shouldSortFilesByDisplayOrder() {
        // given
        FileSortFieldAndOrder displayOrder = new FileSortFieldAndOrder("displayOrder", null);

        // expect
        assertEquals("displayOrder", displayOrder.getSortField());
        assertEquals(SortOrder.asc, displayOrder.getSortOrder());
    }

    @Test
    public void shouldSortFilesByDisplayOrderWithGivenSortType() {
        // given
        FileSortFieldAndOrder displayOrderAsc = new FileSortFieldAndOrder("displayOrder", SortOrder.asc);

        // expect
        assertEquals("displayOrder", displayOrderAsc.getSortField());
        assertEquals(SortOrder.asc, displayOrderAsc.getSortOrder());
    }

    @Test
    public void shouldSortFilesByDisplayOrderDescending() {
        // given
        FileSortFieldAndOrder displayOrderDesc = new FileSortFieldAndOrder("displayOrder", SortOrder.desc);

        // expect
        assertEquals("displayOrder", displayOrderDesc.getSortField());
        assertEquals(SortOrder.desc, displayOrderDesc.getSortOrder());
    }

}
