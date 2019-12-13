package edu.harvard.iq.dataverse.mail;

import com.google.common.collect.Sets;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.GenericDao;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.common.BrandingUtil;
import edu.harvard.iq.dataverse.mail.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.dto.EmailNotificationDto;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.util.MailUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.Tuple2;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.simplejavamail.email.Recipient;

import javax.mail.internet.InternetAddress;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MailMessageCreatorTest {

    private MailMessageCreator mailMessageCreator;

    @Mock
    private SystemConfig systemConfig;

    @Mock
    private PermissionServiceBean permissionService;

    @Mock
    private DataverseDao dataverseDao;

    @Mock
    private ConfirmEmailServiceBean confirmEmailService;

    @Mock
    private GenericDao genericDao;

    @Mock
    private DataverseSession dataverseSession;

    private static String GUIDESBASEURL = "http://guides.dataverse.org";
    private static String GUIDESVERSION = "V8";
    private static String SITEURL = "http://localhost:8080";
    private static String ROOTDVNAME = "Root";
    private static String SYSTEMEMAIL = "test@icm.pl";

    private Dataverse testDataverse = createTestDataverse();

    @BeforeEach
    void prepare() {

        Dataverse rootDataverse = createRootDataverse(ROOTDVNAME);

        RoleAssignment roleAssignment = createRoleAssignment();

        Mockito.when(permissionService.getRolesOfUser(Mockito.any(),
                                                      Mockito.any(Dataverse.class))).thenReturn(Sets.newHashSet(
                roleAssignment));
        Mockito.when(dataverseDao.findRootDataverse()).thenReturn(rootDataverse);
        Mockito.when(dataverseDao.find(createDataverseEmailNotificationDto().getDvObjectId())).thenReturn(testDataverse);
        Mockito.when(systemConfig.getDataverseSiteUrl()).thenReturn(SITEURL);
        Mockito.when(systemConfig.getGuidesBaseUrl()).thenReturn(GUIDESBASEURL);
        Mockito.when(systemConfig.getGuidesVersion()).thenReturn(GUIDESVERSION);
        Mockito.when(dataverseSession.getUser()).thenReturn(new AuthenticatedUser());

        mailMessageCreator = new MailMessageCreator(systemConfig,
                                                    permissionService,
                                                    dataverseDao,
                                                    confirmEmailService,
                                                    genericDao,
                                                    dataverseSession);
    }

    @Test
    public void createMailFooterMessage() {
        //given
        InternetAddress systemEmail = MailUtil.parseSystemAddress(SYSTEMEMAIL);
        String messageText = "Nice message";

        //when
        String footerMessage = mailMessageCreator.createMailFooterMessage(Locale.ENGLISH, ROOTDVNAME, systemEmail);

        //then
        Assert.assertEquals(getFooterMessage(), footerMessage);

    }

    @Test
    public void createRecipients() {
        //given
        String emailRecipients = "mietek@icm.pl,janusz@icm.pl,zdzichu@icm.pl";

        //when
        List<Recipient> recipients = mailMessageCreator.createRecipients(emailRecipients, StringUtils.EMPTY);

        List<String> recipientsEmails = recipients.stream()
                .map(Recipient::getAddress)
                .collect(Collectors.toList());

        //then
        Assert.assertTrue(recipientsEmails.contains("mietek@icm.pl"));
        Assert.assertTrue(recipientsEmails.contains("janusz@icm.pl"));
        Assert.assertTrue(recipientsEmails.contains("zdzichu@icm.pl"));

    }

    @Test
    public void getMessageAndSubject_ForCreateDataverse() {
        //given
        EmailNotificationDto testEmailNotificationDto = createDataverseEmailNotificationDto();

        //when
        Tuple2<String, String> messageAndSubject = mailMessageCreator.getMessageAndSubject(testEmailNotificationDto,
                                                                                           "test@icm.pl");

        //then
        Assert.assertEquals(getCreateDataverseMessage(), messageAndSubject._1);
        Assert.assertEquals(getCreateDataverseSubject(), messageAndSubject._2);
    }

    @Test
    public void getMessageAndSubject_ForCreateDataverse_WithDifferentLocale() {
        //given
        AuthenticatedUser userFromDifferentCountry = new AuthenticatedUser();
        userFromDifferentCountry.setNotificationsLanguage(Locale.forLanguageTag("pl"));

        EmailNotificationDto testEmailNotificationDto = new EmailNotificationDto(1L,
                                                                                 "useremail@test.com",
                                                                                 NotificationType.CREATEDV,
                                                                                 1L,
                                                                                 NotificationObjectType.DATAVERSE,
                                                                                 userFromDifferentCountry);

        //when
        Tuple2<String, String> messageAndSubject = mailMessageCreator.getMessageAndSubject(testEmailNotificationDto,
                                                                                           "test@icm.pl");

        //then
        Assert.assertEquals(getPolishCreateDataverseMessage(), messageAndSubject._1);
        Assert.assertEquals(getPolishCreateDataverseSubject(), messageAndSubject._2);
    }

    @Test
    public void getMessageAndSubject_ForCreateDataverse_WrongArgument() {
        //given
        EmailNotificationDto testEmailNotificationDto = createIncorrectNotificationDto();

        //when
        Tuple2<String, String> messageAndSubject = mailMessageCreator.getMessageAndSubject(testEmailNotificationDto,
                                                                                           "test@icm.pl");

        //then
        Assert.assertEquals(StringUtils.EMPTY, messageAndSubject._1);
        Assert.assertEquals(getCreateDataverseSubject(), messageAndSubject._2);
    }

    @Test
    public void getMessageAndSubject_ForAssignRole() {
        //given
        EmailNotificationDto testEmailNotificationDto = createAssignRoleEmailNotificationDto();

        //when
        Tuple2<String, String> messageAndSubject = mailMessageCreator.getMessageAndSubject(testEmailNotificationDto,
                                                                                           "test@icm.pl");

        //then
        String ADMIN = "admin";
        Assert.assertEquals(getAssignRoleMessage(ADMIN, "dataverse"), messageAndSubject._1);
        Assert.assertEquals(getAssignRoleSubject(), messageAndSubject._2);
    }

    private String getFooterMessage() {
        return "\n\nYou may contact us for support at " + SYSTEMEMAIL + ".\n\nThank you,\n" +
                BrandingUtil.getSupportTeamName(MailUtil.parseSystemAddress(SYSTEMEMAIL), ROOTDVNAME);
    }

    private String getAssignRoleMessage(String role, String dvObjectType) {
        return "Hello, \n" +
                "You are now " + role + " for the " + dvObjectType +
                " \"" + testDataverse.getDisplayName() + "\" (view at " + SITEURL + "/dataverse/" + testDataverse.getAlias() + ").";
    }

    private String getCreateDataverseMessage() {
        return "Hello, \n" +
                "Your new dataverse named " + testDataverse.getDisplayName() + " (view at " + SITEURL + "/dataverse/" + testDataverse.getAlias()
                + ") was created in  (view at )." +
                " To learn more about what you can do with your dataverse, check out the Dataverse Management" +
                " - User Guide at " + GUIDESBASEURL + "/" + GUIDESVERSION + "/user/dataverse-management.html .";
    }

    private String getPolishCreateDataverseMessage() {
        return  "Witaj, \n" +
                "Twoja nowa kolekcja o nazwie " + testDataverse.getDisplayName() + " (zobacz na stronie " + SITEURL +"/dataverse/"+ testDataverse.getAlias()+ ") została utworzona" +
                " w  (zobacz na stronie ). Aby dowiedzieć się więcej, co można zrobić z kolekcją, zapoznaj się z" +
                " rozdziałem Zarządzanie kolekcją w Poradniku użytkownika" +
                " na stronie " + GUIDESBASEURL + "/" + GUIDESVERSION + "/user/dataverse-management.html.";
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

        return dataverse;
    }

    private EmailNotificationDto createDataverseEmailNotificationDto() {
        return new EmailNotificationDto(1L,
                                        "useremail@test.com",
                                        NotificationType.CREATEDV,
                                        1L,
                                        NotificationObjectType.DATAVERSE,
                                        new AuthenticatedUser());
    }

    private EmailNotificationDto createIncorrectNotificationDto() {
        return new EmailNotificationDto(1L,
                                        "useremail@test.com",
                                        NotificationType.CREATEDV,
                                        1L,
                                        NotificationObjectType.AUTHENTICATED_USER,
                                        new AuthenticatedUser());
    }

    private EmailNotificationDto createAssignRoleEmailNotificationDto() {
        return new EmailNotificationDto(1L,
                                        "useremail@test.com",
                                        NotificationType.ASSIGNROLE,
                                        1L,
                                        NotificationObjectType.DATAVERSE,
                                        new AuthenticatedUser());
    }

    private EmailNotificationDto createRequestFileAccessNotificationDto() {
        return new EmailNotificationDto(1L,
                                        "useremail@test.com",
                                        NotificationType.REQUESTFILEACCESS,
                                        1L,
                                        NotificationObjectType.DATAFILE,
                                        new AuthenticatedUser());
    }

    private Dataverse createRootDataverse(String rootdvname) {
        Dataverse rootDataverse = new Dataverse();
        rootDataverse.setName(rootdvname);
        return rootDataverse;
    }

    private RoleAssignment createRoleAssignment() {
        RoleAssignment roleAssignment = new RoleAssignment();
        DataverseRole dataverseRole = new DataverseRole();
        dataverseRole.setAlias(DataverseRole.ADMIN);
        roleAssignment.setRole(dataverseRole);
        return roleAssignment;
    }
}