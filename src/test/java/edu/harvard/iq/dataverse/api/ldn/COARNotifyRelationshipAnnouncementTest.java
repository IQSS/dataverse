package edu.harvard.iq.dataverse.api.ldn;

import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

public class COARNotifyRelationshipAnnouncementTest {

    private COARNotifyRelationshipAnnouncement handler;

    @BeforeEach
    public void setUp() {
        DatasetServiceBean datasetService = Mockito.mock(DatasetServiceBean.class);
        UserNotificationServiceBean userNotificationService = Mockito.mock(UserNotificationServiceBean.class);
        DataverseRoleServiceBean roleService = Mockito.mock(DataverseRoleServiceBean.class);
        RoleAssigneeServiceBean roleAssigneeService = Mockito.mock(RoleAssigneeServiceBean.class);
        handler = new COARNotifyRelationshipAnnouncement(datasetService, userNotificationService, roleService, roleAssigneeService);
    }

    @Test
    public void testIsTrustedDataCiteUrl() {
        // Trusted DOI resolvers
        assertTrue(handler.isTrustedDataCiteUrl("https://doi.org/10.7910/DVN/TJCLKP"));
        assertTrue(handler.isTrustedDataCiteUrl("http://doi.org/10.7910/DVN/TJCLKP"));
        assertTrue(handler.isTrustedDataCiteUrl("https://dx.doi.org/10.7910/DVN/TJCLKP"));
        assertTrue(handler.isTrustedDataCiteUrl("http://dx.doi.org/10.7910/DVN/TJCLKP"));

        // DataCite API
        assertTrue(handler.isTrustedDataCiteUrl("https://api.datacite.org/dois/10.7910/DVN/TJCLKP"));
        assertTrue(handler.isTrustedDataCiteUrl("http://api.datacite.org/dois/10.7910/DVN/TJCLKP"));
        assertTrue(handler.isTrustedDataCiteUrl("https://api.test.datacite.org/dois/10.7910/DVN/TJCLKP"));
        assertTrue(handler.isTrustedDataCiteUrl("http://api.test.datacite.org/dois/10.7910/DVN/TJCLKP"));

        // Invalid DOIs
        assertFalse(handler.isTrustedDataCiteUrl("https://doi.org/not-a-doi"));
        assertFalse(handler.isTrustedDataCiteUrl("https://api.datacite.org/dois/not-a-doi"));

        // Untrusted sources
        assertFalse(handler.isTrustedDataCiteUrl("https://example.com/metadata.xml"));
        assertFalse(handler.isTrustedDataCiteUrl("https://malicious.org/doi.org/10.1234/5678"));
        
        // Null and empty
        assertFalse(handler.isTrustedDataCiteUrl(null));
        assertFalse(handler.isTrustedDataCiteUrl(""));
    }
}
