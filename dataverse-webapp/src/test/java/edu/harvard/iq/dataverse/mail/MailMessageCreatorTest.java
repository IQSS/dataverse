package edu.harvard.iq.dataverse.mail;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.GenericDao;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.common.BrandingUtil;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.common.RoleTranslationUtil;
import edu.harvard.iq.dataverse.mail.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.NotificationParameter;
import edu.harvard.iq.dataverse.notification.dto.EmailNotificationDto;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.util.MailUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.Tuple2;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.simplejavamail.email.Recipient;

import javax.mail.internet.InternetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MailMessageCreatorTest {

    @Mock private SystemConfig systemConfig;
    @Mock private PermissionServiceBean permissionService;
    @Mock private DataverseDao dataverseDao;
    @Mock private ConfirmEmailServiceBean confirmEmailService;
    @Mock private GenericDao genericDao;
    @Mock private DataverseSession dataverseSession;
    @Mock private AuthenticatedUserRepository authenticatedUserRepository;

    @InjectMocks
    private MailMessageCreator mailMessageCreator;

    private final static String GUIDESBASEURL = "http://guides.dataverse.org";
    private final static String GUIDESVERSION = "V8";
    private final static String SITEURL = "http://localhost:8080";
    private final static String ROOTDVNAME = "Root";
    private final static String SYSTEMEMAIL = "test@icm.pl";

    private Dataverse testDataverse = createTestDataverse();
    private Dataset testDataset = createTestDataset();
    private DatasetVersion testDatasetVersion = createTestDatasetVersion();

    @BeforeEach
    void prepare() {
        Dataverse rootDataverse = createRootDataverse(ROOTDVNAME);

        RoleAssignment roleAssignment = createRoleAssignment();

        when(permissionService.getRolesOfUser(any(), any(Dataverse.class)))
                .thenReturn(Sets.newHashSet(roleAssignment));
        when(dataverseDao.findRootDataverse()).thenReturn(rootDataverse);
        when(dataverseDao.find(createDataverseEmailNotificationDto().getDvObjectId())).thenReturn(testDataverse);
        when(genericDao.find(createReturnToAuthorNotificationDto().getDvObjectId(), DatasetVersion.class)).thenReturn(testDatasetVersion);
        when(genericDao.find(createGrantFileAccessInfoNotificationDto().getDvObjectId(), Dataset.class)).thenReturn(testDataset);        when(systemConfig.getDataverseSiteUrl()).thenReturn(SITEURL);
        when(systemConfig.getGuidesBaseUrl(any(Locale.class))).thenReturn(GUIDESBASEURL);
        when(systemConfig.getGuidesVersion()).thenReturn(GUIDESVERSION);
        when(dataverseSession.getUser()).thenReturn(new AuthenticatedUser());
    }

    // -------------------- TESTS --------------------

    @Test
    void createMailFooterMessage() {
        // given
        InternetAddress systemEmail = MailUtil.parseSystemAddress(SYSTEMEMAIL);

        // when
        String footerMessage = mailMessageCreator.createMailFooterMessage( "notification.email.closing", Locale.ENGLISH, ROOTDVNAME, systemEmail);

        // then
        assertThat(footerMessage).isEqualTo(getFooterMessage());

    }

    @Test
    void createRecipients() {
        // given
        String emailRecipients = "mietek@icm.pl,janusz@icm.pl,zdzichu@icm.pl";

        // when
        List<Recipient> recipients = mailMessageCreator.createRecipients(emailRecipients, StringUtils.EMPTY);

        List<String> recipientsEmails = recipients.stream()
                .map(Recipient::getAddress)
                .collect(Collectors.toList());

        // then
        assertThat(recipientsEmails).containsExactlyInAnyOrder("mietek@icm.pl", "janusz@icm.pl", "zdzichu@icm.pl");
    }

    @Test
    void getMessageAndSubject_ForCreateDataverse() {
        // given
        EmailNotificationDto testEmailNotificationDto = createDataverseEmailNotificationDto();

        // when
        Tuple2<String, String> messageAndSubject = mailMessageCreator.getMessageAndSubject(testEmailNotificationDto,
                                                                                           "test@icm.pl");

        // then
        assertThat(messageAndSubject)
                .extracting(Tuple2::_1, Tuple2::_2)
                .containsExactly(getCreateDataverseMessage(), getCreateDataverseSubject());
    }

    @Test
    void getMessageAndSubject_ForCreateDataverse_WithDifferentLocale() {
        // given
        AuthenticatedUser userFromDifferentCountry = new AuthenticatedUser();
        userFromDifferentCountry.setNotificationsLanguage(Locale.forLanguageTag("pl"));

        EmailNotificationDto testEmailNotificationDto = new EmailNotificationDto(1L,
                "useremail@test.com", NotificationType.CREATEDV, 1L,
                NotificationObjectType.DATAVERSE, userFromDifferentCountry, Collections.emptyMap());

        // when
        Tuple2<String, String> messageAndSubject = mailMessageCreator.getMessageAndSubject(testEmailNotificationDto,
                                                                                           "test@icm.pl");

        // then
        assertThat(messageAndSubject)
                .extracting(Tuple2::_1, Tuple2::_2)
                .containsExactly(getPolishCreateDataverseMessage(), getPolishCreateDataverseSubject());
    }

    @Test
    void getMessageAndSubject_ForCreateDataverse_WrongArgument() {
        // given
        EmailNotificationDto testEmailNotificationDto = createIncorrectNotificationDto();

        // when
        Tuple2<String, String> messageAndSubject = mailMessageCreator.getMessageAndSubject(testEmailNotificationDto,
                                                                                           "test@icm.pl");

        // then
        assertThat(messageAndSubject)
                .extracting(Tuple2::_1, Tuple2::_2)
                .containsExactly(StringUtils.EMPTY, getCreateDataverseSubject());
    }

    @Test
    void getMessageAndSubject_ForAssignRole() {
        // given
        EmailNotificationDto testEmailNotificationDto = createAssignRoleEmailNotificationDto();

        // when
        Tuple2<String, String> messageAndSubject = mailMessageCreator.getMessageAndSubject(testEmailNotificationDto,
                                                                                           "test@icm.pl");

        // then
        assertThat(messageAndSubject)
                .extracting(Tuple2::_1, Tuple2::_2)
                .containsExactly(
                        getAssignRoleMessage(RoleTranslationUtil.getLocaleNameFromAlias("admin"), "dataverse"),
                        getAssignRoleSubject());
    }

    @Test
    void getMessageAndSubject_ForDatasetVersion_ReturnToAuthor() {
        // given
        EmailNotificationDto notificationDto = createReturnToAuthorNotificationDto();

        // when
        Tuple2<String, String> messageAndSubject = mailMessageCreator.getMessageAndSubject(notificationDto,
                "test@icm.pl");

        // then
        assertThat(messageAndSubject)
                .extracting(Tuple2::_1, Tuple2::_2)
                .containsExactly(getReturnToAuthorMessage(), "Root: Your dataset has been returned");
    }

    @Test
    void getMessageAndSubject_ForDatasetVersion_SubmitForReviewWithMessage() {
        // given
        AuthenticatedUser requestor = MocksFactory.makeAuthenticatedUser("Notifcation", "Requester");
        when(authenticatedUserRepository.findById(requestor.getId())).thenReturn(Optional.of(requestor));
        EmailNotificationDto testEmailNotificationDto = createSubmitForReviewNotificationDto(requestor);

        // when
        Tuple2<String, String> messageAndSubject = mailMessageCreator.getMessageAndSubject(testEmailNotificationDto, "system@email.com");

        // then
        assertThat(messageAndSubject)
                .extracting(Tuple2::_1, Tuple2::_2)
                .containsExactly(getSubmitForReviewMessage(), "Root: Your dataset has been submitted for review");
    }

    @Test
    void getMessageAndSubject_ForDataset_GrantFileAccessInfo() {
        // given
        EmailNotificationDto notificationDto = createGrantFileAccessInfoNotificationDto();

        // when
        Tuple2<String, String> messageAndSubject = mailMessageCreator.getMessageAndSubject(notificationDto, "system@email.com");

        // then
        assertThat(messageAndSubject)
                .extracting(Tuple2::_1, Tuple2::_2)
                .containsExactly(getGrantFileAccessInfoMessage(), "Root: Request for access to restricted files has been accepted");
    }

    @Test
    void getMessageAndSubject_ForDataset_RejectFileAccessInfo() {
        // given
        EmailNotificationDto notificationDto = createRejectFileAccessInfoNotificationDto();

        // when
        Tuple2<String, String> messageAndSubject = mailMessageCreator.getMessageAndSubject(notificationDto, "system@email.com");

        // then
        assertThat(messageAndSubject)
                .extracting(Tuple2::_1, Tuple2::_2)
                .containsExactly(getRejectFileAccessInfoMessage(), "Root: Request for access to restricted files has been rejected");
    }

    // -------------------- PRIVATE --------------------

    private String getFooterMessage() {
        return "\n\nYou may contact us for support at " + SYSTEMEMAIL + ".\n\nThank you,\n" +
                BrandingUtil.getSupportTeamName(MailUtil.parseSystemAddress(SYSTEMEMAIL), ROOTDVNAME, Locale.ENGLISH);
    }

    private String getAssignRoleMessage(String role, String dvObjectType) {
        return "Hello, \n\n" +
                "You are now " + role + " for the " + dvObjectType +
                " \"" + testDataverse.getDisplayName() + "\" (view at " + SITEURL + "/dataverse/" + testDataverse.getAlias() + ").";
    }

    private String getCreateDataverseMessage() {
        return "Hello, \n\n" +
                "Your new dataverse named " + testDataverse.getDisplayName() + " (view at " + SITEURL + "/dataverse/" + testDataverse.getAlias()
                + ") was created in  (view at )." +
                " To learn more about what you can do with your dataverse, check out the Dataverse Management" +
                " - User Guide at " + GUIDESBASEURL + "/" + GUIDESVERSION + "/user/dataverse-management.html .";
    }

    private String getPolishCreateDataverseMessage() {
        return  "Witaj, \n\n" +
                "Twoja nowa kolekcja o nazwie " + testDataverse.getDisplayName() + " (zobacz na stronie " + SITEURL +"/dataverse/"+ testDataverse.getAlias()+ ") została utworzona" +
                " w  (zobacz na stronie ). Aby dowiedzieć się więcej, co można zrobić z kolekcją, zapoznaj się z" +
                " rozdziałem Zarządzanie kolekcją w Poradniku użytkownika" +
                " na stronie " + GUIDESBASEURL + "/" + GUIDESVERSION + "/user/dataverse-management.html.";
    }

    private String getReturnToAuthorMessage() {
        return "Hello, \n\n" +
                "Dataset \"TheTitle\" (view at http://localhost:8080/dataset.xhtml?persistentId=&version=DRAFT) was " +
                "returned by the data reviewer of \"rootDataverseName\" (view at http://localhost:8080/dataverse/nicedataverse).\n\n" +
                "Additional information from the dataset reviewer:\n\n" +
                "=====\n\n" +
                "Dataset returned to author message\n\n" +
                "=====\n\n" +
                "To make contact about the review of this dataset, please write to verifier@test.com or simply reply to this message.";
    }

    private String getSubmitForReviewMessage() {
        return "Hello, \n\n"
                + "TheTitle (view at http://localhost:8080/dataset.xhtml?persistentId=&version=DRAFT) was submitted for review to be published in "
                + "rootDataverseName (view at http://localhost:8080/dataverse/nicedataverse). "
                + "Don't forget to publish it or send it back to the contributor, Notifcation Requester (Notifcation.Requester@someU.edu)!\n\n"
                + "Additional information\n\nContributors message for curator";
    }

    private String getGrantFileAccessInfoMessage() {
        return "Hello, \n\n"
                + "dataverseAdmin accepted access request to files in dataset “testDataset” "
                + "(view at http://localhost:8080/dataset.xhtml?persistentId=doi:10.5072/FK2/TEST11).\n\n"
                + "Current number of users awaiting file access request resolution in this dataset: 0.\n\n"
                + "You may manage access to files in this dataset at http://localhost:8080/permissions-manage-files.xhtml?id=2";
    }

    private String getRejectFileAccessInfoMessage() {
        return "Hello, \n\n"
                + "dataverseAdmin rejected access request to files in dataset “testDataset” "
                + "(view at http://localhost:8080/dataset.xhtml?persistentId=doi:10.5072/FK2/TEST11).\n\n"
                + "Current number of users awaiting file access request resolution in this dataset: 0.\n\n"
                + "You may manage access to files in this dataset at http://localhost:8080/permissions-manage-files.xhtml?id=2";
    }

    private String getAssignRoleSubject() {
        return "Root: You have been assigned a role";
    }

    private String getCreateDataverseSubject() {
        return "Root: Your dataverse has been created";
    }

    private String getPolishCreateDataverseSubject() {
        return "Root: Twoja kolekcja została utworzona";
    }

    private Dataverse createTestDataverse() {
        Dataverse dataverse = createRootDataverse("NICE DATAVERSE");
        dataverse.setAlias("nicedataverse");
        dataverse.setName("rootDataverseName");

        return dataverse;
    }

    private EmailNotificationDto createDataverseEmailNotificationDto() {
        return new EmailNotificationDto(1L,
                                        "useremail@test.com",
                                        NotificationType.CREATEDV,
                                        1L,
                                        NotificationObjectType.DATAVERSE,
                                        new AuthenticatedUser(),
                                        Collections.emptyMap());
    }

    private EmailNotificationDto createIncorrectNotificationDto() {
        return new EmailNotificationDto(1L,
                                        "useremail@test.com",
                                        NotificationType.CREATEDV,
                                        1L,
                                        NotificationObjectType.AUTHENTICATED_USER,
                                        new AuthenticatedUser(),
                                        Collections.emptyMap());
    }

    private EmailNotificationDto createAssignRoleEmailNotificationDto() {
        return new EmailNotificationDto(1L,
                                        "useremail@test.com",
                                        NotificationType.ASSIGNROLE,
                                        1L,
                                        NotificationObjectType.DATAVERSE,
                                        new AuthenticatedUser(),
                                        Collections.emptyMap());
    }

    private EmailNotificationDto createRequestFileAccessNotificationDto() {
        return new EmailNotificationDto(1L,
                                        "useremail@test.com",
                                        NotificationType.REQUESTFILEACCESS,
                                        1L,
                                        NotificationObjectType.DATAFILE,
                                        new AuthenticatedUser(),
                                        Collections.emptyMap());
    }

    private EmailNotificationDto createGrantFileAccessInfoNotificationDto() {
        return new EmailNotificationDto(1L,
                "usermail@test.com",
                NotificationType.GRANTFILEACCESSINFO,
                2L,
                NotificationObjectType.DATASET,
                new AuthenticatedUser(),
                Collections.singletonMap(NotificationParameter.GRANTED_BY.key(), "dataverseAdmin"));
    }

    private EmailNotificationDto createRejectFileAccessInfoNotificationDto() {
        return new EmailNotificationDto(1L,
                "usermail@test.com",
                NotificationType.REJECTFILEACCESSINFO,
                2L,
                NotificationObjectType.DATASET,
                new AuthenticatedUser(),
                Collections.singletonMap(NotificationParameter.REJECTED_BY.key(), "dataverseAdmin"));
    }

    private EmailNotificationDto createReturnToAuthorNotificationDto() {
        return new EmailNotificationDto(1L, "useremail@test.com", NotificationType.RETURNEDDS,
                3L, NotificationObjectType.DATASET_VERSION, MocksFactory.makeAuthenticatedUser("Notification", "Reciever"),
                createParametersMap(NotificationParameter.MESSAGE.key(), "Dataset returned to author message",
                        NotificationParameter.REPLY_TO.key(), "verifier@test.com"));
    }

    private EmailNotificationDto createSubmitForReviewNotificationDto(AuthenticatedUser requestor) {
        return new EmailNotificationDto(1L,
                "useremail@test.com",
                NotificationType.SUBMITTEDDS,
                3L,
                NotificationObjectType.DATASET_VERSION,
                MocksFactory.makeAuthenticatedUser("Jurek","Kiler"),
                createParametersMap(
                        NotificationParameter.REQUESTOR_ID.key(), String.valueOf(requestor.getId()),
                        NotificationParameter.MESSAGE.key(), "Contributors message for curator"));
    }

    private Dataverse createRootDataverse(String rootdvname) {
        Dataverse rootDataverse = new Dataverse();
        rootDataverse.setName(rootdvname);
        return rootDataverse;
    }

    private RoleAssignment createRoleAssignment() {
        RoleAssignment roleAssignment = new RoleAssignment();
        DataverseRole dataverseRole = new DataverseRole();
        dataverseRole.setAlias(BuiltInRole.ADMIN.getAlias());
        roleAssignment.setRole(dataverseRole);
        return roleAssignment;
    }

    private Dataset createTestDataset() {
        Dataverse dataverse = createTestDataverse();
        dataverse.setId(1L);

        Dataset dataset = new Dataset() {
            @Override public String getDisplayName() { return "testDataset"; }
        };
        dataset.setId(2L);
        dataset.setOwner(dataverse);
        dataset.setGlobalId(new GlobalId("doi:10.5072/FK2/TEST11"));
        return dataset;
    }

    private DatasetVersion createTestDatasetVersion() {
        Dataverse dataverse = createTestDataverse();
        dataverse.setId(1L);

        Dataset dataset = new Dataset();
        dataset.setId(2L);
        dataset.setOwner(dataverse);

        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setId(3L);
        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);

        DatasetFieldType datasetFieldType = new DatasetFieldType();
        datasetFieldType.setName(DatasetFieldConstant.title);
        datasetFieldType.setChildDatasetFieldTypes(Collections.emptyList());


        DatasetField datasetField = DatasetField.createNewEmptyDatasetField(datasetFieldType, datasetVersion);
        datasetField.setValue("TheTitle");

        datasetVersion.setDatasetFields(Lists.newArrayList(datasetField));
        datasetVersion.setDataset(dataset);
        dataset.setVersions(Lists.newArrayList(datasetVersion));

        return datasetVersion;
    }

    private <K, V> Map<K, V> createParametersMap(K key1, V value1, K key2, V value2) {
        Map<K, V> parameters = new HashMap<>();
        parameters.put(key1, value1);
        parameters.put(key2, value2);
        return parameters;
    }
}