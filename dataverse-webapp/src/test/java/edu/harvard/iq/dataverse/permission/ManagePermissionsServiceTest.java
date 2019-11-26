package edu.harvard.iq.dataverse.permission;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseDefaultContributorRoleCommand;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.UserNotificationService;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.Timestamp;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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
    @Mock
    private RoleAssigneeServiceBean roleAssigneeService;
    @Mock
    private UserNotificationService userNotificationService;

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
        role.setAlias("notFileDownloader");

        when(commandEngine.submit(any(AssignRoleCommand.class))).thenReturn(new RoleAssignment(role, roleAssignee, dvObject, null));
        when(commandEngine.submit(any(CreateRoleCommand.class))).thenReturn(this.role);
        when(commandEngine.submit(any(UpdateDataverseDefaultContributorRoleCommand.class))).thenReturn(this.dvObject);
        doNothing().when(userNotificationService).sendNotificationWithEmail(any(AuthenticatedUser.class), any(Timestamp.class), any(NotificationType.class), any(Long.class), any(NotificationObjectType.class));
    }

    @Test
    public void assignRoleWithNotification() {
        // given & when
        RoleAssignment resultAssignment = managePermissionsService.assignRoleWithNotification(role, roleAssignee, dvObject);

        // then
        verify(commandEngine, times(1)).submit(any(AssignRoleCommand.class));
        assertEquals("testRole", resultAssignment.getRole().getName());
    }

    @Test
    public void removeRoleAssignmentWithNotification() {
        // given
        RoleAssignment roleAssignment = new RoleAssignment(role, roleAssignee, dvObject, null);

        // when
        managePermissionsService.removeRoleAssignmentWithNotification(roleAssignment);

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
