package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import java.io.File;
import java.io.IOException;
import com.jayway.restassured.response.Response;
import java.util.UUID;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static com.jayway.restassured.RestAssured.given;
import com.jayway.restassured.path.xml.XmlPath;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import static junit.framework.Assert.assertEquals;
import org.junit.Ignore;

public class BatchImportIT {

    private static final Logger logger = Logger.getLogger(BatchImportIT.class.getCanonicalName());

    private static final String builtinUserKey = "burrito";
    private static final String keyString = "X-Dataverse-key";
    private static String EMPTY_STRING = "";
    private static final String idKey = "id";
    private static final String apiTokenKey = "apiToken";
    private static final String usernameKey = "userName";
    private static final String emailKey = "email";
    private static String username1;
    private static String username2;
    private static String apiToken1;
    private static String apiToken2;
    private static String dataverseAlias;
    private static int datasetId;
    private static String importDirectoryAndDataverseAliasMustMatch = "batchImportDv";

    public BatchImportIT() {
    }

    @BeforeClass
    public static void setUpClass() {

        Response createUserResponse = createUser(getRandomUsername(), "firstName", "lastName");
//        createUserResponse.prettyPrint();
        assertEquals(200, createUserResponse.getStatusCode());

        JsonPath createdUser1 = JsonPath.from(createUserResponse.body().asString());
        apiToken1 = createdUser1.getString("data." + apiTokenKey);
        username1 = createdUser1.getString("data.user." + usernameKey);

        Response makeSuperuserResponse = makeSuperuser(username1);
        assertEquals(200, makeSuperuserResponse.getStatusCode());

        Response createUserResponse1 = createUser(getRandomUsername(), "firstName", "lastName");
//        createUserResponse.prettyPrint();
        assertEquals(200, createUserResponse1.getStatusCode());

        JsonPath createdUser2 = JsonPath.from(createUserResponse1.body().asString());
        apiToken2 = createdUser2.getString("data." + apiTokenKey);
        username2 = createdUser2.getString("data.user." + usernameKey);

        dataverseAlias = "dv" + getRandomIdentifier();
        Response createDataverseResponse = createDataverse(dataverseAlias, apiToken1);
//        createDataverseResponse.prettyPrint();
//        assertEquals(201, createDataverseResponse.getStatusCode());
    }

