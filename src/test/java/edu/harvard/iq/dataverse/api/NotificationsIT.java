package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationsIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testNotifications() {
        // SendNotificationOnDatasetCreation setting is false
        Response disableSendNotificationOnDatasetCreationSettingResponse = UtilIT.deleteSetting(SettingsServiceBean.Key.SendNotificationOnDatasetCreation);
        disableSendNotificationOnDatasetCreationSettingResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createAuthor = UtilIT.createRandomUser();
        createAuthor.then().assertThat()
                .statusCode(OK.getStatusCode());
        String authorApiToken = UtilIT.getApiTokenFromResponse(createAuthor);

        Response noPermsUser = UtilIT.createRandomUser();
        noPermsUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String noPermsApiToken = UtilIT.getApiTokenFromResponse(noPermsUser);

        // Some API calls don't generate a notification: https://github.com/IQSS/dataverse/issues/1342
        Response createDataverseResponse = UtilIT.createRandomDataverse(authorApiToken);
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        // Some API calls don't generate a notification: https://github.com/IQSS/dataverse/issues/1342
        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, authorApiToken);
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Response getNotifications = UtilIT.getNotifications(authorApiToken);
        getNotifications.then().assertThat()
                .body("data.notifications[0].type", equalTo("CREATEACC"))
                .body("data.notifications[0].displayAsRead", equalTo(false))
                .body("data.notifications[1]", equalTo(null))
                .statusCode(OK.getStatusCode());

        Response unreadCount = UtilIT.getUnreadNotificationsCount(authorApiToken);
        unreadCount.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.unreadCount", equalTo(1));

        long createAccountId = JsonPath.from(getNotifications.getBody().asString()).getLong("data.notifications[0].id");

        Response markReadNoPerms = UtilIT.markNotificationAsRead(createAccountId, noPermsApiToken);
        markReadNoPerms.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        Response markRead = UtilIT.markNotificationAsRead(createAccountId, authorApiToken);
        markRead.then().assertThat().statusCode(OK.getStatusCode());

        Response getNotifications2 = UtilIT.getNotifications(authorApiToken);
        getNotifications2.then().assertThat()
                .body("data.notifications[0].type", equalTo("CREATEACC"))
                .body("data.notifications[0].displayAsRead", equalTo(true))
                .body("data.notifications[1]", equalTo(null))
                .statusCode(OK.getStatusCode());

        Response deleteNotificationNoPerms = UtilIT.deleteNotification(createAccountId, noPermsApiToken);
        deleteNotificationNoPerms.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        Response deleteNotification = UtilIT.deleteNotification(createAccountId, authorApiToken);
        deleteNotification.then().assertThat().statusCode(OK.getStatusCode());

        Response getNotifications3 = UtilIT.getNotifications(authorApiToken);
        getNotifications3.then().assertThat()
                .body("data.notifications[0]", equalTo(null))
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
                .body("data.notifications.size()", equalTo(2))
                .statusCode(OK.getStatusCode());

        String firstNotificationType = JsonPath.from(getNotifications.body().asString()).getString("data.notifications[0].type");
        String secondNotificationType = JsonPath.from(getNotifications.body().asString()).getString("data.notifications[1].type");
        if (firstNotificationType.equals("DATASETCREATED")) {
            assertEquals("CREATEACC", secondNotificationType);
        } else if (firstNotificationType.equals("CREATEACC")) {
            assertEquals("DATASETCREATED", secondNotificationType);
        } else {
            fail("Unexpected notification type: " + firstNotificationType);
        }
    }
}
