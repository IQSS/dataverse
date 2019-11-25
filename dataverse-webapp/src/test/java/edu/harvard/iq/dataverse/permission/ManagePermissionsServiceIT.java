package edu.harvard.iq.dataverse.permission;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class ManagePermissionsServiceIT extends WebappArquillianDeployment {
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @Inject
    private ManagePermissionsService managePermissionsService;
    @Inject
    private DataverseDao dataverseDao;
    @Inject
    private DataverseSession dataverseSession;
    @EJB
    private AuthenticationServiceBean authenticationService;
    @Inject
    private DataverseRoleServiceBean roleService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        dataverseSession.setUser(authenticationService.getAdminUser());
    }

    @Test
    public void shouldAssignRoleWithNotification() {
        // given
        dataverseSession.setUser(authenticationService.findByID(1L));
        String userEmail = dataverseSession.getUser().getDisplayInfo().getEmailAddress();
        Dataverse dataverse = dataverseDao.find(1L);
        DataverseRole roleToBeAssigned = roleService.findBuiltinRoleByAlias("editor");

        // when
        managePermissionsService.assignRoleWithNotification(roleToBeAssigned, dataverseSession.getUser(), dataverse);

        // then
        TypedQuery<RoleAssignment> query = em.createNamedQuery(
                "RoleAssignment.listByAssigneeIdentifier_DefinitionPointId_RoleId",
                RoleAssignment.class);
        query.setParameter("assigneeIdentifier", dataverseSession.getUser().getIdentifier());
        query.setParameter("definitionPointId", dataverse.getId());
        query.setParameter("roleId", roleToBeAssigned.getId());
        List<RoleAssignment> roles = query.getResultList();

        assertEquals(1, roles.size());
        RoleAssignment role = roles.get(0);
        assertEquals(dataverseSession.getUser().getIdentifier(), role.getAssigneeIdentifier());
        assertEquals(dataverse.getId(), role.getDefinitionPoint().getId());
        assertEquals(roleToBeAssigned.getId(), role.getRole().getId());

        await()
                .atMost(Duration.ofSeconds(5L))
                .until(() -> smtpServer.mailBox().stream()
                        .anyMatch(emailModel -> emailModel.getTo().equals(userEmail)));

    }

    @Test
    public void shouldAssignRoleWithNotification_withPermissionsException() {
        // given
        Dataverse dataverse = dataverseDao.find(1L);
        DataverseRole roleToBeAssigned = roleService.findBuiltinRoleByAlias("editor");
        dataverseSession.setUser(GuestUser.get());

        // when&then
        thrown.expect(PermissionException.class);
        managePermissionsService.assignRoleWithNotification(roleToBeAssigned, dataverseSession.getUser(), dataverse);
    }

    @Test
    public void removeRoleAssignmentWithNotification() {
        // given
        Dataverse dataverse = dataverseDao.find(1L);
        String userEmail = dataverseSession.getUser().getDisplayInfo().getEmailAddress();
        RoleAssignment toBeRemoved = new RoleAssignment(roleService.findBuiltinRoleByAlias("editor"), dataverseSession.getUser(), dataverse, null);
        em.persist(toBeRemoved);
        em.flush();

        // when
        managePermissionsService.removeRoleAssignmentWithNotification(toBeRemoved);

        // then
        TypedQuery<RoleAssignment> query = em.createNamedQuery(
                "RoleAssignment.listByAssigneeIdentifier_DefinitionPointId_RoleId",
                RoleAssignment.class);
        query.setParameter("assigneeIdentifier", dataverseSession.getUser().getIdentifier());
        query.setParameter("definitionPointId", dataverse.getId());
        query.setParameter("roleId", roleService.findBuiltinRoleByAlias("editor").getId());
        List<RoleAssignment> roles = query.getResultList();
        assertEquals(0, roles.size());

        await()
                .atMost(Duration.ofSeconds(5L))
                .until(() -> smtpServer.mailBox().stream()
                        .anyMatch(emailModel -> emailModel.getTo().equals(userEmail)));
    }

    @Test
    public void removeRoleAssignmentWithNotification_withPermissionsException() {
        // given
        Dataverse dataverse = dataverseDao.find(1L);
        RoleAssignment toBeRemoved = new RoleAssignment(roleService.findBuiltinRoleByAlias("editor"), dataverseSession.getUser(), dataverse, null);
        em.persist(toBeRemoved);
        em.flush();

        dataverseSession.setUser(GuestUser.get());

        // when&then
        thrown.expect(PermissionException.class);
        managePermissionsService.removeRoleAssignmentWithNotification(toBeRemoved);
    }

    @Test
    public void shouldSaveOrUpdateRole() {
        // given
        Dataverse dataverse = dataverseDao.find(19L);
        DataverseRole toBeSaved = new DataverseRole();
        toBeSaved.setOwner(dataverse);
        toBeSaved.setName("newRoleName");
        toBeSaved.setAlias("newRoleAlias");
        toBeSaved.setDescription("newRoleDesc");

        // when
        managePermissionsService.saveOrUpdateRole(toBeSaved);

        // then
        DataverseRole dbRole = roleService.findCustomRoleByAliasAndOwner("newRoleAlias", dataverse.getId());
        assertTrue(dataverse.getRoles()
                .stream()
                .map(DataverseRole::getId)
                .collect(Collectors.toList())
                .contains(dbRole.getId()));
        assertTrue(dataverse.getRoles()
                .stream()
                .map(DataverseRole::getAlias)
                .collect(Collectors.toList())
                .contains("newRoleAlias"));
    }

    @Test
    public void shouldSaveOrUpdateRole_withPermissionsException() {
        // given
        Dataverse dataverse = dataverseDao.find(19L);
        DataverseRole toBeSaved = new DataverseRole();
        toBeSaved.setOwner(dataverse);
        toBeSaved.setName("newRoleName");
        toBeSaved.setAlias("newRoleAlias");
        toBeSaved.setDescription("newRoleDesc");

        dataverseSession.setUser(GuestUser.get());

        // when&then
        thrown.expect(PermissionException.class);
        managePermissionsService.saveOrUpdateRole(toBeSaved);;
    }

    @Test
    public void shouldSetDataverseDefaultContributorRole() {
        // given
        Dataverse dataverse = dataverseDao.find(19L);
        DataverseRole toBeSetDefault = new DataverseRole();
        toBeSetDefault.setOwner(dataverse);
        toBeSetDefault.setName("newRoleName");
        toBeSetDefault.setAlias("newRoleAlias");
        toBeSetDefault.setDescription("newRoleDesc");
        em.persist(toBeSetDefault);
        em.flush();

        // when
        managePermissionsService.setDataverseDefaultContributorRole(toBeSetDefault, dataverse);

        // then
        assertEquals(toBeSetDefault.getId(), dataverse.getDefaultContributorRole().getId());
        assertEquals(toBeSetDefault.getAlias(), dataverse.getDefaultContributorRole().getAlias());
    }

    @Test
    public void shouldSetDataverseDefaultContributorRole_withPermissionsException() {
        // given
        Dataverse dataverse = dataverseDao.find(19L);
        DataverseRole toBeSetDefault = new DataverseRole();
        toBeSetDefault.setOwner(dataverse);
        toBeSetDefault.setName("newRoleName");
        toBeSetDefault.setAlias("newRoleAlias");
        toBeSetDefault.setDescription("newRoleDesc");

        dataverseSession.setUser(GuestUser.get());

        // when&then
        thrown.expect(PermissionException.class);
        managePermissionsService.setDataverseDefaultContributorRole(toBeSetDefault, dataverse);;
    }
}