    @AfterClass
    public static void tearDownClass() {
        boolean disabled = false;

        if (disabled) {
            return;
        }

        Response destroyDatasetResponse = destroyDataset(datasetId, apiToken1);
        destroyDatasetResponse.prettyPrint();
//        assertEquals(200, destroyDatasetResponse.getStatusCode());

        Response deleteDataverseResponse = deleteDataverse(dataverseAlias, apiToken1);
//        deleteDataverseResponse.prettyPrint();
//        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUser1Response = deleteUser(username1);
//        deleteUser1Response.prettyPrint();
        assertEquals(200, deleteUser1Response.getStatusCode());

        Response deleteUser2Response = deleteUser(username2);
//        deleteUser2Response.prettyPrint();
        assertEquals(200, deleteUser2Response.getStatusCode());
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void roundTripDdi() throws Exception {
        String directoryPath = "scripts/issues/907/" + importDirectoryAndDataverseAliasMustMatch;
        String absoluteDirectoryPath = new File(directoryPath).getAbsolutePath();

        String parentDataverse = dataverseAlias;
        Response migrateResponse = migrate(absoluteDirectoryPath, parentDataverse, apiToken1);
//        migrateResponse.prettyPrint();

        Thread.sleep(500);
        Response listDatasetsResponse = listDatasets(parentDataverse, apiToken1);
//        listDatasetsResponse.prettyPrint();
        XmlPath xmlPath = new XmlPath(listDatasetsResponse.body().asString());
        String datasetUrlFromSword = xmlPath.getString("feed.entry[0].id");
        if (datasetUrlFromSword != null && !datasetUrlFromSword.isEmpty()) {
            String persistentIdentifier = datasetUrlFromSword.substring(68);
            Response indexDatasetResponse = indexDataset(persistentIdentifier);
//            indexDatasetResponse.prettyPrint();
            datasetId = JsonPath.from(indexDatasetResponse.body().asString()).getInt("data.id");
            Response datasetAsJsonResponse = getDatasetAsJson(datasetId, apiToken1);
//            datasetAsJsonResponse.prettyPrint();
            String title = JsonPath.from(datasetAsJsonResponse.body().asString()).getString("data.latestVersion.metadataBlocks.citation.fields[0].value");
            String persistentUrl = JsonPath.from(datasetAsJsonResponse.body().asString()).getString("data.persistentUrl");
            logger.info(title + " - " + persistentUrl);
            boolean ddiFromDto = false;
            Response datasetAsDdi = getDatasetAsDdi(persistentIdentifier, ddiFromDto, apiToken1);
            String minimalDdiMethod = datasetAsDdi.prettyPrint();

            ddiFromDto = true;
            
            
            Response datasetAsDdiFromDto = getDatasetAsDdi(persistentIdentifier, ddiFromDto, apiToken1);
            String fromDto = datasetAsDdiFromDto.prettyPrint();

            /**
             * Parity with the minimal DDI export is a step along the way. It
             * demonstrates that we are producing valid DDI according to
             * http://guides.dataverse.org/en/latest/developers/tools.html#msv
             * but the next step will be producing a full DDI similar to what is
             * being imported in this round trip test.
             */
            boolean parityWithMinimalDdiExport = false;
            if (parityWithMinimalDdiExport) {
                assertEquals(minimalDdiMethod, fromDto);
            }
//            File originalFile = new File(absoluteDirectoryPath).listFiles()[0];
//            String originalPretty = prettyFormat(new String(Files.readAllBytes(Paths.get(originalFile.getAbsolutePath()))));
//            String exportedPretty = prettyFormat(datasetAsDdi.body().asString());
//            logger.fine("original: " + originalPretty);
//            logger.fine("exported: " + exportedPretty);
            boolean doneWithDdiExportIssue2579 = false;
            if (doneWithDdiExportIssue2579) {
                /**
                 * @todo Implement DDI export
                 * https://github.com/IQSS/dataverse/issues/2579
                 */
//                assertEquals(exportedPretty, originalPretty);
            }
        } else {
            boolean ddiFromDto = false;
            Response datasetAsDdi = getDatasetAsDdi("doi:10.5072/junkDoi", ddiFromDto, apiToken1);
            datasetAsDdi.prettyPrint();
            assertEquals(404, datasetAsDdi.getStatusCode());
        }
    }

    public static String prettyFormat(String input, int indent) {
        try {
            Source source = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult streamResult = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, streamResult);
            return streamResult.getWriter().toString();
        } catch (IllegalArgumentException | TransformerException ex) {
            logger.fine("Exception while pretty printing XML: " + ex);
            return null;
        }
    }

    public static String prettyFormat(String input) {
        return prettyFormat(input, 2);
    }

    @Test
    public void ensureDdiExportIsSuperuserOnlyForNow() throws Exception {
        boolean ddiFromDto = false;
        Response datasetAsDdiNonSuperuser = getDatasetAsDdi("doi:10.5072/junkDoi", ddiFromDto, apiToken2);
//        datasetAsDdiNonSuperuser.prettyPrint();
        assertEquals(404, datasetAsDdiNonSuperuser.getStatusCode());

        Response datasetAsDdiInvalidApiToken = getDatasetAsDdi("doi:10.5072/junkDoi", ddiFromDto, "junkToken");
//        datasetAsDdiInvalidApiToken.prettyPrint();
        assertEquals(404, datasetAsDdiInvalidApiToken.getStatusCode());
    }

    private Response migrate(String filename, String parentDataverse, String apiToken) throws IOException {
        if (filename == null || filename.isEmpty()) {
            throw new IOException("null or empty filename");
        }
        logger.info(parentDataverse + "dataverse targeted for import of " + filename);
        Response response = given()
                .contentType("application/atom+xml")
                .get("/api/batch/migrate/?dv=" + parentDataverse + "&key=" + apiToken + "&path=" + filename + "&createDV=true");
        return response;
    }

