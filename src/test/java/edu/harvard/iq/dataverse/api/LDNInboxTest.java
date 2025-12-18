package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.api.ldn.COARNotifyRelationshipAnnouncement;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@LocalJvmSettings
public class LDNInboxTest {

    @InjectMocks
    private LDNInbox ldnInbox;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private DatasetServiceBean datasetService;
    @Mock
    private UserNotificationServiceBean userNotificationService;
    @Mock
    private DataverseRoleServiceBean roleService;
    @Mock
    private RoleAssigneeServiceBean roleAssigneeService;
    @Mock
    private SettingsServiceBean settingsService;
    @Mock
    private DataverseServiceBean dataverseService;

    
    private static final String DATACITE_URI_PREFIX = "http://purl.org/spar/datacite/";
    private static final String FRBR_SUPPLEMENT = "http://purl.org/vocab/frbr/core#supplement";
    private static final String TEST_REMOTE_IP = "127.0.0.1";
    private static final String datasetPid = "doi:10.5072/FK2/TESTID";
    GlobalId pid = new GlobalId("doi", "10.5072", "FK2/TESTID", "/", "https://doi.org/", "test");

    @BeforeEach
    public void setUp() {

        // Inject the mocked request into the API bean
        ldnInbox.httpRequest = httpRequest;
        when(httpRequest.getRemoteAddr()).thenReturn(TEST_REMOTE_IP);
    }

