package edu.harvard.iq.dataverse;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mail.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.group.Group;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PermissionServiceBeanTest {

    @InjectMocks
    private PermissionServiceBean permissionServiceBean;

    @Mock
    private DataverseRoleServiceBean roleService;

    @Mock
    private SystemConfig systemConfig;

    @Mock
    private GroupServiceBean groupService;

    @Mock
    private ConfirmEmailServiceBean confirmEmailServiceBean;

    private AuthenticatedUser authenticatedUser = MocksFactory.makeAuthenticatedUser("John", "Doe");
    private DataverseRequest authenticatedUserRequest = new DataverseRequest(authenticatedUser, IpAddress.valueOf("127.0.0.1"));

    private GuestUser guestUser = GuestUser.get();
    private DataverseRequest guestUserRequest = new DataverseRequest(guestUser, IpAddress.valueOf("127.0.0.1"));

    private Dataset dataset = MocksFactory.makeDataset();

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should have permission when authenticated user have role assignments that contains checked permission")
    public void requestOn_user_have_correct_role_assignment() {
        // given
        when(roleService.directRoleAssignmentsByAssigneesAndDvObjects(any(), any()))
            .thenReturn(Lists.newArrayList(buildRoleAssignmentForRoleWithPermissions(Permission.ViewUnpublishedDataset, Permission.EditDataset)));

        // when
        boolean hasPermission = permissionServiceBean.requestOn(authenticatedUserRequest, dataset).has(Permission.ViewUnpublishedDataset);

        // then
        assertTrue(hasPermission);
        verify(roleService).directRoleAssignmentsByAssigneesAndDvObjects(
                Sets.newHashSet(authenticatedUser),
                Sets.newHashSet(dataset, dataset.getOwner()));
    }

    @Test
    @DisplayName("Should have permission when authenticated user is member of group which have role assignments that contains checked permission")
    public void requestOn_user_have_correct_role_assignment_by_group_assignment() {
        // given
        Group group = mock(Group.class);

        when(groupService.groupsFor(authenticatedUserRequest, dataset)).thenReturn(Sets.newHashSet(group));
        when(roleService.directRoleAssignmentsByAssigneesAndDvObjects(any(), any()))
            .thenReturn(Lists.newArrayList(buildRoleAssignmentForRoleWithPermissions(Permission.ViewUnpublishedDataset, Permission.EditDataset)));

        // when
        boolean hasPermission = permissionServiceBean.requestOn(authenticatedUserRequest, dataset).has(Permission.ViewUnpublishedDataset);

        // then
        assertTrue(hasPermission);
        verify(roleService).directRoleAssignmentsByAssigneesAndDvObjects(
                Sets.newHashSet(authenticatedUser, group),
                Sets.newHashSet(dataset, dataset.getOwner()));
    }

    @Test
    @DisplayName("Should not have permission when authenticated user do not have role assignments with checked permission")
    public void requestOn_user_do_not_have_correct_role_assignment() {
        // given
        when(roleService.directRoleAssignmentsByAssigneesAndDvObjects(any(), any()))
            .thenReturn(Lists.newArrayList(buildRoleAssignmentForRoleWithPermissions(Permission.ViewUnpublishedDataset, Permission.EditDataset)));

        // when
        boolean hasPermission = permissionServiceBean.requestOn(authenticatedUserRequest, dataset).has(Permission.PublishDataset);

        // then
        assertFalse(hasPermission);
        verify(roleService).directRoleAssignmentsByAssigneesAndDvObjects(
                Sets.newHashSet(authenticatedUser),
                Sets.newHashSet(dataset, dataset.getOwner()));
    }

    @Test
    @DisplayName("Should have permission when guest user have role assignments that contains checked permission")
    public void requestOn_guestUser_have_correct_role_assignment() {
        // given
        when(roleService.directRoleAssignmentsByAssigneesAndDvObjects(any(), any()))
            .thenReturn(Lists.newArrayList(buildRoleAssignmentForRoleWithPermissions(Permission.ViewUnpublishedDataset, Permission.EditDataset)));

        // when
        boolean hasPermission = permissionServiceBean.requestOn(guestUserRequest, dataset).has(Permission.ViewUnpublishedDataset);

        // then
        assertTrue(hasPermission);
        verify(roleService).directRoleAssignmentsByAssigneesAndDvObjects(
                Sets.newHashSet(guestUser),
                Sets.newHashSet(dataset, dataset.getOwner()));
    }

    @Test
    @DisplayName("Should not have permission when user is guest and checked permission is for authenticated users only")
    public void requestOn_guestUser_permission_is_for_authenticated_users_only() {
        // when
        boolean hasPermission = permissionServiceBean.requestOn(guestUserRequest, dataset).has(Permission.EditDataset);

        // then
        assertFalse(hasPermission);
        verifyZeroInteractions(roleService);
    }

    @Test
    @DisplayName("Should not have permission when readonly mode is on and checked permission is write permission")
    public void requestOn_user_do_not_have_permission_for_write_operations_in_readonly_mode() {
        // given
        when(systemConfig.isReadonlyMode()).thenReturn(true);

        // when
        boolean hasPermission = permissionServiceBean.requestOn(authenticatedUserRequest, dataset).has(Permission.PublishDataset);

        // then
        assertFalse(hasPermission);
        verifyZeroInteractions(groupService, roleService);
    }

    @Test
    @DisplayName("Should have permission when readonly mode is on and checked permission is not write permission")
    public void requestOn_user_do_have_permission_for_not_write_operations_in_readonly_mode() {
        // given
        when(systemConfig.isReadonlyMode()).thenReturn(true);
        when(roleService.directRoleAssignmentsByAssigneesAndDvObjects(any(), any()))
            .thenReturn(Lists.newArrayList(buildRoleAssignmentForRoleWithPermissions(Permission.ViewUnpublishedDataset, Permission.EditDataset)));

        // when
        boolean hasPermission = permissionServiceBean.requestOn(authenticatedUserRequest, dataset).has(Permission.ViewUnpublishedDataset);

        // then
        assertTrue(hasPermission);
        verify(roleService).directRoleAssignmentsByAssigneesAndDvObjects(
                Sets.newHashSet(authenticatedUser),
                Sets.newHashSet(dataset, dataset.getOwner()));
    }


    @Test
    @DisplayName("Should have same permissions as defined by role assignments when user is authenticated")
    public void permissionsFor_authenticated_user() {
        // given
        when(roleService.directRoleAssignmentsByAssigneesAndDvObjects(any(), any()))
            .thenReturn(Lists.newArrayList(buildRoleAssignmentForRoleWithPermissions(Permission.ViewUnpublishedDataset, Permission.EditDataset)));

        // when
        Set<Permission> permissions = permissionServiceBean.permissionsFor(authenticatedUserRequest, dataset);

        // then
        assertThat(permissions).containsExactlyInAnyOrder(Permission.ViewUnpublishedDataset, Permission.EditDataset);
        verify(roleService).directRoleAssignmentsByAssigneesAndDvObjects(
                Sets.newHashSet(authenticatedUser),
                Sets.newHashSet(dataset, dataset.getOwner()));
    }

    @Test
    @DisplayName("Should have all permissions when user is superadmin")
    public void permissionsFor_superuser() {
        // given
        authenticatedUser.setSuperuser(true);

        // when
        Set<Permission> permissions = permissionServiceBean.permissionsFor(authenticatedUserRequest, dataset);

        // then
        assertThat(permissions).containsExactlyInAnyOrder(Permission.values());
    }

    @Test
    @DisplayName("Should have permissions without any permission dedicated for authenticated users only")
    public void permissionsFor_guest() {
        // given
        when(roleService.directRoleAssignmentsByAssigneesAndDvObjects(any(), any()))
            .thenReturn(Lists.newArrayList(buildRoleAssignmentForRoleWithPermissions(
                    Permission.PublishDataset,
                    Permission.ViewUnpublishedDataset,
                    Permission.EditDataset)));

        // when
        Set<Permission> permissions = permissionServiceBean.permissionsFor(guestUserRequest, dataset);

        // then
        assertThat(permissions).containsExactlyInAnyOrder(Permission.ViewUnpublishedDataset);
    }

    @Test
    @DisplayName("Should have only read permissions when user is authenticated and readonly mode is on")
    public void permissionsFor_authenticated_users_in_readonly_mode() {
        // given
        when(systemConfig.isReadonlyMode()).thenReturn(true);
        when(roleService.directRoleAssignmentsByAssigneesAndDvObjects(any(), any()))
            .thenReturn(Lists.newArrayList(buildRoleAssignmentForRoleWithPermissions(Permission.values())));

        // when
        Set<Permission> permissions = permissionServiceBean.permissionsFor(authenticatedUserRequest, dataset);

        // then
        assertThat(permissions).containsExactlyInAnyOrder(Permission.ViewUnpublishedDataverse, Permission.ViewUnpublishedDataset, Permission.DownloadFile);
    }

    @Test
    @DisplayName("Should have only read permissions when user is superuser and readonly mode is on")
    public void permissionsFor_superuser_in_readonly_mode() {
        // given
        authenticatedUser.setSuperuser(true);
        when(systemConfig.isReadonlyMode()).thenReturn(true);

        // when
        Set<Permission> permissions = permissionServiceBean.permissionsFor(authenticatedUserRequest, dataset);

        // then
        assertThat(permissions).containsExactlyInAnyOrder(Permission.ViewUnpublishedDataverse, Permission.ViewUnpublishedDataset, Permission.DownloadFile);
    }

    @Test
    @DisplayName("Should have edit dataverse permission")
    public void permissionsFor_user_with_editDataverse_permission() {
        //given
        User user = new AuthenticatedUser();
        Dataverse dataverse = new Dataverse();

        //when
        when(roleService.directRoleAssignmentsByAssigneesAndDvObjects(any(), any()))
                .thenReturn(Lists.newArrayList(buildRoleAssignmentForRoleWithPermissions(
                        Permission.EditDataverse)));
        boolean isUserAllowedToEditDataverse = permissionServiceBean.isUserAbleToEditDataverse(user, dataverse);
        //then
        assertTrue(isUserAllowedToEditDataverse);
    }

    @Test
    @DisplayName("Shouldn't have edit dataverse permission")
    public void permissionsFor_user_without_editDataverse_permission() {
        //given
        User user = new AuthenticatedUser();
        Dataverse dataverse = new Dataverse();

        //when
        boolean isUserAllowedToEditDataverse = permissionServiceBean.isUserAbleToEditDataverse(user, dataverse);
        //then
        assertFalse(isUserAllowedToEditDataverse);
    }

    // -------------------- PRIVATE --------------------

    private RoleAssignment buildRoleAssignmentForRoleWithPermissions(Permission... permissions) {
        RoleAssignment roleAssignment = new RoleAssignment();

        DataverseRole role = new DataverseRole();
        role.addPermissions(Lists.newArrayList(permissions));
        roleAssignment.setRole(role);

        return roleAssignment;
    }

    private List<RoleAssignment> assignRoleForUserInDataverse(User user, String roleAlias) {
        RoleAssignment roleAssignment = new RoleAssignment();

        DataverseRole adminRole = new DataverseRole();
        adminRole.setAlias(roleAlias);

        roleAssignment.setRole(adminRole);
        roleAssignment.setAssigneeIdentifier(user.getIdentifier());
        return Collections.singletonList(roleAssignment);
    }
}