    private static Response createUser(String username, String firstName, String lastName) {
        String userAsJson = getUserAsJsonString(username, firstName, lastName);
        String password = getPassword(userAsJson);
        Response response = given()
                .body(userAsJson)
                .contentType(ContentType.JSON)
                .post("/api/builtin-users?key=" + builtinUserKey + "&password=" + password);
        return response;
    }

    private static Response makeSuperuser(String userToMakeSuperuser) {
        Response response = given().post("/api/admin/superuser/" + userToMakeSuperuser);
        return response;
    }

    private static Response deleteUser(String username) {
        Response deleteUserResponse = given()
                .delete("/api/admin/authenticatedUsers/" + username + "/");
        return deleteUserResponse;
    }

    private static Response createDataverse(String alias, String apiToken) {
        JsonArrayBuilder contactArrayBuilder = Json.createArrayBuilder();
        contactArrayBuilder.add(Json.createObjectBuilder().add("contactEmail", getEmailFromUserName(getRandomIdentifier())));
        JsonArrayBuilder subjectArrayBuilder = Json.createArrayBuilder();
        subjectArrayBuilder.add("Other");
        JsonObject dvData = Json.createObjectBuilder()
                .add("alias", alias)
                .add("name", alias)
                .add("dataverseContacts", contactArrayBuilder)
                .add("dataverseSubjects", subjectArrayBuilder)
                .build();
        Response createDataverseResponse = given()
                .body(dvData.toString()).contentType(ContentType.JSON)
                .when().post("/api/dataverses/:root?key=" + apiToken);
        return createDataverseResponse;
    }

    private static Response deleteDataverse(String doomed, String apiToken) {
        return given().delete("/api/dataverses/" + doomed + "?key=" + apiToken);
    }

    private Response getSwordStatement(String persistentId, String apiToken) {
        Response swordStatementResponse = given()
                .auth().basic(apiToken, EMPTY_STRING)
                .get("/dvn/api/data-deposit/v1.1/swordv2/statement/study/" + persistentId);
        return swordStatementResponse;
    }

    private Response listDatasets(String dataverseAlias, String apiToken) {
        Response swordStatementResponse = given()
                .auth().basic(apiToken, EMPTY_STRING)
                .get("/dvn/api/data-deposit/v1.1/swordv2/collection/dataverse/" + dataverseAlias);
        return swordStatementResponse;
    }

    private Response indexDataset(String persistentIdentifier) {
        Response response = given()
                .get("/api/admin/index/dataset?persistentId=" + persistentIdentifier);
        return response;
    }

    private Response getDatasetAsJson(int datasetId, String apiToken) {
        Response response = given()
                .header(keyString, apiToken)
                .get("/api/datasets/" + datasetId);
        return response;
    }

    private Response getDatasetAsDdi(String persistentIdentifier, boolean dto, String apiToken) {
        Response response = given()
                .header(keyString, apiToken)
                .get("/api/datasets/:persistentId/ddi?persistentId=" + persistentIdentifier + "&dto=" + dto);
        return response;
    }

    private static Response destroyDataset(Integer datasetId, String apiToken) {
        return given()
                .header(keyString, apiToken)
                .delete("/api/datasets/" + datasetId + "/destroy");
    }

    private static String getRandomUsername() {
        return "user" + getRandomIdentifier().substring(0, 8);
    }

    private static String getRandomIdentifier() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String getUserAsJsonString(String username, String firstName, String lastName) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(usernameKey, username);
        builder.add("firstName", firstName);
        builder.add("lastName", lastName);
        builder.add(emailKey, getEmailFromUserName(username));
        String userAsJson = builder.build().toString();
        logger.fine("User to create: " + userAsJson);
        return userAsJson;
    }

    private static String getPassword(String jsonStr) {
        String password = JsonPath.from(jsonStr).get(usernameKey);
        return password;
    }

    private static String getEmailFromUserName(String username) {
        return username + "@mailinator.com";
    }

}
