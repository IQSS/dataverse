
package edu.harvard.iq.dataverse.api;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.UUID;


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
        String citingResourceName = "An Example Publication Citing the Dataset";
        String relationship = "Cites";

        JsonObject message = createRelationshipAnnouncementMessage(
                citingResourceId,
                citingResourceName,
                datasetPid,
                relationship
        );

        // Send the message to the LDN inbox
        Response response = UtilIT.sendMessageToLDNInbox(message.toString());

        // Verify the response
        response.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", equalTo("Message Received"));

        // Wait a moment for notification to be created
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify that a notification was created for the superuser
        Response notifications = UtilIT.getNotifications(superuserApiToken);

        notifications.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data", notNullValue());

        // Check that at least one notification exists with type DATASETMENTIONED
        String notificationsJson = notifications.asString();
        assertTrue(notificationsJson.contains("DATASETMENTIONED"),
                "Expected to find DATASETMENTIONED notification");
        assertTrue(notificationsJson.contains(citingResourceId),
                "Expected notification to contain citing resource ID");
    }

    @Test
    @JvmSetting(key = JvmSettings.LINKEDDATANOTIFICATION_ALLOWED_HOSTS, value = "192.0.2.1")
    public void testRejectMessageFromNonWhitelistedHost() {

        JsonObject message = createRelationshipAnnouncementMessage(
                "https://doi.org/10.1234/test",
                "Test Publication",
                datasetPid,
                "Cites"
        );

        // Send the message - should be rejected
        Response response = UtilIT.sendMessageToLDNInbox(message.toString());

        response.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode());

        // Restore whitelist
        Response restoreWhitelist = UtilIT.setSetting(
                SettingsServiceBean.Key.LDNMessageHosts,
                "*"
        );
        restoreWhitelist.then().assertThat().statusCode(OK.getStatusCode());
    }

    @Test
    public void testRejectInvalidJsonLD() {
        String invalidJson = "{ this is not valid json }";

        Response response = UtilIT.sendMessageToLDNInbox(invalidJson);

        response.then().assertThat()
                .statusCode(400); // Bad Request
    }

    @Test
    public void testRejectMessageForNonExistentDataset() {
        String nonExistentPid = "doi:10.5072/FK2/NONEXISTENT";

        JsonObject message = createRelationshipAnnouncementMessage(
                "https://doi.org/10.1234/test",
                "Test Publication",
                nonExistentPid,
                "Cites"
        );

        Response response = UtilIT.sendMessageToLDNInbox(message.toString());

        response.then().assertThat()
                .statusCode(SERVICE_UNAVAILABLE.getStatusCode());
    }

    @Test
    public void testAcceptMessageWithMultipleRelationshipTypes() {
        // Test different relationship types
        String[] relationships = {
                "Cites",
                "IsSupplementTo",
                "IsReferencedBy",
                "IsRelatedTo"
        };

        for (String relationship : relationships) {
            JsonObject message = createRelationshipAnnouncementMessage(
                    "https://doi.org/10.1234/test-" + relationship.toLowerCase(),
                    "Test Publication for " + relationship,
                    datasetPid,
                    relationship
            );

            Response response = UtilIT.sendMessageToLDNInbox(message.toString());

            response.then().assertThat()
                    .statusCode(OK.getStatusCode())
                    .body("data.message", equalTo("Message Received"));
        }
    }

    @Test
    public void testAcceptMessageWithDifferentResourceTypes() {
        String[] resourceTypes = {
                "ScholarlyArticle",
                "Dataset",
                "Software",
                "Preprint"
        };

        for (String resourceType : resourceTypes) {
            JsonObject message = createRelationshipAnnouncementMessageWithType(
                    "https://doi.org/10.1234/test-" + resourceType.toLowerCase(),
                    "Test Resource of type " + resourceType,
                    datasetPid,
                    "Cites",
                    resourceType
            );

            Response response = UtilIT.sendMessageToLDNInbox(message.toString());

            response.then().assertThat()
                    .statusCode(OK.getStatusCode())
                    .body("data.message", equalTo("Message Received"));
        }
    }

    /**
     * Helper method to create a COAR Notify Relationship Announcement message
     */
    private JsonObject createRelationshipAnnouncementMessage(
            String citingResourceId,
            String citingResourceName,
            String citedDatasetPid,
            String relationship) {
        return createRelationshipAnnouncementMessageWithType(
                citingResourceId,
                citingResourceName,
                citedDatasetPid,
                relationship,
                "ScholarlyArticle"
        );
    }

    /**
     * Helper method to create a COAR Notify Relationship Announcement message with specific resource type
     */
    private JsonObject createRelationshipAnnouncementMessageWithType(
            String citingResourceId,
            String citingResourceName,
            String citedDatasetPid,
            String relationship,
            String resourceType) {

        String messageId = "urn:uuid:" + UUID.randomUUID().toString();
        String relationshipId = "urn:uuid:" + UUID.randomUUID().toString();

        // Convert PID to URL format if needed
        String citedDatasetUrl = citedDatasetPid;
        if (citedDatasetPid.startsWith("doi:")) {
            citedDatasetUrl = "https://doi.org/" + citedDatasetPid.substring(4);
        } else if (citedDatasetPid.startsWith("hdl:")) {
            citedDatasetUrl = "https://hdl.handle.net/" +

                    citedDatasetPid.substring(4);
        }

        // Build the COAR Notify message following the specification
        JsonObjectBuilder messageBuilder = Json.createObjectBuilder();

        // Add @context
        messageBuilder.add("@context", Json.createArrayBuilder()
                .add("https://www.w3.org/ns/activitystreams")
                .add("https://purl.org/coar/notify"));

        // Add message id and type
        messageBuilder.add("id", messageId);
        messageBuilder.add("type", Json.createArrayBuilder()
                .add("Announce")
                .add("coar-notify:RelationshipAction"));

        // Add actor (the system sending the notification)
        messageBuilder.add("actor", Json.createObjectBuilder()
                .add("id", "https://example.org/repository")
                .add("name", "Example Repository")
                .add("type", "Service"));

        // Add origin (inbox of the sender)
        messageBuilder.add("origin", Json.createObjectBuilder()
                .add("id", "https://example.org/repository")
                .add("inbox", "https://example.org/inbox")
                .add("type", "Service"));

        // Add target (inbox of the receiver - this Dataverse instance)
        messageBuilder.add("target", Json.createObjectBuilder()
                .add("id", RestAssured.baseURI)
                .add("inbox", RestAssured.baseURI + "/api/inbox")
                .add("type", "Service"));

        // Add object (the relationship being announced)
        messageBuilder.add("object", Json.createObjectBuilder()
                .add("id", relationshipId)
                .add("type", "Relationship")
                .add("as:subject", citingResourceId)
                .add("as:relationship", "https://purl.org/datacite/ontology#" + relationship)
                .add("as:object", citedDatasetUrl));

        // Add context (the citing resource details)
        messageBuilder.add("context", Json.createObjectBuilder()
                .add("id", citingResourceId)
                .add("ietf:cite-as", citingResourceId)
                .add("type", "sorg:" + resourceType)
                .add("sorg:name", citingResourceName));

        return messageBuilder.build();
    }
}

