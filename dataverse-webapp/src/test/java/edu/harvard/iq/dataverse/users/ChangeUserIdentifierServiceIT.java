package edu.harvard.iq.dataverse.users;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class ChangeUserIdentifierServiceIT extends WebappArquillianDeployment {

    @Inject
    private ChangeUserIdentifierService changeUserIdentifierService;

    @Inject
    private DataverseSession dataverseSession;

    @EJB
    private AuthenticationServiceBean authenticationService;

    @EJB
    private BuiltinUserServiceBean builtinUserService;

    @Inject
    private DataverseRoleServiceBean roleService;

    @EJB
    private RoleAssigneeServiceBean roleAssigneeService;

    @EJB
    private DataverseRoleServiceBean dataverseRoleService;

    @EJB
    private DataverseDao dataverseDao;

    @Before
    public void setUp() {
        dataverseSession.setUser(authenticationService.getAdminUser());
    }

    @Test
    public void changeUserIdentifier() {
        // given
        String userIdBefore = "filedownloader";
        String userIdAfter = "changedUserIdentifier";
        RoleAssignment roleAssignment = new RoleAssignment(dataverseRoleService.find(2L),
                authenticationService.getAuthenticatedUser(userIdBefore),
                dataverseDao.find(67L), "privateUrl");
        roleService.save(roleAssignment);

        // when
        changeUserIdentifierService.changeUserIdentifier(authenticationService.getAdminUser(), userIdBefore, userIdAfter);

        //then
        assertNull(authenticationService.getAuthenticatedUser(userIdBefore));
        assertNotNull(authenticationService.getAuthenticatedUser(userIdAfter));
        assertNull(builtinUserService.findByUserName(userIdBefore));
        assertNotNull(builtinUserService.findByUserName(userIdAfter));

        AuthenticatedUser authenticatedUser = authenticationService.getAuthenticatedUser(userIdAfter);
        assertEquals(userIdAfter, authenticatedUser.getAuthenticatedUserLookup().getPersistentUserId());

        assertTrue(roleAssigneeService.getAssignmentsFor("@"+userIdBefore).isEmpty());
        assertEquals(3, roleAssigneeService.getAssignmentsFor("@"+userIdAfter).size());
    }
}
