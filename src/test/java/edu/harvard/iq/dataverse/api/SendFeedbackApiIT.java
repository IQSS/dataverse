package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.text.MessageFormat;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SendFeedbackApiIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @AfterEach
    public void reset() {
        UtilIT.deleteSetting(SettingsServiceBean.Key.RateLimitingCapacityByTierAndAction);
    }

    @Test
    public void testBadJson() {
        Response response = UtilIT.sendFeedback("{'notValidJson'", null);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", CoreMatchers.startsWith("Invalid JSON; error message:"));
    }

    @Test
    public void testSupportRequest() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("fromEmail", "from@mailinator.com");
        job.add("subject", "Help!");
        job.add("body", "I need help.");

        Response response = UtilIT.sendFeedback(job, null);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].fromEmail", CoreMatchers.equalTo("from@mailinator.com"));
    }

    @Test
    public void testSendFeedbackOnRootDataverse() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        long rootDataverseId = 1;
        job.add("targetId", rootDataverseId);
        job.add("fromEmail", "from@mailinator.com");
        job.add("toEmail", "to@mailinator.com");
        job.add("subject", "collaboration");
        job.add("body", "Are you interested writing a grant based on this research?");

        Response response = UtilIT.sendFeedback(job, null);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());

        job = Json.createObjectBuilder();
        job.add("identifier", "root");
        job.add("fromEmail", "from@mailinator.com");
        job.add("toEmail", "to@mailinator.com");
        job.add("subject", "collaboration");
        job.add("body", "Are you interested writing a grant based on this research?");

        response = UtilIT.sendFeedback(job, null);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testSendFeedbackOnDataset() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String fromEmail = UtilIT.getEmailFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        String pathToJsonFile = "scripts/api/data/dataset-create-new-all-default-fields.json";
        Response createDataset = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, apiToken);
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        long datasetId = JsonPath.from(createDataset.body().asString()).getLong("data.id");
        String persistentId = JsonPath.from(createDataset.body().asString()).getString("data.persistentId");
        Response response;
        String pathToFile = "src/main/webapp/resources/images/dataverseproject.png";
        Response uploadResponse = UtilIT.uploadFileViaNative(String.valueOf(datasetId), pathToFile, apiToken);
        uploadResponse.prettyPrint();
        long fileId = JsonPath.from(uploadResponse.body().asString()).getLong("data.files[0].dataFile.id");

        // Test with body text length to long (length of body after sanitizing/removing html = 67)
        UtilIT.setSetting(SettingsServiceBean.Key.ContactFeedbackMessageSizeLimit, "60");
        response = UtilIT.sendFeedback(buildJsonEmail(0, persistentId, null), apiToken);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", CoreMatchers.equalTo(MessageFormat.format(BundleUtil.getStringFromBundle("sendfeedback.body.error.exceedsLength"), 67, 60)));
        // reset to unlimited
        UtilIT.setSetting(SettingsServiceBean.Key.ContactFeedbackMessageSizeLimit, "0");

        // Test with no body/body length =0
        response = UtilIT.sendFeedback(Json.createObjectBuilder().add("targetId", datasetId).add("subject", "collaboration").add("body", ""), apiToken);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", CoreMatchers.equalTo(BundleUtil.getStringFromBundle("sendfeedback.body.error.isEmpty")));

        // Test with missing subject
        response = UtilIT.sendFeedback(Json.createObjectBuilder().add("targetId", datasetId).add("body", ""), apiToken);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", CoreMatchers.equalTo(BundleUtil.getStringFromBundle("sendfeedback.body.error.missingRequiredFields")));

        // Test send feedback on DataFile
        // Test don't send fromEmail. Let it get it from the requesting user
        response = UtilIT.sendFeedback(buildJsonEmail(fileId, null, null), apiToken);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].fromEmail", CoreMatchers.equalTo(fromEmail));

        // Test guest calling with no token
        fromEmail = "testEmail@example.com";
        response = UtilIT.sendFeedback(buildJsonEmail(datasetId, null, fromEmail), null);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].fromEmail", CoreMatchers.equalTo(fromEmail));
        validateEmail(response.body().asString());

        // Test guest calling with no token and missing email
        response = UtilIT.sendFeedback(buildJsonEmail(datasetId, null, null), null);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", CoreMatchers.equalTo(BundleUtil.getStringFromBundle("sendfeedback.fromEmail.error.missing")));

        // Test with invalid email - also tests that fromEmail trumps the users email if it is included in the Json
        response = UtilIT.sendFeedback(buildJsonEmail(datasetId, null, "BADEmail"), apiToken);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", CoreMatchers.equalTo(MessageFormat.format(BundleUtil.getStringFromBundle("sendfeedback.fromEmail.error.invalid"), "BADEmail")));

        // Test with bad identifier
        response = UtilIT.sendFeedback(buildJsonEmail(0, "BadIdentifier", null), apiToken);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", CoreMatchers.equalTo(BundleUtil.getStringFromBundle("sendfeedback.request.error.targetNotFound")));
    }

    private JsonObjectBuilder buildJsonEmail(long targetId, String identifier, String fromEmail) {
        JsonObjectBuilder job = Json.createObjectBuilder();
        if (targetId > 0) {
            job.add("targetId", targetId);
        }
        if (identifier != null) {
            job.add("identifier", identifier);
        }
        job.add("subject", "collaboration");
        job.add("body", "Are you interested writing a grant based on this research? {\"<script src=\\\"http://malicious.url.com\\\"/>\", \"\"}");
        if (fromEmail != null) {
            job.add("fromEmail", fromEmail);
        }
        return job;
    }
    private void validateEmail(String body) {
        assertTrue(!body.contains("malicious.url.com"));
    }

    @Test
    public void testSendRateLimited() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String fromEmail = UtilIT.getEmailFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        String pathToJsonFile = "scripts/api/data/dataset-create-new-all-default-fields.json";
        Response createDataset = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, apiToken);
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        long datasetId = JsonPath.from(createDataset.body().asString()).getLong("data.id");
        Response response;

        // Test with rate limiting on
        UtilIT.setSetting(SettingsServiceBean.Key.RateLimitingCapacityByTierAndAction, "[{\"tier\": 0, \"limitPerHour\": 1, \"actions\": [\"CheckRateLimitForDatasetFeedbackCommand\"]}]");
        // This call gets allowed because the setting change OKs it when resetting rate limiting
        response = UtilIT.sendFeedback(buildJsonEmail(datasetId, null, "testEmail@example.com"), null);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].fromEmail", CoreMatchers.equalTo("testEmail@example.com"));

        // Call 1 of the 1 per hour limit
        response = UtilIT.sendFeedback(buildJsonEmail(datasetId, null, "testEmail2@example.com"), null);
        response.prettyPrint();
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
        // Call 2 - over the limit
        response = UtilIT.sendFeedback(buildJsonEmail(datasetId, null, "testEmail2@example.com"), null);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(TOO_MANY_REQUESTS.getStatusCode());

        response = UtilIT.sendFeedback(buildJsonEmail(datasetId, null, null), apiToken);
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].fromEmail", CoreMatchers.equalTo(fromEmail));
    }
}
