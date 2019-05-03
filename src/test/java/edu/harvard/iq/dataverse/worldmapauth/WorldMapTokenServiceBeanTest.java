package edu.harvard.iq.dataverse.worldmapauth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean.StaticPermissionQuery;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;

public class WorldMapTokenServiceBeanTest {

    private WorldMapTokenServiceBean worldMapTokenServiceBean;

    @Before
    public void setUp() {
        this.worldMapTokenServiceBean = new WorldMapTokenServiceBean();
        this.worldMapTokenServiceBean.permissionService = mock(PermissionServiceBean.class);
    }

    @After
    public void tearDown() {
        this.worldMapTokenServiceBean = null;
    }

    private WorldMapToken createWorldMapToken(AuthenticatedUser user, DataFile file) {
        WorldMapToken worldMapToken = new WorldMapToken();
        worldMapToken.setDataverseUser(user);
        worldMapToken.setDatafile(file);

        return worldMapToken;
    }

    private void mockPermissionServiceCallForEditDataset(AuthenticatedUser user, DataFile file,
            boolean isUserPermitted) {
        StaticPermissionQuery query = mock(StaticPermissionQuery.class);
        Mockito.when(this.worldMapTokenServiceBean.permissionService.userOn(user, file)).thenReturn(query);
        Mockito.when(query.has(Permission.EditDataset)).thenReturn(isUserPermitted);
    }

    @Test
    public void testCanTokenUserEditFileWithUndefinedWorldMapToken() {
        WorldMapToken worldMapToken = null;

        assertFalse(this.worldMapTokenServiceBean.canTokenUserEditFile(worldMapToken));
    }

    @Test
    public void testCanTokenUserEditFileWithPermission() {
        AuthenticatedUser user = new AuthenticatedUser();
        DataFile file = new DataFile();
        WorldMapToken worldMapToken = this.createWorldMapToken(user, file);

        this.mockPermissionServiceCallForEditDataset(user, file, true);

        assertTrue(this.worldMapTokenServiceBean.canTokenUserEditFile(worldMapToken));
    }

    @Test
    public void testCanTokenUserEditFileWithoutPermission() {
        AuthenticatedUser user = new AuthenticatedUser();
        DataFile file = new DataFile();
        WorldMapToken worldMapToken = this.createWorldMapToken(user, file);

        this.mockPermissionServiceCallForEditDataset(user, file, false);

        assertFalse(this.worldMapTokenServiceBean.canTokenUserEditFile(worldMapToken));
    }
}