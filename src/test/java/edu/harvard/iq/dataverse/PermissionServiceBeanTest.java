package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PermissionServiceBeanTest {

    @InjectMocks
    private PermissionServiceBean permissionService;

    @Mock
    private GroupServiceBean groupService;

    @Mock
    private DataverseRoleServiceBean roleService;

    @Mock
    private DvObjectServiceBean dvObjectServiceBean;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testIsPowerUserOn_Superuser() {
        AuthenticatedUser superuser = mock(AuthenticatedUser.class);
        when(superuser.isSuperuser()).thenReturn(true);
        Dataverse dv = new Dataverse();

        assertTrue(permissionService.isPowerUserOn(superuser, dv));
    }

    @Test
    public void testIsPowerUserOn_NullUser() {
        Dataverse dv = new Dataverse();
        assertFalse(permissionService.isPowerUserOn(null, dv));
    }

    @Test
    public void testIsPowerUserOn_NullDvObject() {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        assertFalse(permissionService.isPowerUserOn(user, null));
    }

    @Test
    public void testIsPowerUserOn_WithAssignment() {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.isSuperuser()).thenReturn(false);
        when(user.isAuthenticated()).thenReturn(true);
        when(user.getIdentifier()).thenReturn("@user");
        
        Dataverse dv = new Dataverse();
        dv.setId(1L);

        when(groupService.groupsFor(eq(user), eq(dv))).thenReturn(Collections.emptySet());

        DataverseRole powerUserRole = new DataverseRole();
        powerUserRole.addPermission(Permission.ScopedPowerUser);
        
        RoleAssignment assignment = new RoleAssignment(powerUserRole, user, dv, null);
        
        when(roleService.directRoleAssignments(anySet(), anySet())).thenReturn(new ArrayList<>(Collections.singletonList(assignment)));

        assertTrue(permissionService.isPowerUserOn(user, dv));
    }

    @Test
    public void testIsPowerUserOn_WithParentAssignment() {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.isSuperuser()).thenReturn(false);
        when(user.isAuthenticated()).thenReturn(true);
        when(user.getIdentifier()).thenReturn("@user");
        
        Dataverse parent = new Dataverse();
        parent.setId(1L);
        parent.setPermissionRoot(true);
        
        Dataverse child = new Dataverse();
        child.setId(2L);
        child.setOwner(parent);

        when(groupService.groupsFor(eq(user), any())).thenReturn(Collections.emptySet());

        DataverseRole powerUserRole = new DataverseRole();
        powerUserRole.addPermission(Permission.ScopedPowerUser);
        
        RoleAssignment assignment = new RoleAssignment(powerUserRole, user, parent, null);
        
        when(roleService.directRoleAssignments(anySet(), anySet())).thenReturn(new ArrayList<>(Collections.singletonList(assignment)));

        assertTrue(permissionService.isPowerUserOn(user, child));
    }

    @Test
    public void testIsPowerUserOn_BlockedNormalAdmin() {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.isSuperuser()).thenReturn(false);
        when(user.isAuthenticated()).thenReturn(true);
        when(user.getIdentifier()).thenReturn("@user");
        Dataverse dv = new Dataverse();
        dv.setId(1L);

        when(groupService.groupsFor(eq(user), eq(dv))).thenReturn(Collections.emptySet());

        DataverseRole adminRole = new DataverseRole();
        adminRole.addPermission(Permission.EditDataverse);
        
        RoleAssignment assignment = new RoleAssignment(adminRole, user, dv, null);
        
        when(roleService.directRoleAssignments(anySet(), anySet())).thenReturn(new ArrayList<>(Collections.singletonList(assignment)));

        assertFalse(permissionService.isPowerUserOn(user, dv));
    }

    @Test
    public void testHasPermissionsFor_PowerUser() {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.isSuperuser()).thenReturn(false);
        when(user.isAuthenticated()).thenReturn(true);
        when(user.getIdentifier()).thenReturn("@user");
        Dataverse dv = new Dataverse();
        dv.setId(1L);

        DataverseRole powerUserRole = new DataverseRole();
        powerUserRole.addPermission(Permission.ScopedPowerUser);
        RoleAssignment assignment = new RoleAssignment(powerUserRole, user, dv, null);
        when(roleService.directRoleAssignments(anySet(), anySet())).thenReturn(new ArrayList<>(Collections.singletonList(assignment)));

        assertTrue(permissionService.hasPermissionsFor(user, dv, EnumSet.of(Permission.AddDataverse)));
    }

    @Test
    public void testPermissionsFor_PowerUser() {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.isSuperuser()).thenReturn(false);
        when(user.isAuthenticated()).thenReturn(true);
        when(user.getIdentifier()).thenReturn("@user");
        Dataverse dv = new Dataverse();
        dv.setId(1L);

        DataverseRole powerUserRole = new DataverseRole();
        powerUserRole.addPermission(Permission.ScopedPowerUser);
        RoleAssignment assignment = new RoleAssignment(powerUserRole, user, dv, null);
        when(roleService.directRoleAssignments(anySet(), anySet())).thenReturn(new ArrayList<>(Collections.singletonList(assignment)));

        Set<Permission> perms = permissionService.permissionsFor(user, dv);
        assertEquals(EnumSet.allOf(Permission.class), perms);
    }

    @Test
    public void testWhichChildrenHasPermissionsFor_PowerUser() {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.isSuperuser()).thenReturn(false);
        when(user.isAuthenticated()).thenReturn(true);
        when(user.getIdentifier()).thenReturn("@user");
        
        Dataverse parent = new Dataverse();
        parent.setId(1L);
        
        Dataverse child1 = new Dataverse();
        child1.setId(2L);
        Dataverse child2 = new Dataverse();
        child2.setId(3L);
        
        List<DvObject> children = Arrays.asList(child1, child2);
        when(dvObjectServiceBean.findByOwnerId(1L)).thenReturn(children);

        DataverseRole powerUserRole = new DataverseRole();
        powerUserRole.addPermission(Permission.ScopedPowerUser);
        RoleAssignment assignment = new RoleAssignment(powerUserRole, user, parent, null);
        when(roleService.directRoleAssignments(anySet(), anySet())).thenReturn(new ArrayList<>(Collections.singletonList(assignment)));

        DataverseRequest req = mock(DataverseRequest.class);
        when(req.getUser()).thenReturn(user);

        List<DvObject> result = permissionService.whichChildrenHasPermissionsFor(req, parent, EnumSet.of(Permission.AddDataverse));
        assertEquals(2, result.size());
        assertTrue(result.contains(child1));
        assertTrue(result.contains(child2));
    }

    @Test
    public void testWhichChildrenHasPermissionsFor_PowerUserOnSingleChild() {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.isSuperuser()).thenReturn(false);
        when(user.isAuthenticated()).thenReturn(true);
        when(user.getIdentifier()).thenReturn("@user");

        Dataverse parent = new Dataverse();
        parent.setId(1L);

        Dataverse child1 = new Dataverse();
        child1.setId(2L);
        child1.setOwner(parent);
        Dataverse child2 = new Dataverse();
        child2.setId(3L);
        child2.setOwner(parent);

        List<DvObject> children = Arrays.asList(child1, child2);
        when(dvObjectServiceBean.findByOwnerId(1L)).thenReturn(children);

        // Power User assignment only on child1
        DataverseRole powerUserRole = new DataverseRole();
        powerUserRole.addPermission(Permission.ScopedPowerUser);
        RoleAssignment assignment = new RoleAssignment(powerUserRole, user, child1, null);

        // Group service returns empty for both
        when(groupService.groupsFor(any(DataverseRequest.class))).thenReturn(Collections.emptySet());
        when(groupService.groupsFor(eq(user), any())).thenReturn(Collections.emptySet());

        // roleService.directRoleAssignments should return the assignment for child1 when called with children
        // We need to capture the set of objects passed to directRoleAssignments or use a matcher
        when(roleService.directRoleAssignments(anySet(), anyCollection())).thenAnswer(invocation -> {
            Collection<DvObject> dvos = invocation.getArgument(1);
            if (dvos != null && dvos.contains(child1)) {
                return new ArrayList<>(Collections.singletonList(assignment));
            }
            return new ArrayList<>();
        });

        DataverseRequest req = mock(DataverseRequest.class);
        when(req.getUser()).thenReturn(user);

        List<DvObject> result = permissionService.whichChildrenHasPermissionsFor(req, parent, EnumSet.of(Permission.AddDataverse));
        
        assertEquals(1, result.size());
        assertTrue(result.contains(child1));
        assertFalse(result.contains(child2));
    }

    @Test
    public void testHasLocallyFAIRAccess_PowerUser() {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.isSuperuser()).thenReturn(false);
        when(user.isAuthenticated()).thenReturn(true);
        when(user.getIdentifier()).thenReturn("@user");
        
        Dataverse dv = new Dataverse();
        dv.setId(1L);
        dv.setLocallyFAIRRoleAssigneeIdentifiers(Collections.singleton("@some_group"));

        DataverseRole powerUserRole = new DataverseRole();
        powerUserRole.addPermission(Permission.ScopedPowerUser);
        RoleAssignment assignment = new RoleAssignment(powerUserRole, user, dv, null);
        when(roleService.directRoleAssignments(anySet(), anySet())).thenReturn(new ArrayList<>(Collections.singletonList(assignment)));

        DataverseRequest req = mock(DataverseRequest.class);
        when(req.getUser()).thenReturn(user);

        assertTrue(permissionService.hasLocallyFAIRAccess(req, dv));
    }
}
