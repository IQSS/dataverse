package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Guestbook;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GuestbooksIT {

    @Test
    public void testGuestbook() throws IOException, JsonParseException {
        Response createResponse = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createResponse);
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        String datasetPid = JsonPath.from(createDatasetResponse.asString()).getString("data.persistentId");

        // Test create Guestbook
        Guestbook guestbook = UtilIT.createRandomGuestbook(dataverseAlias, datasetPid, apiToken);

        // Test get Guestbook
        Response getGestbookResponse = UtilIT.getGuestbook(guestbook.getId(), apiToken);
        getGestbookResponse.then().assertThat().statusCode(OK.getStatusCode());
        JsonObject data = JsonUtil.getJsonObject(getGestbookResponse.getBody().asString());
        String guestbookAsJson = data.getJsonObject("data").toString();

        // Test Update Guestbook
        guestbookAsJson = guestbookAsJson.replace("my test guestbook","my modified test guestbook")
            .replace("positionRequired\":false", "positionRequired\": true")
            .replace("displayOrder\":3}", "displayOrder\":3},{\"value\":\"Green\",\"displayOrder\":4}"); // add a new option (Green)

        Response updateGuestbookResponse = UtilIT.updateGuestbook(guestbook.getId(), guestbookAsJson, apiToken);
        updateGuestbookResponse.then().assertThat().statusCode(OK.getStatusCode());

        getGestbookResponse = UtilIT.getGuestbook(guestbook.getId(), apiToken);
        getGestbookResponse.then().assertThat().statusCode(OK.getStatusCode());
        data = JsonUtil.getJsonObject(getGestbookResponse.getBody().asString());
        String newGuestbookAsJson = data.getJsonObject("data").toString();
        // verify changed fields are changed
        assertTrue(newGuestbookAsJson.contains("my modified test guestbook"));
        assertTrue(newGuestbookAsJson.contains("\"positionRequired\":true"));
        // verify all custom question options are there plus the new one (Green)
        assertTrue(newGuestbookAsJson.contains("\"value\":\"Red\""));
        assertTrue(newGuestbookAsJson.contains("\"value\":\"White\""));
        assertTrue(newGuestbookAsJson.contains("\"value\":\"Yellow\""));
        assertTrue(newGuestbookAsJson.contains("\"value\":\"Purple\""));
        assertTrue(newGuestbookAsJson.contains("\"value\":\"Green\""));

        // Test remove a custom question
        JsonParser jsonParser = new JsonParser();
        guestbook = new Guestbook();
        // use the json from the previous 'get' to parse
        guestbook = jsonParser.parseGuestbook(data.getJsonObject("data"), guestbook);
        // remove the first question
        String firstQuestion = guestbook.getCustomQuestions().get(0).getQuestionString();
        String secondQuestion = guestbook.getCustomQuestions().get(1).getQuestionString();
        guestbook.getCustomQuestions().remove(0);
        // get the json minus the removed question and call update
        guestbookAsJson = JsonPrinter.json(guestbook).build().toString();
        updateGuestbookResponse = UtilIT.updateGuestbook(guestbook.getId(), guestbookAsJson, apiToken);
        updateGuestbookResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        String body = updateGuestbookResponse.getBody().asString();
        assertTrue(!body.contains(firstQuestion)); // verify first question is removed
        assertTrue(body.contains(secondQuestion)); // verify second question is still there

        // Test disable Guestbook
        updateGuestbookResponse = UtilIT.enableGuestbook(dataverseAlias, guestbook.getId(), apiToken, "false");
        updateGuestbookResponse.prettyPrint();
        updateGuestbookResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.message", containsString("enabled=false"));
    }
}
