package edu.harvard.iq.dataverse.bannersandmessages;

import edu.harvard.iq.dataverse.persistence.MocksFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataverseUtilTest {

    @Test
    public void testGetSuggestedDataverseNameOnCreate() {
        System.out.println("getSuggestedDataverseNameOnCreate");
        assertEquals(null, DataverseUtil.getSuggestedDataverseNameOnCreate(null));
        assertEquals("Homer Simpson Dataverse", DataverseUtil.getSuggestedDataverseNameOnCreate(MocksFactory.makeAuthenticatedUser("Homer", "Simpson")));
    }

}
