package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.GrantSuperuserStatusCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeAllRolesCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeSuperuserStatusCommand;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DashboardUsersServiceTest {

    @InjectMocks
    private DashboardUsersService dashboardUsersService;

    @Mock
    private EjbDataverseEngine engineService;
    @Mock
    private DataverseRequestServiceBean dataverseRequestService;
    @Mock
    private UserServiceBean userServiceBean;

    private AuthenticatedUser testUser = createTestUser();

    @BeforeEach
    void setUp() throws CommandException {
        when(engineService.submit(any(GrantSuperuserStatusCommand.class))).thenReturn(new AuthenticatedUser());
        when(engineService.submit(any(RevokeSuperuserStatusCommand.class))).thenReturn(new AuthenticatedUser());
        when(engineService.submit(any(RevokeAllRolesCommand.class))).thenReturn(new AuthenticatedUser());
        when(userServiceBean.find(any())).thenReturn(testUser);
    }

    @Test
    public void revokeAllRolesForUser() {
        // given
        AuthenticatedUser user = createTestUser();

        // when
        dashboardUsersService.revokeAllRolesForUser(user);

        // then
        verify(engineService, times(1)).submit(any(RevokeAllRolesCommand.class));
    }

    @Test
    public void changeSuperuserStatus_grantSuperuser() {
        // given
        AuthenticatedUser user = createTestUser();
        user.setSuperuser(false);
        this.testUser.setSuperuser(false);

        // when
        dashboardUsersService.changeSuperuserStatus(user);

        // then
        verify(engineService, times(1)).submit(any(GrantSuperuserStatusCommand.class));
    }

    @Test
    public void changeSuperuserStatus_revokeSuperuser() {
        // given
        AuthenticatedUser user = createTestUser();
        user.setSuperuser(true);
        this.testUser.setSuperuser(true);

        // when
        dashboardUsersService.changeSuperuserStatus(user);

        // then
        verify(engineService, times(1)).submit(any(RevokeSuperuserStatusCommand.class));
    }

    // -------------------- PRIVATE ---------------------

    private AuthenticatedUser createTestUser() {
        AuthenticatedUser user = new AuthenticatedUser();
        user.setFirstName("test");
        user.setLastName("user");
        user.setRoles("testRole");
        user.setSuperuser(true);

        return user;
    }
}
