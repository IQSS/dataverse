package edu.harvard.iq.dataverse.dashboard;

import com.rometools.utils.Lists;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ejb.EJB;
import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional(TransactionMode.ROLLBACK)
public class DashboardUsersServiceIT extends WebappArquillianDeployment {

    @Inject
    private DataverseSession dataverseSession;
    @EJB
    private AuthenticationServiceBean authenticationServiceBean;
    @Inject
    private DashboardUsersService dashboardUsersService;
    @Inject
    private RoleAssigneeServiceBean roleAssigneeService;

    @BeforeEach
    public void setUp() {
        dataverseSession.setUser(authenticationServiceBean.getAdminUser());
    }

    @Test
    public void shouldRevokeAllRolesForUser() {
        // given
        AuthenticatedUser user = authenticationServiceBean.findByID(2L);

        // when
        dashboardUsersService.revokeAllRolesForUser(user.getId());

        // then
        AuthenticatedUser dbUser = authenticationServiceBean.findByID(2L);


        assertTrue(roleAssigneeService.getUserExplicitGroups(dbUser).isEmpty());
        assertTrue(Lists.isEmpty(roleAssigneeService.getAssignmentsFor(dbUser.getUserIdentifier())));
    }

    @Test
    public void shouldRevokeAllRolesForUser_withPermissionsException() {
        // given
        AuthenticatedUser user = authenticationServiceBean.findByID(2L);
        dataverseSession.setUser(authenticationServiceBean.findByID(4L));

        // when & then
        assertThrows(PermissionException.class, () -> dashboardUsersService.revokeAllRolesForUser(user.getId()));
    }

    @Test
    public void shouldChangeSuperuserStatus_revokeSuperuserStatus() {
        // given
        AuthenticatedUser user = authenticationServiceBean.findByID(2L);

        // when
        AuthenticatedUser dbUser = dashboardUsersService.changeSuperuserStatus(user.getId());

        // then
        assertNotNull(dbUser);
        assertFalse(dbUser.isSuperuser());
    }

    @Test
    public void shouldChangeSuperuserStatus_grantSuperuserStatus() {
        // given
        AuthenticatedUser user = authenticationServiceBean.findByID(3L);

        // when
        AuthenticatedUser dbUser = dashboardUsersService.changeSuperuserStatus(user.getId());

        // then
        assertNotNull(dbUser);
        assertTrue(dbUser.isSuperuser());
    }

    @Test
    public void shouldChangeSuperuserStatus_revokeSuperuserStatus_withPermissionException() {
        // given
        AuthenticatedUser user = authenticationServiceBean.findByID(2L);
        dataverseSession.setUser(authenticationServiceBean.findByID(4L));

        // when & then
        assertThrows(PermissionException.class, () -> dashboardUsersService.changeSuperuserStatus(user.getId()));
    }

    @Test
    public void shouldChangeSuperuserStatus_grantSuperuserStatus_withPermissionException() {
        // given
        AuthenticatedUser user = authenticationServiceBean.findByID(3L);
        dataverseSession.setUser(authenticationServiceBean.findByID(4L));

        // when & then
        assertThrows(PermissionException.class, () -> dashboardUsersService.changeSuperuserStatus(user.getId()));
    }
}
