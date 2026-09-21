package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseContact;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataverseDTOTest {

    @Test
    public void testDataverseDTO() {
        DataverseDTO dv = new DataverseDTO();
        List<DataverseContact> dataverseContacts = new ArrayList<>();
        dataverseContacts.add(new DataverseContact());
        dataverseContacts.add(new DataverseContact());
        dv.setGuestbookRoot(Boolean.FALSE);
        dv.setAffiliation("affiliation");
        dv.setDataverseType(Dataverse.DataverseType.JOURNALS);
        dv.setDataverseContacts(dataverseContacts);
        dv.setDescription("description");
        dv.setName("name");
        dv.setAlias("alias");
        dv.setDatasetFileCountLimit(Integer.MAX_VALUE);

        assertEquals(Boolean.FALSE, dv.getGuestbookRoot());
        assertEquals("affiliation", dv.getAffiliation());
        assertEquals("description", dv.getDescription());
        assertEquals("name", dv.getName());
        assertEquals("alias", dv.getAlias());
        assertEquals(Integer.MAX_VALUE, dv.getDatasetFileCountLimit());
        assertEquals(2,dv.getDataverseContacts().size());
    }
}
