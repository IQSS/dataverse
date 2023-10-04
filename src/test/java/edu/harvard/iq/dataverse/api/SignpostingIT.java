package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import io.restassured.response.Response;

import edu.harvard.iq.dataverse.util.json.JsonUtil;

import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.json.JsonObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SignpostingIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testSignposting() {

        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response toggleSuperuser = UtilIT.makeSuperUser(username);
        toggleSuperuser.then().assertThat().statusCode(OK.getStatusCode());

        Response createDataverse = UtilIT.createRandomDataverse(apiToken);
        createDataverse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse);
        Integer dataverseId = UtilIT.getDataverseIdFromResponse(createDataverse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat().statusCode(CREATED.getStatusCode());

        String datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverse.then().assertThat().statusCode(OK.getStatusCode());
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPid, "major", apiToken);
        publishDataset.then().assertThat().statusCode(OK.getStatusCode());

        String datasetLandingPage = RestAssured.baseURI + "/dataset.xhtml?persistentId=" + datasetPid;
        System.out.println("Checking dataset landing page for Signposting: " + datasetLandingPage);
        Response getHtml = given().get(datasetLandingPage);

        System.out.println("Link header: " + getHtml.getHeader("Link"));

        getHtml.then().assertThat().statusCode(OK.getStatusCode());

        // Make sure there's Signposting stuff in the "Link" header such as
        // the dataset PID, cite-as, etc.
        String linkHeader = getHtml.getHeader("Link");
        assertTrue(linkHeader.contains(datasetPid));
        assertTrue(linkHeader.contains("cite-as"));
        assertTrue(linkHeader.contains("describedby"));

        Response headHtml = given().head(datasetLandingPage);

        System.out.println("Link header: " + headHtml.getHeader("Link"));

        headHtml.then().assertThat().statusCode(OK.getStatusCode());

        // Make sure there's Signposting stuff in the "Link" header such as
        // the dataset PID, cite-as, etc.
        linkHeader = getHtml.getHeader("Link");
        assertTrue(linkHeader.contains(datasetPid));
        assertTrue(linkHeader.contains("cite-as"));
        assertTrue(linkHeader.contains("describedby"));
        assertTrue(linkHeader.contains("<http://creativecommons.org/publicdomain/zero/1.0>;rel=\"license\""));

        Pattern pattern = Pattern.compile("<([^<]*)> ; rel=\"linkset\";type=\"application\\/linkset\\+json\"");
        Matcher matcher = pattern.matcher(linkHeader);
        matcher.find();
        String linksetUrl = matcher.group(1);

        System.out.println("Linkset URL: " + linksetUrl);

        Response linksetResponse = given().accept(ContentType.JSON).get(linksetUrl);

        String responseString = linksetResponse.getBody().asString();

        JsonObject data = JsonUtil.getJsonObject(responseString);
        JsonObject lso = data.getJsonArray("linkset").getJsonObject(0);
        System.out.println("Linkset: " + lso.toString());

        linksetResponse.then().assertThat().statusCode(OK.getStatusCode());

        assertTrue(lso.getString("anchor").indexOf("/dataset.xhtml?persistentId=" + datasetPid) > 0);
        assertTrue(lso.containsKey("describedby"));

        // Test export URL from link header
        // regex inspired by https://stackoverflow.com/questions/68860255/how-to-match-the-closest-opening-and-closing-brackets
        Pattern exporterPattern = Pattern.compile("[<\\[][^()\\[\\]]*?exporter=schema.org[^()\\[\\]]*[>\\]]");
        Matcher exporterMatcher = exporterPattern.matcher(linkHeader);
        exporterMatcher.find();

        Response exportDataset = UtilIT.exportDataset(datasetPid, "schema.org");
        exportDataset.prettyPrint();
        exportDataset.then().assertThat().statusCode(OK.getStatusCode());

    }

}
