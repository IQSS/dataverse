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
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.ejb.EJBException;
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

    @EJB
    private RoleAssigneeServiceBean roleAssigneeService;

    @EJB
    private DataverseRoleServiceBean dataverseRoleService;

    @EJB
    private DataverseDao dataverseDao;

    @Test
    public void changeUserIdentifier_notSuperuser() {
        // given
        dataverseSession.setUser(authenticationService.getAuthenticatedUser("filedownloader"));

        // when
        Exception exception = Assertions.assertThrows(EJBException.class, () -> {
            changeUserIdentifierService.changeUserIdentifier("oldId", "newId");
        });
        assertEquals("User is not authorized to call this method. Only superuser is allowed to do it.", exception.getCause().getMessage());
    }

    @Test
    public void changeUserIdentifier() {
        // given
        dataverseSession.setUser(authenticationService.getAdminUser());

        String userIdBefore = "filedownloader";
        String userIdAfter = "changedUserIdentifier";
        RoleAssignment roleAssignment = new RoleAssignment(dataverseRoleService.find(2L),
                authenticationService.getAuthenticatedUser(userIdBefore),
                dataverseDao.find(67L), "privateUrl");
        dataverseRoleService.save(roleAssignment);

        // when
        changeUserIdentifierService.changeUserIdentifier(userIdBefore, userIdAfter);

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
