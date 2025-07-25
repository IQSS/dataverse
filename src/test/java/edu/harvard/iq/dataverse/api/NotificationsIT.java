package edu.harvard.iq.dataverse.api;

import static edu.harvard.iq.dataverse.UserNotification.Type.CREATEACC;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.logging.Logger;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;
import java.util.logging.Level;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class NotificationsIT {

    private static final Logger logger = Logger.getLogger(NotificationsIT.class.getCanonicalName());

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testNotifications() {

        Response createAuthor = UtilIT.createRandomUser();
        createAuthor.prettyPrint();
        createAuthor.then().assertThat()
                .statusCode(OK.getStatusCode());
        String authorUsername = UtilIT.getUsernameFromResponse(createAuthor);
        String authorApiToken = UtilIT.getApiTokenFromResponse(createAuthor);

        Response nopermsUser = UtilIT.createRandomUser();
        nopermsUser.prettyPrint();
        nopermsUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String nopermsApiToken = UtilIT.getApiTokenFromResponse(nopermsUser);

        // Some API calls don't generate a notification: https://github.com/IQSS/dataverse/issues/1342
        Response createDataverseResponse = UtilIT.createRandomDataverse(authorApiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        // Some API calls don't generate a notification: https://github.com/IQSS/dataverse/issues/1342
        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, authorApiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

//        int datasetId = JsonPath.from(createDataset.body().asString()).getInt("data.id");
//        String datasetPersistentId = JsonPath.from(createDataset.body().asString()).getString("data.persistentId");

        Response getNotifications = UtilIT.getNotifications(authorApiToken);
        getNotifications.prettyPrint();
        getNotifications.then().assertThat()
                .body("data.notifications[0].type", equalTo(CREATEACC.toString()))
                .body("data.notifications[0].displayAsRead", equalTo(false))
                .body("data.notifications[1]", equalTo(null))
                .statusCode(OK.getStatusCode());

        Response unreadCount = UtilIT.getUnreadNotificationsCount(authorApiToken);
        unreadCount.prettyPrint();
        unreadCount.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.unreadCount", equalTo(1));

        long createAccountId = JsonPath.from(getNotifications.getBody().asString()).getLong("data.notifications[0].id");

        Response markReadNoPerms = UtilIT.markNotificationAsRead(createAccountId, nopermsApiToken);
        markReadNoPerms.prettyPrint();
        markReadNoPerms.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        Response markRead = UtilIT.markNotificationAsRead(createAccountId, authorApiToken);
        markRead.prettyPrint();
        markRead.then().assertThat().statusCode(OK.getStatusCode());

        Response getNotifications2 = UtilIT.getNotifications(authorApiToken);
        getNotifications2.prettyPrint();
        getNotifications2.then().assertThat()
                .body("data.notifications[0].type", equalTo(CREATEACC.toString()))
                .body("data.notifications[0].displayAsRead", equalTo(true))
                .body("data.notifications[1]", equalTo(null))
                .statusCode(OK.getStatusCode());

        Response deleteNotificationNoPerms = UtilIT.deleteNotification(createAccountId, nopermsApiToken);
        deleteNotificationNoPerms.prettyPrint();
        deleteNotificationNoPerms.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        Response deleteNotification = UtilIT.deleteNotification(createAccountId, authorApiToken);
        deleteNotification.prettyPrint();
        deleteNotification.then().assertThat().statusCode(OK.getStatusCode());

        Response getNotifications3 = UtilIT.getNotifications(authorApiToken);
        getNotifications3.prettyPrint();
        getNotifications3.then().assertThat()
                .body("data.notifications[0]", equalTo(null))
                .statusCode(OK.getStatusCode());

        Response createCoauthor = UtilIT.createRandomUser();
        createCoauthor.prettyPrint();
        createCoauthor.then().assertThat().statusCode(OK.getStatusCode());
        String coauthorUsername = UtilIT.getUsernameFromResponse(createCoauthor);
        String coauthorApiToken = UtilIT.getApiTokenFromResponse(createCoauthor);

        Response grantRole = UtilIT.grantRoleOnDataverse(dataverseAlias, DataverseRole.DS_CONTRIBUTOR, "@" + coauthorUsername, authorApiToken);
        grantRole.prettyPrint();
        assertEquals(OK.getStatusCode(), grantRole.getStatusCode());

        Response coauthorGetNotifications = UtilIT.getNotifications(coauthorApiToken);
        coauthorGetNotifications.prettyPrint();
        coauthorGetNotifications.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.notifications[0].type", equalTo(CREATEACC.toString()))
                .body("data.notifications[0].displayAsRead", equalTo(false))
                // No notification that the coauthor now has access? A case of https://github.com/IQSS/dataverse/issues/1342
                .body("data.notifications[1]", equalTo(null));

    }
}
