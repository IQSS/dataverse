package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import java.util.logging.Logger;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.BeforeClass;
import org.junit.Test;

public class NotificationsIT {

    private static final Logger logger = Logger.getLogger(NotificationsIT.class.getCanonicalName());

    @BeforeClass
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
        Response getNotifications = UtilIT.getNotifications(authorApiToken);
        getNotifications.prettyPrint();
        getNotifications.then().assertThat()
                .body("data.notifications[0].type", equalTo("CREATEACC"))
                .body("data.notifications[1]", equalTo(null))
                .statusCode(OK.getStatusCode());

        long id = JsonPath.from(getNotifications.getBody().asString()).getLong("data.notifications[0].id");

        Response deleteNotification = UtilIT.deleteNotification(id, authorApiToken);
        deleteNotification.prettyPrint();
        deleteNotification.then().assertThat().statusCode(OK.getStatusCode());

        Response getNotifications2 = UtilIT.getNotifications(authorApiToken);
        getNotifications2.prettyPrint();
        getNotifications2.then().assertThat()
                .body("data.notifications[0]", equalTo(null))
                .statusCode(OK.getStatusCode());

    }
}
