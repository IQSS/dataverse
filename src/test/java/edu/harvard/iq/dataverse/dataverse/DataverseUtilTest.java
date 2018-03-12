package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.mocks.MocksFactory;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DataverseUtilTest {

    @Test
    public void testGetSuggestedDataverseNameOnCreate() {
        System.out.println("getSuggestedDataverseNameOnCreate");
        assertEquals(null, DataverseUtil.getSuggestedDataverseNameOnCreate(null));
        assertEquals("Homer Simpson Dataverse", DataverseUtil.getSuggestedDataverseNameOnCreate(MocksFactory.makeAuthenticatedUser("Homer", "Simpson")));
    }

}
