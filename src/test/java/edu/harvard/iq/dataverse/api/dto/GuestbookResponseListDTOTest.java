package edu.harvard.iq.dataverse.api.dto;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuestbookResponseListDTOTest {

    @Test
    public void testConstructor() {
        Date now = new Date();
        GuestbookResponseListDTO fixture = new GuestbookResponseListDTO(1L,"datasetTitle","Name","My Institution","My Position","Download", now,"file.txt", 3L, "responseText");
        assertEquals(1L,fixture.getId());
        assertEquals("datasetTitle", fixture.getDataset());
        assertEquals("Name",fixture.getName());
        assertEquals("My Institution",fixture.getInstitution());
        assertEquals("My Position",fixture.getPosition());
        assertEquals(now, fixture.getDate());
        assertEquals("file.txt", fixture.getFile());
        assertEquals(3L, fixture.getFileId());
        assertEquals("responseText", fixture.getResponses());
    }

    @Test
    public void testConstructorNull() {
        GuestbookResponseListDTO fixture = new GuestbookResponseListDTO(null,null,null,null,null,null,null,null,null,null);
        assertNull(fixture.getId());
        assertNull(fixture.getDataset());
        assertNull(fixture.getName());
        assertNull(fixture.getInstitution());
        assertNull(fixture.getPosition());
        assertNull(fixture.getDate());
        assertNull(fixture.getFile());
        assertNull(fixture.getFileId());
        assertNull(fixture.getResponses());
    }
}
