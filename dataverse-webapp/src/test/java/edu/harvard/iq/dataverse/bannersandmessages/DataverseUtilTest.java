package edu.harvard.iq.dataverse.bannersandmessages;

import edu.harvard.iq.dataverse.mocks.MocksFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DataverseUtilTest {

    @Test
    public void testGetSuggestedDataverseNameOnCreate() {
        System.out.println("getSuggestedDataverseNameOnCreate");
        assertEquals(null, DataverseUtil.getSuggestedDataverseNameOnCreate(null));
        assertEquals("Homer Simpson Dataverse", DataverseUtil.getSuggestedDataverseNameOnCreate(MocksFactory.makeAuthenticatedUser("Homer", "Simpson")));
    }

}
