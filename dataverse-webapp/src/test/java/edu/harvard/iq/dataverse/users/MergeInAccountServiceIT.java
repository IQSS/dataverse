package edu.harvard.iq.dataverse.users;
import edu.harvard.iq.dataverse.AcceptedConsentDao;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.GenericDao;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.OAuthTokenDataDao;
import edu.harvard.iq.dataverse.authorization.groups.ExplicitGroupDao;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2TokenDataServiceBean;
import edu.harvard.iq.dataverse.datafile.FileAccessRequestDao;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.guestbook.GuestbookResponseServiceBean;
import edu.harvard.iq.dataverse.mail.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.notification.NotificationParameter;
import edu.harvard.iq.dataverse.notification.NotificationParametersUtil;
import edu.harvard.iq.dataverse.notification.UserNotificationService;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.consent.AcceptedConsent;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionUser;
import edu.harvard.iq.dataverse.persistence.dataverse.link.SavedSearch;
import edu.harvard.iq.dataverse.persistence.group.ExplicitGroup;
import edu.harvard.iq.dataverse.persistence.guestbook.GuestbookResponse;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserLookup;
import edu.harvard.iq.dataverse.persistence.user.BuiltinUser;
import edu.harvard.iq.dataverse.persistence.user.ConfirmEmailData;
import edu.harvard.iq.dataverse.persistence.user.OAuth2TokenData;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;
import edu.harvard.iq.dataverse.persistence.user.UserNotificationRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowComment;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchServiceBean;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.testcontainers.shaded.com.google.common.collect.Lists;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class MergeInAccountServiceIT extends WebappArquillianDeployment {

    @Inject private MergeInAccountService mergeInAccountService;
    @Inject private DataverseSession dataverseSession;
    @EJB private AuthenticationServiceBean authenticationService;
    @EJB private RoleAssigneeServiceBean roleAssigneeService;
    @EJB private DatasetDao datasetDao;
    @EJB private DvObjectServiceBean dvObjectService;
    @EJB private GuestbookResponseServiceBean guestbookResponseService;
    @EJB private UserNotificationRepository userNotificationRepository;
    @EJB private SavedSearchServiceBean savedSearchService;
    @EJB private BuiltinUserServiceBean builtinUserService;
    @EJB private OAuth2TokenDataServiceBean oAuth2TokenDataService;
    @EJB private DataverseDao dataverseDao;
    @EJB private ExplicitGroupServiceBean explicitGroupService;
    @EJB private DataFileServiceBean dataFileService;
    @EJB private DatasetVersionServiceBean datasetVersionService;
    @EJB private DataverseRoleServiceBean dataverseRoleService;
    @EJB private ConfirmEmailServiceBean confirmEmailService;
    @EJB private GenericDao genericDao;
    @EJB private FileAccessRequestDao fileAccessRequestDao;
    @EJB private ExplicitGroupDao explicitGroupDao;
    @EJB private AcceptedConsentDao acceptedConsentDao;
    @EJB private OAuthTokenDataDao oAuthTokenDataDao;
    @Inject private UserNotificationService userNotificationService;
    private NotificationParametersUtil notificationParametersUtil = new NotificationParametersUtil();

    @PersistenceContext(unitName = "VDCNet-ejbPU") private EntityManager entityManager;

    @Test
    public void changeUserIdentifier_notSuperuser() {
        // given
        dataverseSession.setUser(authenticationService.getAuthenticatedUser("filedownloader"));

        // when
        Exception exception = Assertions.assertThrows(EJBException.class, () -> {
            mergeInAccountService.mergeAccounts("toBeConsumedUser", "toBeMergedTo");
        });
        assertEquals("User is not authorized to call this method. Only superuser is allowed to do it.", exception.getCause().getMessage());
    }

    @Test
    public void mergeAccounts() {
        // given
        dataverseSession.setUser(authenticationService.getAdminUser());

        createUserWithObjects("toBeConsumedUser", "test1@mail.com", "test1-api-token");
        createUserWithoutObjects("toBeMergedTo", "test2@mail.com", "test2-api-token");
        entityManager.flush();
        AuthenticatedUser consumed = authenticationService.getAuthenticatedUser("toBeConsumedUser");
        AuthenticatedUser base = authenticationService.getAuthenticatedUser("toBeMergedTo");
        assertNotNull(consumed.getId());
        assertNotNull(base.getId());

        // when
        mergeInAccountService.mergeAccounts("toBeConsumedUser", "toBeMergedTo");
        entityManager.flush();

        // then
        // CONSUMED
        assertNull(authenticationService.getAuthenticatedUser(consumed.getUserIdentifier()));
        assertNull(builtinUserService.findByUserName(consumed.getUserIdentifier()));
        assertNull(entityManager.find(AuthenticatedUserLookup.class, consumed.getAuthenticatedUserLookup().getId()));
        assertNull(authenticationService.findApiTokenByUser(consumed));
        assertNull(oAuth2TokenDataService.get(consumed.getId(), "test-provider").orElse(null));
        assertNull(confirmEmailService.findSingleConfirmEmailDataByUser(consumed));

        assertEquals(0, entityManager.createNativeQuery("SELECT ac FROM acceptedconsent ac" +
                " WHERE user_id=" + consumed.getId()).getResultList().size());

        assertEquals(0, explicitGroupService.findDirectlyContainingGroups(consumed).size());
        assertEquals(0, entityManager.createNativeQuery("SELECT far FROM fileaccessrequests far" +
                " WHERE authenticated_user_id=" + consumed.getId()).getResultList().size());

        assertThat(authenticationService.getWorkflowCommentsByAuthenticatedUser(consumed)).hasSize(0);
        assertThat(savedSearchService.findByAuthenticatedUser(consumed)).hasSize(0);
        assertThat(userNotificationRepository.findByUser(consumed.getId())).hasSize(0);
        assertThat(guestbookResponseService.findByAuthenticatedUserId(consumed)).hasSize(0);
        assertThat(dvObjectService.findByAuthenticatedUserId(consumed)).hasSize(0);
        assertThat(datasetDao.getDatasetLocksByUser(consumed)).hasSize(0);
        assertThat(datasetDao.getDatasetVersionUsersByAuthenticatedUser(consumed)).hasSize(0);
        assertThat(roleAssigneeService.getAssignmentsFor(consumed.getIdentifier())).hasSize(0);

        // BASE
        assertNotNull(authenticationService.getAuthenticatedUser(base.getUserIdentifier()));
        assertNotNull(builtinUserService.findByUserName(base.getUserIdentifier()));
        assertNotNull(entityManager.find(AuthenticatedUserLookup.class, base.getAuthenticatedUserLookup().getId()));
        assertNotNull(authenticationService.findApiTokenByUser(base));
        assertNotNull(oAuth2TokenDataService.get(base.getId(), "test-provider").orElse(null));
        assertNotNull(confirmEmailService.findSingleConfirmEmailDataByUser(base));

        assertEquals(1, entityManager.createNativeQuery("SELECT ac FROM acceptedconsent ac" +
                " WHERE user_id=" + base.getId()).getResultList().size());

        assertEquals(1, explicitGroupService.findDirectlyContainingGroups(base).size());
        assertEquals(1, entityManager.createNativeQuery("SELECT far FROM fileaccessrequests far" +
                " WHERE authenticated_user_id=" + base.getId()).getResultList().size());

        assertThat(authenticationService.getWorkflowCommentsByAuthenticatedUser(base)).hasSize(1);
        assertThat(savedSearchService.findByAuthenticatedUser(base)).hasSize(1);
        assertThat(userNotificationRepository.findByUser(base.getId())).hasSize(1);
        assertThat(guestbookResponseService.findByAuthenticatedUserId(base)).hasSize(1);
        assertThat(dvObjectService.findByAuthenticatedUserId(base)).hasSize(1);
        assertThat(datasetDao.getDatasetLocksByUser(base)).hasSize(1);
        assertThat(datasetDao.getDatasetVersionUsersByAuthenticatedUser(base)).hasSize(1);
        assertThat(roleAssigneeService.getAssignmentsFor(base.getIdentifier())).hasSize(1);
    }

    private void createUserWithObjects(String userIdentifier, String uniqueMail, String uniqueApiToken) {
        AuthenticatedUser authenticatedUser = createUserWithoutObjects(userIdentifier, uniqueMail, uniqueApiToken);

        addObjectsToUser(authenticatedUser);
    }

    private void addObjectsToUser(AuthenticatedUser authenticatedUser) {
        AcceptedConsent acceptedConsent = new AcceptedConsent("testName", Locale.ENGLISH, "test", false, authenticatedUser);
        entityManager.persist(acceptedConsent);

        ExplicitGroup explicitGroup = new ExplicitGroup();
        explicitGroup.setDisplayName("testDisplayName");
        explicitGroup.setGroupAliasInOwner("testGroupAliasOwner");
        explicitGroup.setOwner(dataverseDao.find(51L));
        explicitGroup.add(authenticatedUser);
        explicitGroupService.persist(explicitGroup);

        DataFile dataFile = new DataFile();
        dataFile.setRootDataFileId(53L);
        dataFile.setChecksumType(DataFile.ChecksumType.MD5);
        dataFile.setChecksumValue("test-checksumeVal");
        dataFile.setContentType("app/test");
        dataFile.setFileAccessRequesters(Lists.newArrayList(authenticatedUser));
        dataFile.setCreateDate(new Timestamp(new Date().getTime()));
        dataFile.setModificationTime(new Timestamp(new Date().getTime()));
        dataFileService.save(dataFile);

        WorkflowComment workflowComment =
                new WorkflowComment(datasetVersionService.getById(41L), WorkflowComment.Type.RETURN_TO_AUTHOR,
                        "test message", authenticatedUser);
        entityManager.persist(workflowComment);

        SavedSearch savedSearch = new SavedSearch("test query", dataverseDao.find(51L), authenticatedUser);
        savedSearchService.save(savedSearch);

        UserNotification userNotification = new UserNotification();
        userNotification.setUser(authenticatedUser);
        userNotification.setType("testType");
        userNotificationRepository.save(userNotification);

        UserNotification userNotificationRequestor = new UserNotification();
        userNotificationRequestor.setUser(authenticationService.findByID(3L));

        Map<String, String> parameters = new HashMap<>();
        parameters.put(NotificationParameter.REQUESTOR_ID.key(), authenticatedUser.getId().toString());
        notificationParametersUtil.setParameters(userNotification, parameters);

        userNotificationRequestor.setType("testType");
        userNotificationRepository.save(userNotificationRequestor);

        GuestbookResponse guestbookResponse = guestbookResponseService.findById(1L);
        guestbookResponse.setAuthenticatedUser(authenticatedUser);
        entityManager.merge(guestbookResponse);

        DvObject dvObject = dvObjectService.findDvObject(67L);
        dvObject.setCreator(authenticatedUser);
        dvObject.setReleaseUser(authenticatedUser);
        entityManager.merge(dvObject);

        DatasetLock datasetLock = new DatasetLock(DatasetLock.Reason.InReview, authenticatedUser);
        datasetLock.setDataset(datasetDao.find(66L));
        entityManager.persist(datasetLock);

        DatasetVersionUser datasetVersionUser = entityManager.find(DatasetVersionUser.class, 41L);
        datasetVersionUser.setAuthenticatedUser(authenticatedUser);
        entityManager.merge(datasetVersionUser);

        RoleAssignment roleAssignment = new RoleAssignment(dataverseRoleService.find(2L),
                authenticatedUser,
                dataverseDao.find(67L), "privateUrl");
        dataverseRoleService.save(roleAssignment);
    }

    private AuthenticatedUser createUserWithoutObjects(String userIdentifier, String uniqueMail, String uniqueApiToken) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setUserIdentifier(userIdentifier);
        authenticatedUser.setCreatedTime(new Timestamp(new Date().getTime()));
        authenticatedUser.setEmail(uniqueMail);
        authenticatedUser.setFirstName("testFirstName");
        authenticatedUser.setLastName("testLastName");
        authenticatedUser.setNotificationsLanguage(Locale.ENGLISH);
        authenticationService.save(authenticatedUser);

        AuthenticatedUserLookup authenticatedUserLookup =
                new AuthenticatedUserLookup("test", authenticatedUser.getIdentifier(), authenticatedUser);
        entityManager.persist(authenticatedUserLookup);

        authenticatedUser.setAuthenticatedUserLookup(authenticatedUserLookup);

        BuiltinUser builtinUser = new BuiltinUser();
        builtinUser.setUserName(userIdentifier);
        builtinUserService.save(builtinUser);

        ApiToken apiToken = new ApiToken();
        apiToken.setAuthenticatedUser(authenticatedUser);
        apiToken.setTokenString(uniqueApiToken);
        apiToken.setCreateTime(new Timestamp(1L));
        apiToken.setExpireTime(new Timestamp(2L));
        entityManager.persist(apiToken);

        OAuth2TokenData oAuth2TokenData = new OAuth2TokenData();
        oAuth2TokenData.setAccessToken("test-token");
        oAuth2TokenData.setUser(authenticatedUser);
        oAuth2TokenData.setOauthProviderId("test-provider");
        oAuth2TokenDataService.store(oAuth2TokenData);

        ConfirmEmailData confirmEmailData = new ConfirmEmailData(authenticatedUser, 1);
        entityManager.persist(confirmEmailData);
        return authenticatedUser;
    }
}
