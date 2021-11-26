package edu.harvard.iq.dataverse.datafile;

import com.github.sleroy.fakesmtp.model.EmailModel;
import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.mail.FakeSmtpServerUtil;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.group.Group;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;
import edu.harvard.iq.dataverse.persistence.user.UserNotificationRepository;
import org.apache.commons.lang3.StringUtils;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author madryk
 */
@RunWith(Arquillian.class)
public class FilePermissionsServiceIT extends WebappArquillianDeployment {

    @Inject
    private FilePermissionsService filePermissionsService;

    @Inject
    private DataFileServiceBean dataFileService;
    @Inject
    private AuthenticationServiceBean authenticationService;
    @Inject
    private GroupServiceBean groupService;
    @Inject
    private DataverseSession dataverseSession;
    @Inject
    private RoleAssigneeServiceBean roleAssigneeService;
    @Inject
    private UserNotificationRepository userNotificationRepository;


    @Before
    public void initBefore() {
        dataverseSession.setUser(authenticationService.getAuthenticatedUser("superuser"));
    }


    // -------------------- TESTS --------------------

    @Test
    public void assignFileDownloadRole() {
        // given
        DataFile datafile1 = dataFileService.find(53L);
        DataFile datafile2 = dataFileService.find(55L);
        AuthenticatedUser user = authenticationService.getAuthenticatedUser("superuser");
        int userNotificationsCountBefore = userNotificationRepository.findByUser(user.getId()).size();

        Group group = groupService.getGroup("explicit/1-rootgroup");
        AuthenticatedUser groupMember = authenticationService.getAuthenticatedUser("rootGroupMember");
        int groupMemberNotificationsCountBefore = userNotificationRepository.findByUser(groupMember.getId()).size();

        AuthenticatedUser userToBeInformed = authenticationService.getAuthenticatedUser("dataverseAdmin");
        int userToBeInformedNotificationsCountBefore = userNotificationRepository.findByUser(userToBeInformed.getId()).size();

        datafile1.getFileAccessRequesters().add(user);
        datafile1 = dataFileService.save(datafile1);

        // when
        List<RoleAssignment> roleAssignments = filePermissionsService.assignFileDownloadRole(
                Lists.newArrayList(user, group),
                Lists.newArrayList(datafile1, datafile2));

        // then
        assertEquals(4, roleAssignments.size());

        assertContainsRoleAssignment(roleAssignments, "@superuser", 53L);
        assertContainsRoleAssignment(roleAssignments, "@superuser", 55L);

        assertContainsRoleAssignment(roleAssignments, "&explicit/1-rootgroup", 53L);
        assertContainsRoleAssignment(roleAssignments, "&explicit/1-rootgroup", 55L);

        assertThat(dataFileService.find(53L).getFileAccessRequesters(), is(empty()));
        assertThat(dataFileService.find(55L).getFileAccessRequesters(), is(empty()));


        EmailModel userEmail = FakeSmtpServerUtil.waitForEmailSentTo(smtpServer, "superuser@mailinator.com");
        assertEquals("Root: You have been granted access to a restricted file", userEmail.getSubject());

        List<UserNotification> userNotifications = userNotificationRepository.findByUser(user.getId());
        assertEquals(1 + userNotificationsCountBefore, userNotifications.size());
        assertUserNotification(userNotifications.get(0), NotificationType.GRANTFILEACCESS, 52L, true);

        EmailModel groupMemberEmail = FakeSmtpServerUtil.waitForEmailSentTo(smtpServer, "groupmember@mailinator.com");
        assertEquals("Root: You have been granted access to a restricted file", groupMemberEmail.getSubject());

        List<UserNotification> groupMemberNotifications = userNotificationRepository.findByUser(groupMember.getId());
        assertEquals(1 + groupMemberNotificationsCountBefore, groupMemberNotifications.size());
        assertUserNotification(groupMemberNotifications.get(0), NotificationType.GRANTFILEACCESS, 52L, true);

        EmailModel infoMail = FakeSmtpServerUtil.waitForEmailSentTo(smtpServer, userToBeInformed.getEmail());
        assertEquals("Root: Request for access to restricted files has been accepted", infoMail.getSubject());

        List<UserNotification> userToBeInformedNotifications = userNotificationRepository.findByUser(userToBeInformed.getId());
        assertEquals(1 + userToBeInformedNotificationsCountBefore, userToBeInformedNotifications.size());
        assertUserNotification(userToBeInformedNotifications.get(0), NotificationType.GRANTFILEACCESSINFO, 52L, true);
    }

    @Test
    @Transactional(TransactionMode.ROLLBACK)
    public void assignFileDownloadRole__MISSING_MANAGE_DATASET_PERMISSIONS() {
        // given
        dataverseSession.setUser(GuestUser.get());
        AuthenticatedUser user = authenticationService.getAuthenticatedUser("superuser");
        DataFile datafile = dataFileService.find(53L);

        // when
        Executable assignRoleOperation = () -> filePermissionsService.assignFileDownloadRole(
                Lists.newArrayList(user),
                Lists.newArrayList(datafile));

        // then
        assertThrows(PermissionException.class, assignRoleOperation);
    }


