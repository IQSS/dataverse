
package edu.harvard.iq.dataverse.api;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import edu.harvard.iq.dataverse.workflow.internalspi.COARNotifyRelationshipAnnouncementStep;
import static edu.harvard.iq.dataverse.workflow.internalspi.COARNotifyRelationshipAnnouncementStep.DATACITE_URI_PREFIX;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.UUID;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

@LocalJvmSettings
@JvmSetting(key = JvmSettings.LINKEDDATANOTIFICATION_ALLOWED_HOSTS, value = "*")
public class LDNInboxIT {

    private static String apiToken;
    private static String username;
    private static String dataverseAlias;
    private static String datasetPid;
    private static Integer datasetId;
    private static String superuserApiToken;
    private static String superusername;

    private static final String FRBR_SUPPLEMENT = "http://purl.org/vocab/frbr/core#supplement";

    static SettingsServiceBean settingsServiceBean = Mockito.mock(SettingsServiceBean.class);
    static DataverseServiceBean dataverseServiceBean = Mockito.mock(DataverseServiceBean.class);

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        // Create test user and get API token
        Response createUser = UtilIT.createRandomUser();
        apiToken = UtilIT.getApiTokenFromResponse(createUser);
        username = UtilIT.getUsernameFromResponse(createUser);

        // Create superuser for receiving notifications
        Response createSuperuser = UtilIT.createRandomUser();
        superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        superusername = UtilIT.getUsernameFromResponse(createSuperuser);
        Response makeSuperuser = UtilIT.setSuperuserStatus(superusername, true);
        makeSuperuser.then().assertThat().statusCode(OK.getStatusCode());