    @Test
    @JvmSetting(key = JvmSettings.LINKEDDATANOTIFICATION_ALLOWED_HOSTS, value = "192.168.1.100")
    public void testAcceptMessage_ForbiddenIp() {
        // given
        String remoteIp = "10.0.0.5";
        when(httpRequest.getRemoteAddr()).thenReturn(remoteIp);
        String messageBody = createRelationshipAnnouncementMessage("https://doi.org/10.1234/test", datasetPid,
                DATACITE_URI_PREFIX + "Cites");

        // when & then
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            ldnInbox.acceptMessage(messageBody);
        });

        assertEquals("The LDN Inbox does not accept messages from this address", exception.getMessage());
        verify(datasetService, never()).findByGlobalId(anyString());
    }

    @Test
    @JvmSetting(key = JvmSettings.LINKEDDATANOTIFICATION_ALLOWED_HOSTS, value = "192.168.1.100,10.0.0.5")
    public void testAcceptMessage_AllowedIp() {
        // given
        String remoteIp = "10.0.0.5";
        when(httpRequest.getRemoteAddr()).thenReturn(remoteIp);
        // This is a valid JSON-LD but will cause a BadRequestException later, which is
        // fine.
        // It proves we got past the ForbiddenException check.
        String messageBody = "{\"@context\": \"https://www.w3.org/ns/activitystreams\"}";

        // when & then
        // We expect a BadRequestException because the message is incomplete,
        // but this proves the IP was allowed.
        assertThrows(BadRequestException.class, () -> {
            ldnInbox.acceptMessage(messageBody);
        }, "Should throw BadRequest, not Forbidden, when IP is in the whitelist.");
    }

    @Test
    @JvmSetting(key = JvmSettings.LINKEDDATANOTIFICATION_ALLOWED_HOSTS, value = "*")
    public void testAcceptMessage_AllowedIp_Wildcard() {
        // given
        String remoteIp = "10.0.0.5";
        when(httpRequest.getRemoteAddr()).thenReturn(remoteIp);
        String messageBody = "{\"@context\": \"https://www.w3.org/ns/activitystreams\"}";

        // when & then
        // We expect a BadRequestException because the message is incomplete,
        // but this proves the wildcard allowed the request to proceed.
        assertThrows(jakarta.ws.rs.BadRequestException.class, () -> {
            ldnInbox.acceptMessage(messageBody);
        }, "Should throw BadRequest, not Forbidden, when whitelist is a wildcard.");
    }

    @Test
    @JvmSetting(key = JvmSettings.LINKEDDATANOTIFICATION_ALLOWED_HOSTS, value = TEST_REMOTE_IP)
    public void testAcceptRelationshipAnnouncementMessage() {
        // given
        String citingResourceId = "https://doi.org/10.1234/example-publication";
        String relationship = DATACITE_URI_PREFIX + "Cites";
        String message = createRelationshipAnnouncementMessage(citingResourceId, datasetPid, relationship);

        Dataset targetDataset = new Dataset();
        targetDataset.setId(1L);
        AuthenticatedUser testUser = mock(AuthenticatedUser.class);
        RoleAssignment testRoleAssignment = mock(RoleAssignment.class);
        DataverseRole testRole = mock(DataverseRole.class);
        RoleAssignee testRoleAssignee = mock(RoleAssignee.class);

        try (MockedStatic<PidUtil> pidUtilMock = Mockito.mockStatic(PidUtil.class)) {

            pidUtilMock.when(() -> PidUtil.parseAsGlobalID(datasetPid)).thenReturn(pid);
            when(datasetService.findByGlobalId(datasetPid)).thenReturn(targetDataset);

            // Mocks to ensure a user is found to be notified
            when(roleService.rolesAssignments(targetDataset)).thenReturn(Collections.singleton(testRoleAssignment));
            when(testRoleAssignment.getRole()).thenReturn(testRole);
            when(testRole.permissions()).thenReturn(Collections.singleton(Permission.PublishDataset));
            when(testRoleAssignment.getAssigneeIdentifier()).thenReturn("testAssignee");
            when(roleAssigneeService.getRoleAssignee("testAssignee")).thenReturn(testRoleAssignee);
            when(roleAssigneeService.getExplicitUsers(testRoleAssignee)).thenReturn(Collections.singletonList(testUser));

            // Setup mocks for BrandingUtil
            BrandingUtil.injectServices(dataverseService, settingsService);

            // when
            Response response = ldnInbox.acceptMessage(message);

            // then
            assertEquals(OK.getStatusCode(), response.getStatus());
            ArgumentCaptor<UserNotification.Type> typeCaptor = ArgumentCaptor.forClass(UserNotification.Type.class);
            ArgumentCaptor<Long> objectIdCaptor = ArgumentCaptor.forClass(Long.class);
            verify(userNotificationService).sendNotification(any(), any(), typeCaptor.capture(),
                    objectIdCaptor.capture(), any(), any(), anyBoolean(), anyString());
            assertEquals(UserNotification.Type.DATASETMENTIONED, typeCaptor.getValue());
            assertTrue(objectIdCaptor.getValue().equals(targetDataset.getId()));
        }
    }

    @Test
    @JvmSetting(key = JvmSettings.LINKEDDATANOTIFICATION_ALLOWED_HOSTS, value = TEST_REMOTE_IP)
    public void testAcceptRelationshipAnnouncementMessage_IsSupplementTo() {
        // given
        String citingResourceId = "https://doi.org/10.1234/example-publication";
        String relationship = FRBR_SUPPLEMENT;
        String message = createRelationshipAnnouncementMessage(citingResourceId, datasetPid, relationship);

        Dataset targetDataset = new Dataset();
        try (MockedStatic<PidUtil> pidUtilMock = Mockito.mockStatic(PidUtil.class)) {

            pidUtilMock.when(() -> PidUtil.parseAsGlobalID(datasetPid)).thenReturn(pid);

            when(datasetService.findByGlobalId(datasetPid)).thenReturn(targetDataset);
            // Setup mocks for BrandingUtil
            BrandingUtil.injectServices(dataverseService, settingsService);

            // when
            Response response = ldnInbox.acceptMessage(message);

            // then
            assertEquals(OK.getStatusCode(), response.getStatus());
        }
    }

    @Test
    @JvmSetting(key = JvmSettings.LINKEDDATANOTIFICATION_ALLOWED_HOSTS, value = TEST_REMOTE_IP)
    public void testAcceptMessage_NoDatasetFound() {
        // given
        String citingResourceId = "https://doi.org/10.1234/example-publication";
        String relationship = DATACITE_URI_PREFIX + "Cites";
        String message = createRelationshipAnnouncementMessage(citingResourceId, datasetPid, relationship);

        try (MockedStatic<PidUtil> pidUtilMock = Mockito.mockStatic(PidUtil.class)) {

            pidUtilMock.when(() -> PidUtil.parseAsGlobalID(datasetPid)).thenReturn(pid);
            when(datasetService.findByGlobalId(datasetPid)).thenReturn(null);

            // when
            Response response = ldnInbox.acceptMessage(message);

            // then
            assertEquals(OK.getStatusCode(), response.getStatus());
            verify(userNotificationService, never()).sendNotification(any(), any(),any(), anyLong(), any(),any(), anyBoolean(), any());
        }
    }

    @Test
    @JvmSetting(key = JvmSettings.LINKEDDATANOTIFICATION_ALLOWED_HOSTS, value = TEST_REMOTE_IP)
    public void testAcceptMessage_InvalidJsonLD() {
        // given
        String invalidMessage = "{\"@context\": \"https://www.w3.org/ns/activitystreams\", \"@id\": \"urn:uuid:invalid\"}";

        // when & then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            ldnInbox.acceptMessage(invalidMessage);
        });

        assertTrue(exception.getMessage().contains("Message does not contain an 'object' key"));
    }

    static String createRelationshipAnnouncementMessage(String sourceId, String targetId, String relationship) {
        JsonArray context = Json.createArrayBuilder().add("https://www.w3.org/ns/activitystreams")
                .add("https://purl.org/coar/notify").build();

        JsonArray type = Json.createArrayBuilder().add("Announce").add("coar-notify:RelationshipAction").build();

        JsonObject message = Json.createObjectBuilder().add("@context", context)
                .add("id", "urn:uuid:" + UUID.randomUUID().toString()).add("type", type)
                .add("origin", Json.createObjectBuilder().add("id", "https://some-service.com").add("type", "Service"))
                .add("target", Json.createObjectBuilder().add("id", "https://dataverse.org").add("type", "Service"))
                .add("object",
                        Json.createObjectBuilder().add("id", "urn:uuid:" + UUID.randomUUID().toString())
                                .add("type", "Relationship").add("as:relationship", relationship)
                                .add("as:subject", sourceId).add("as:object", targetId))
                .add("actor", Json.createObjectBuilder().add("id", "https://some-service.com").add("type", "Service")
                        .add("name", "Some Service"))
                .build();
        return message.toString();
    }
}