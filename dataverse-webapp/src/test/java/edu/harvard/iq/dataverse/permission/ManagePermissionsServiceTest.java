package edu.harvard.iq.dataverse.permission;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseDefaultContributorRoleCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ManagePermissionsServiceTest {
    @InjectMocks
    private ManagePermissionsService managePermissionsService;

    @Mock
    private EjbDataverseEngine commandEngine;
    @Mock
    private DataverseRequestServiceBean dvRequestService;

    private Dataverse dvObject = new Dataverse();
    private AuthenticatedUser roleAssignee = new AuthenticatedUser();
    private DataverseRole role = new DataverseRole();

    @BeforeEach
    public void setUp() {
        dvObject.setId(1L);
        dvObject.setName("testDv");
        roleAssignee.setId(1L);
        role.setId(1L);
        role.setName("testRole");

        when(commandEngine.submit(any(AssignRoleCommand.class))).thenReturn(new RoleAssignment(role, roleAssignee, dvObject, null));
        when(commandEngine.submit(any(CreateRoleCommand.class))).thenReturn(this.role);
        when(commandEngine.submit(any(UpdateDataverseDefaultContributorRoleCommand.class))).thenReturn(this.dvObject);
    }

    @Test
    public void assignRole() {
        // given & when
        RoleAssignment resultAssignment = managePermissionsService.assignRole(role, roleAssignee, dvObject);

        // then
        verify(commandEngine, times(1)).submit(any(AssignRoleCommand.class));
        assertEquals("testRole", resultAssignment.getRole().getName());
    }

    @Test
    public void removeRoleAssignment() {
        // given
        RoleAssignment roleAssignment = new RoleAssignment(role, roleAssignee, dvObject, null);

        // when
        managePermissionsService.removeRoleAssignment(roleAssignment);

        // then
        verify(commandEngine, times(1)).submit(any(RevokeRoleCommand.class));
    }

    @Test
    public void saveOrUpdateRole() {
        // given & when
        DataverseRole savedRole = managePermissionsService.saveOrUpdateRole(role);

        // then
        verify(commandEngine, times(1)).submit(any(CreateRoleCommand.class));
        assertEquals("testRole", savedRole.getName());
    }

    @Test
    public void setDataverseDefaultContributorRole() {
        // given & when
        Dataverse setResult = managePermissionsService.setDataverseDefaultContributorRole(role, dvObject);

        // then
        verify(commandEngine, times(1)).submit(any(UpdateDataverseDefaultContributorRoleCommand.class));
        assertEquals("testDv", setResult.getName());
    }
}
