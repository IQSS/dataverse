package edu.harvard.iq.dataverse;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.PermissionServiceBean.RequestPermissionQuery;
import edu.harvard.iq.dataverse.authorization.Permission;

public class PermissionsWrapperTest {

    private PermissionsWrapper permissionWrapper;

    @Before
    public void setUp() {
        this.permissionWrapper = new PermissionsWrapper();
        this.permissionWrapper.permissionService = mock(PermissionServiceBean.class);
        this.permissionWrapper.dvRequestService = mock(DataverseRequestServiceBean.class);
    }

    @After
    public void tearDown() {
        this.permissionWrapper = null;
    }

    @Test
    public void testCanManageDatasetPermissionsWithUndefinedDataverse() {
        Dataverse dataverse = null;
        User user = GuestUser.get();

        assertFalse(this.permissionWrapper.canManageDataversePermissions(user, dataverse));
    }

    @Test
    public void testCanManageDatasetPermissionsWithUndefinedDataverseId() {
        Dataverse dataverse = new Dataverse();
        User user = GuestUser.get();

        assertFalse(this.permissionWrapper.canManageDataversePermissions(user, dataverse));
    }

    @Test
    public void testCanManageDatasetPermissionsWithUndefinedUser() {
        Dataverse dataverse = new Dataverse();
        dataverse.setId(1L);
        User user = null;

        assertFalse(this.permissionWrapper.canManageDataversePermissions(user, dataverse));
    }

    @Test
    public void testCanManageDatasetPermissions() {
        Dataverse dataverse = new Dataverse();
        dataverse.setId(1L);
        User user = GuestUser.get();

        DataverseRequest dataverseRequest = this.permissionWrapper.dvRequestService.getDataverseRequest();
        RequestPermissionQuery requestPermissionQuery = mock(RequestPermissionQuery.class);

        // mock permission service request
        Mockito.when(this.permissionWrapper.permissionService.requestOn(dataverseRequest, dataverse)).thenReturn(requestPermissionQuery);
        Mockito.when(requestPermissionQuery.has(Permission.ManageDataversePermissions)).thenReturn(true);

        assertTrue(this.permissionWrapper.canManageDataversePermissions(user, dataverse));
    }

}