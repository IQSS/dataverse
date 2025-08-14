package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import static edu.harvard.iq.dataverse.UserNotification.Type.*;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

public class NotificationsIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
        disableSendNotificationOnDatasetCreationSetting();
    }

    @AfterAll
    public static void afterClass() {
        disableSendNotificationOnDatasetCreationSetting();
    }

    @Test
    public void testNotifications() {
        // SendNotificationOnDatasetCreation setting is false

        Response createAuthor = UtilIT.createRandomUser();
        createAuthor.then().assertThat()
                .statusCode(OK.getStatusCode());
        String authorApiToken = UtilIT.getApiTokenFromResponse(createAuthor);

        Response noPermsUser = UtilIT.createRandomUser();
        noPermsUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String noPermsApiToken = UtilIT.getApiTokenFromResponse(noPermsUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(authorApiToken);
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, authorApiToken);
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Response getNotifications = UtilIT.getNotifications(authorApiToken);
        getNotifications.then().assertThat()
                .body("data.notifications[0].displayAsRead", equalTo(false))
                .body("data.notifications[1].displayAsRead", equalTo(false))
                .body("data.notifications.size()", equalTo(2))
                .statusCode(OK.getStatusCode());

        String firstNotificationType = JsonPath.from(getNotifications.body().asString()).getString("data.notifications[0].type");
        String secondNotificationType = JsonPath.from(getNotifications.body().asString()).getString("data.notifications[1].type");
        long createAccountId = 0L;
        if (firstNotificationType.equals(CREATEDV.toString())) {
            assertEquals(CREATEACC.toString(), secondNotificationType);
            createAccountId = JsonPath.from(getNotifications.getBody().asString()).getLong("data.notifications[1].id");
        } else if (firstNotificationType.equals(CREATEACC.toString())) {
            assertEquals(CREATEDV.toString(), secondNotificationType);
            createAccountId = JsonPath.from(getNotifications.getBody().asString()).getLong("data.notifications[0].id");
        } else {
            fail("Unexpected notification type: " + firstNotificationType);
        }

        Response unreadCount = UtilIT.getUnreadNotificationsCount(authorApiToken);
        unreadCount.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.unreadCount", equalTo(2));

        Response markReadNoPerms = UtilIT.markNotificationAsRead(createAccountId, noPermsApiToken);
        markReadNoPerms.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        Response markRead = UtilIT.markNotificationAsRead(createAccountId, authorApiToken);
        markRead.then().assertThat().statusCode(OK.getStatusCode());

        Response getNotifications2 = UtilIT.getNotifications(authorApiToken);
        getNotifications2.then().assertThat()
                .body("data.notifications.size()", equalTo(2))
                .statusCode(OK.getStatusCode());

        firstNotificationType = JsonPath.from(getNotifications2.body().asString()).getString("data.notifications[0].type");
        secondNotificationType = JsonPath.from(getNotifications2.body().asString()).getString("data.notifications[1].type");
        if (firstNotificationType.equals(CREATEDV.toString())) {
            assertEquals(CREATEACC.toString(), secondNotificationType);
            assertTrue(JsonPath.from(getNotifications2.body().asString()).getBoolean("data.notifications[1].displayAsRead"));
            assertFalse(JsonPath.from(getNotifications2.body().asString()).getBoolean("data.notifications[0].displayAsRead"));
        } else if (firstNotificationType.equals(CREATEACC.toString())) {
            assertEquals(CREATEDV.toString(), secondNotificationType);
            assertTrue(JsonPath.from(getNotifications2.body().asString()).getBoolean("data.notifications[0].displayAsRead"));
            assertFalse(JsonPath.from(getNotifications2.body().asString()).getBoolean("data.notifications[1].displayAsRead"));
        } else {
            fail("Unexpected notification type: " + firstNotificationType);
        }

        Response deleteNotificationNoPerms = UtilIT.deleteNotification(createAccountId, noPermsApiToken);
        deleteNotificationNoPerms.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        Response deleteNotification = UtilIT.deleteNotification(createAccountId, authorApiToken);
        deleteNotification.then().assertThat().statusCode(OK.getStatusCode());

        Response getNotifications3 = UtilIT.getNotifications(authorApiToken);
        getNotifications3.then().assertThat()
                .body("data.notifications[0].type", equalTo(CREATEDV.toString()))
                .body("data.notifications.size()", equalTo(1))
                .statusCode(OK.getStatusCode());

        // SendNotificationOnDatasetCreation setting is true

        createAuthor = UtilIT.createRandomUser();
        createAuthor.then().assertThat()
                .statusCode(OK.getStatusCode());
        authorApiToken = UtilIT.getApiTokenFromResponse(createAuthor);

        Response enableSendNotificationOnDatasetCreationSettingResponse = UtilIT.enableSetting(SettingsServiceBean.Key.SendNotificationOnDatasetCreation);
        enableSendNotificationOnDatasetCreationSettingResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        createDataverseResponse = UtilIT.createRandomDataverse(authorApiToken);
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, authorApiToken);
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        getNotifications = UtilIT.getNotifications(authorApiToken);
        getNotifications.then().assertThat()
                .body("data.notifications[0].displayAsRead", equalTo(false))
                .body("data.notifications[1].displayAsRead", equalTo(false))
                .body("data.notifications[2].displayAsRead", equalTo(false))
                .body("data.notifications.size()", equalTo(3))
                .statusCode(OK.getStatusCode());

        List<String> notificationTypes = JsonPath.from(getNotifications.body().asString()).getList("data.notifications.type");

        List<String> expectedTypes = Arrays.asList(CREATEACC.toString(), CREATEDV.toString(), DATASETCREATED.toString());

        assertTrue(notificationTypes.containsAll(expectedTypes) && expectedTypes.containsAll(notificationTypes));

        disableSendNotificationOnDatasetCreationSetting();

        // inAppNotificationFormat optional query parameter test cases

        // inAppNotificationFormat = false (default)

        createAuthor = UtilIT.createRandomUser();
        createAuthor.then().assertThat()
                .statusCode(OK.getStatusCode());
        authorApiToken = UtilIT.getApiTokenFromResponse(createAuthor);

        getNotifications = UtilIT.getNotifications(authorApiToken);
        getNotifications.then().assertThat()
                .body("data.notifications[0].displayAsRead", equalTo(false))
                .body("data.notifications.size()", equalTo(1))
                // In-App fields should be null
                .body("data.notifications[0].rootDataverseName", equalTo(null))
                .body("data.notifications[0].userGuidesBaseUrl", equalTo(null))
                .body("data.notifications[0].userGuidesVersion", equalTo(null))
                .body("data.notifications[0].userGuidesSectionPath", equalTo(null))
                // Email-related fields should be present
                .body("data.notifications[0].subjectText", equalTo("Root: Your account has been created"))
                .body("data.notifications[0].messageText", containsString("Hello,"))
                .statusCode(OK.getStatusCode());

        // inAppNotificationFormat = true

        createAuthor = UtilIT.createRandomUser();
        createAuthor.then().assertThat()
                .statusCode(OK.getStatusCode());
        authorApiToken = UtilIT.getApiTokenFromResponse(createAuthor);

        getNotifications = UtilIT.getNotifications(authorApiToken, true);
        getNotifications.then().assertThat()
                .body("data.notifications[0].displayAsRead", equalTo(false))
                .body("data.notifications.size()", equalTo(1))
                // In-App fields should be present
                .body("data.notifications[0].rootDataverseName", equalTo("Root"))
                .body("data.notifications[0].userGuidesBaseUrl", equalTo("https://guides.dataverse.org"))
                .body("data.notifications[0].userGuidesSectionPath", equalTo("user/index.html"))
                .body("data.notifications[0].userGuidesVersion", not(equalTo(null)))
                // Email-related fields should be null
                .body("data.notifications[0].subjectText", equalTo(null))
                .body("data.notifications[0].messageText", equalTo(null))
                .statusCode(OK.getStatusCode());
    }

    private static void disableSendNotificationOnDatasetCreationSetting() {
        Response disableSendNotificationOnDatasetCreationSettingResponse = UtilIT.deleteSetting(SettingsServiceBean.Key.SendNotificationOnDatasetCreation);
        disableSendNotificationOnDatasetCreationSettingResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
    }
}
