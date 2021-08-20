package edu.harvard.iq.dataverse.dashboard;

import com.rometools.utils.Lists;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.*;
import org.junit.rules.*;
import org.junit.runner.*;

import javax.ejb.EJB;
import javax.inject.Inject;

import static org.junit.Assert.*;

@RunWith(Arquillian.class)
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

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
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
        thrown.expect(PermissionException.class);
        dashboardUsersService.revokeAllRolesForUser(user.getId());
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
        thrown.expect(PermissionException.class);
        dashboardUsersService.changeSuperuserStatus(user.getId());
    }

    @Test
    public void shouldChangeSuperuserStatus_grantSuperuserStatus_withPermissionException() {
        // given
        AuthenticatedUser user = authenticationServiceBean.findByID(3L);
        dataverseSession.setUser(authenticationServiceBean.findByID(4L));

        // when & then
        thrown.expect(PermissionException.class);
        dashboardUsersService.changeSuperuserStatus(user.getId());
    }
}