    @Test
    @Transactional(TransactionMode.ROLLBACK)
    public void revokeFileDownloadRole() {
        // given
        DataFile datafile1 = dataFileService.find(53L);
        DataFile datafile2 = dataFileService.find(55L);
        AuthenticatedUser user = authenticationService.getAuthenticatedUser("filedownloader");

        // when
        List<RoleAssignment> removedRoleAssignments = filePermissionsService.revokeFileDownloadRole(
                Lists.newArrayList(user),
                Lists.newArrayList(datafile1, datafile2));

        // then
        assertEquals(2, removedRoleAssignments.size());

        assertContainsRoleAssignment(removedRoleAssignments, "@filedownloader", 53L);
        assertContainsRoleAssignment(removedRoleAssignments, "@filedownloader", 55L);

        assertThat(roleAssigneeService.getAssignmentsFor("@filedownloader"), is(empty()));
    }


    @Test
    @Transactional(TransactionMode.ROLLBACK)
    public void revokeFileDownloadRole__MISSING_MANAGE_DATASET_PERMISSIONS() {
        // given
        dataverseSession.setUser(GuestUser.get());
        DataFile datafile = dataFileService.find(53L);
        AuthenticatedUser user = authenticationService.getAuthenticatedUser("filedownloader");

        // when
        Executable revokeRoleOperation = () -> filePermissionsService.revokeFileDownloadRole(Lists.newArrayList(user), Lists.newArrayList(datafile));

        // then
        assertThrows(PermissionException.class, revokeRoleOperation);
    }


    @Test
    public void rejectRequestAccessToFiles() {
        // given
        AuthenticatedUser user = authenticationService.getAuthenticatedUser("superuser");
        int userNotificationsCountBefore = userNotificationRepository.findByUser(user.getId()).size();

        AuthenticatedUser userToBeInformed = authenticationService.getAuthenticatedUser("dataverseAdmin");
        int userToBeInformedNotificationsCountBefore = userNotificationRepository.findByUser(userToBeInformed.getId()).size();

        DataFile datafile1 = dataFileService.find(53L);
        datafile1.getFileAccessRequesters().add(user);
        datafile1 = dataFileService.save(datafile1);

        DataFile datafile2 = dataFileService.find(55L);
        datafile2.getFileAccessRequesters().add(user);
        datafile2 = dataFileService.save(datafile2);

        // when
        filePermissionsService.rejectRequestAccessToFiles(user, Lists.newArrayList(datafile1, datafile2));

        // then
        assertThat(dataFileService.find(53L).getFileAccessRequesters(), is(empty()));
        assertThat(dataFileService.find(55L).getFileAccessRequesters(), is(empty()));

        EmailModel userEmail = FakeSmtpServerUtil.waitForEmailSentTo(smtpServer, "superuser@mailinator.com");
        assertEquals("Root: Your request for access to a restricted file has been", userEmail.getSubject());

        List<UserNotification> userNotifications = userNotificationRepository.findByUser(user.getId());
        assertEquals(1 + userNotificationsCountBefore, userNotifications.size());
        assertUserNotification(userNotifications.get(0), NotificationType.REJECTFILEACCESS, 52L, true);

        EmailModel infoMail = FakeSmtpServerUtil.waitForEmailSentTo(smtpServer, userToBeInformed.getEmail());
        assertEquals("Root: Request for access to restricted files has been rejected", infoMail.getSubject());

        List<UserNotification> userToBeInformedNotifications = userNotificationRepository.findByUser(userToBeInformed.getId());
        assertEquals(1 + userToBeInformedNotificationsCountBefore, userToBeInformedNotifications.size());
        assertUserNotification(userToBeInformedNotifications.get(0), NotificationType.REJECTFILEACCESSINFO, 52L, true);
    }


    // -------------------- PRIVATE --------------------

    private void assertContainsRoleAssignment(List<RoleAssignment> actualRoleAssignments,
            String expectedAssigneeId, long expectedFileId) {

        Optional<RoleAssignment> actualRoleAssignment = actualRoleAssignments.stream()
            .filter(assignment -> StringUtils.equals(assignment.getAssigneeIdentifier(), expectedAssigneeId))
            .filter(assignment -> assignment.getDefinitionPoint().getId().longValue() == expectedFileId)
            .findAny();

        assertTrue(actualRoleAssignment.isPresent());
        assertNotNull(actualRoleAssignment.get().getId());
        assertNull(actualRoleAssignment.get().getPrivateUrlToken());
        assertEquals("fileDownloader", actualRoleAssignment.get().getRole().getAlias());

    }

    private void assertUserNotification(UserNotification actualNotification,
            String expectedType, long expectedObjectId, boolean expectedEmailed) {

        assertEquals(expectedType, actualNotification.getType());
        assertEquals(Long.valueOf(expectedObjectId), actualNotification.getObjectId());
        assertEquals(expectedEmailed, actualNotification.isEmailed());
    }
}