        // Create dataverse
        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.then().assertThat().statusCode(201);
        dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);

        // Create and publish a dataset
        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.then().assertThat().statusCode(201);
        datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        // Publish the dataset
        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverse.then().assertThat().statusCode(OK.getStatusCode());

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPid, "major", apiToken);
        publishDataset.then().assertThat().statusCode(OK.getStatusCode());

        // Setup mocks behavior, inject as deps - needed to build announcements
        Mockito.when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.InstallationName))
                .thenReturn("LDN IT Tester");
        BrandingUtil.injectServices(dataverseServiceBean, settingsServiceBean);
    }

    @AfterAll
    public static void afterClass() {
        // Clean up: remove test dataset and dataverse
        if (datasetId != null) {
            Response destroyDataset = UtilIT.destroyDataset(datasetId, apiToken);
            destroyDataset.then().assertThat().statusCode(OK.getStatusCode());
        }

        if (dataverseAlias != null) {
            Response deleteDataverse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
            deleteDataverse.then().assertThat().statusCode(OK.getStatusCode());
        }

        // Delete test users
        Response deleteUser = UtilIT.deleteUser(username);
        deleteUser.then().assertThat().statusCode(OK.getStatusCode());
        Response deleteSuperuser = UtilIT.deleteUser(superusername);
        deleteSuperuser.then().assertThat().statusCode(OK.getStatusCode());
    }

    @Test
    public void testAcceptRelationshipAnnouncementMessage() {
        // Create a COAR Notify Relationship Announcement message
        String citingResourceId = "https://doi.org/10.1234/example-publication";
        String relationship = DATACITE_URI_PREFIX + "Cites";

        String message = createRelationshipAnnouncementMessage(citingResourceId, datasetPid, relationship);

        // Send the message to the LDN inbox
        Response response = UtilIT.sendMessageToLDNInbox(message);

        // Verify the response
        response.then().assertThat().statusCode(OK.getStatusCode()).body("data.message", equalTo("Message Received"));

        // Wait a moment for notification to be created
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify that a notification was created for the superuser
        Response notifications = UtilIT.getNotifications(superuserApiToken);

        notifications.then().assertThat().statusCode(OK.getStatusCode()).body("data", notNullValue());

        // Check that at least one notification exists with type DATASETMENTIONED
        String notificationsJson = notifications.asString();
        assertTrue(notificationsJson.contains("DATASETMENTIONED"), "Expected to find DATASETMENTIONED notification");
        assertTrue(notificationsJson.contains(citingResourceId), "Expected notification to contain citing resource ID");
    }

    @Test
    @JvmSetting(key = JvmSettings.LINKEDDATANOTIFICATION_ALLOWED_HOSTS, value = "192.0.2.1")
    public void testRejectMessageFromNonWhitelistedHost() {
        String message = createRelationshipAnnouncementMessage("https://doi.org/10.1234/test", datasetPid,
                DATACITE_URI_PREFIX + "Cites");

        // Send the message - should be rejected
        Response response = UtilIT.sendMessageToLDNInbox(message);

        response.then().assertThat().statusCode(FORBIDDEN.getStatusCode());
    }

    @Test
    public void testRejectInvalidJsonLD() {
        String invalidJson = "{ this is not valid json }";

        Response response = UtilIT.sendMessageToLDNInbox(invalidJson);

        response.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testRejectMessageForNonExistentDataset() {
        String nonExistentPid = "doi:10.5072/FK2/NONEXISTENT";

        String message = createRelationshipAnnouncementMessage("https://doi.org/10.1234/test", nonExistentPid,
                DATACITE_URI_PREFIX + "Cites");

        Response response = UtilIT.sendMessageToLDNInbox(message);

        response.then().assertThat().statusCode(SERVICE_UNAVAILABLE.getStatusCode());
    }

    @Test
    public void testAcceptMessageWithMultipleRelationshipTypes() {
        // Test different relationship types - the ones supported by Dataverse and the
        // default from DSpace
        String[] relationships = { DATACITE_URI_PREFIX + "Cites", DATACITE_URI_PREFIX + "IsSupplementTo",
                DATACITE_URI_PREFIX + "IsReferencedBy", DATACITE_URI_PREFIX + "IsCitedBy",
                DATACITE_URI_PREFIX + "IsSupplementedBy", DATACITE_URI_PREFIX + "References", FRBR_SUPPLEMENT };

        for (String relationship : relationships) {
            String message = createRelationshipAnnouncementMessage(
                    "https://doi.org/10.1234/test-" + relationship.toLowerCase(), datasetPid, relationship);

            Response response = UtilIT.sendMessageToLDNInbox(message);

            response.then().assertThat().statusCode(OK.getStatusCode()).body("data.message",
                    equalTo("Message Received"));
        }
    }

    @Test
    public void testAcceptMessageWithUrn() {
        // Test with URN format identifier
        String urnPid = "urn:nbn:de:0000-12345";

        // Create a dataset with URN (in real scenario, this would be configured)
        // For this test, we'll use the existing dataset but reference it with a URN in
        // the message
        String message = createRelationshipAnnouncementMessage("https://doi.org/10.1234/test-urn", urnPid,
                DATACITE_URI_PREFIX + "Cites");

        Response response = UtilIT.sendMessageToLDNInbox(message);

        response.then().assertThat().statusCode(OK.getStatusCode());
    }

    @Test
    public void testAcceptMessageWithHandle() {
        // Test with Handle format identifier
        String handlePid = "hdl:1234.5/67890";

        String message = createRelationshipAnnouncementMessage("https://doi.org/10.1234/test-handle", handlePid,
                DATACITE_URI_PREFIX + "Cites");

        Response response = UtilIT.sendMessageToLDNInbox(message);

        response.then().assertThat().statusCode(OK.getStatusCode());
    }

    @Test
    public void testRejectMessageWithNonUriRelationship() {
        // Test with an unsupported relationship type
        String message = createRelationshipAnnouncementMessage("https://doi.org/10.1234/test-unsupported", datasetPid,
                "UnsupportedRelationType");

        Response response = UtilIT.sendMessageToLDNInbox(message);

        // Should be rejected
        response.then().assertThat().statusCode(BAD_REQUEST.getStatusCode());
    }

    /**
     * Helper method to create a COAR Notify Relationship Announcement message with
     * specific resource type
     */
    private String createRelationshipAnnouncementMessage(String citingResourceId, String targetDatasetPid,
            String relationship) {

        JsonObjectBuilder targetBuilder = Json.createObjectBuilder().add("id", RestAssured.baseURI)
                .add("inbox", RestAssured.baseURI + "/api/inbox").add("type", "Service");

        JsonObject rel = Json.createObjectBuilder().add("as:object", targetDatasetPid)
                .add("as:relationship", relationship).add("as:subject", citingResourceId)
                .add("id", "urn:uuid:" + UUID.randomUUID().toString()).add("type", "Relationship").build();

        return COARNotifyRelationshipAnnouncementStep.buildAnnouncement(rel, targetBuilder.build());
    }
}