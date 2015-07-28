package edu.harvard.iq.dataverse.api;

import com.google.common.base.Stopwatch;
import static com.jayway.restassured.RestAssured.given;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import static com.jayway.restassured.path.xml.XmlPath.from;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import junit.framework.Assert;
import static junit.framework.Assert.assertEquals;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SearchIT {

    private static final Logger logger = Logger.getLogger(SearchIT.class.getCanonicalName());

    private static final String builtinUserKey = "burrito";
    private static final String EMPTY_STRING = "";
    private static final String idKey = "id";
    private static final String apiTokenKey = "apiToken";
    private static final String usernameKey = "userName";
    private static final String emailKey = "email";
    private static TestUser homer;
    private static TestUser ned;
    private static final String dv1 = "dv1";
    private static final String dv2 = "dv2";
    private static String dataset1;
    private static String dataset2;
    private static long nedAdminOnRootAssignment;
    private static String dataverseToCreateDataset1In = "root";
    private static final String dataverseToCreateDataset2In = "dv2";
    private static final boolean homerGivesNedPermissionAtRootTestDisabled = false;
    private static final boolean homerGivesNedPermissionAtNewDvDisabled = false;
    private Stopwatch timer;

    public SearchIT() {
    }

    @BeforeClass
    public static void setUpClass() {

        boolean enabled = true;
        if (!enabled) {
            return;
        }

        logger.info("Running setup...");

        JsonObject homerJsonObject = createUser(getUserAsJsonString("homer", "Homer", "Simpson"));
        homer = new TestUser(homerJsonObject);

        Response makeSuperUserResponse = makeSuperuser(homer.getUsername());
        assertEquals(200, makeSuperUserResponse.getStatusCode());

        JsonObject nedJsonObject = createUser(getUserAsJsonString("ned", "Ned", "Flanders"));
        ned = new TestUser(nedJsonObject);
    }

    @Test
    public void homerGivesNedPermissionAtRoot() {
        if (homerGivesNedPermissionAtRootTestDisabled) {
            return;
        }

        Response enableNonPublicSearch = enableSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        assertEquals(200, enableNonPublicSearch.getStatusCode());

        long rootDataverseId = 1;
        String rootDataverseAlias = getDataverseAlias(rootDataverseId, homer.getApiToken());
        if (rootDataverseAlias != null) {
            dataverseToCreateDataset1In = rootDataverseAlias;
        }

        String xmlIn = getDatasetXml(homer.getUsername(), homer.getUsername(), homer.getUsername());
        Response createDataset1Response = createDataset(xmlIn, dataverseToCreateDataset1In, homer.getApiToken());
//        System.out.println(createDataset1Response.prettyPrint());
        assertEquals(201, createDataset1Response.getStatusCode());

        dataset1 = getGlobalId(createDataset1Response);
//        String zipFileName = "1000files.zip";
        String zipFileName = "trees.zip";
        try {
            Response uploadZipFileResponse = uploadZipFile(dataset1, zipFileName, homer.getApiToken());
        } catch (FileNotFoundException ex) {
            System.out.println("Problem uploading " + zipFileName + ": " + ex.getMessage());
        }

        Integer idHomerFound = printDatasetId(dataset1, homer);
        assertEquals(true, idHomerFound != null);

        Integer idNedFoundBeforeBecomingAdmin = printDatasetId(dataset1, ned);
        String roleToAssign = "admin";
        assertEquals(null, idNedFoundBeforeBecomingAdmin);

        timer = Stopwatch.createStarted();
        Response grantNedAdminOnRoot = grantRole(dataverseToCreateDataset1In, roleToAssign, ned.getUsername(), homer.getApiToken());
//        System.out.println(grantNedAdminOnRoot.prettyPrint());
        System.out.println("Method took: " + timer.stop());
        assertEquals(200, grantNedAdminOnRoot.getStatusCode());

        Integer idNedFoundAfterBecomingAdmin = printDatasetId(dataset1, ned);
//        Response contentDocResponse = querySolr("entityId:" + idHomerFound);
//        System.out.println(contentDocResponse.prettyPrint());
//        Response permDocResponse = querySolr("definitionPointDvObjectId:" + idHomerFound);
//        System.out.println(idHomerFound + " was found by homer (user id " + homer.getId() + ")");
//        System.out.println(idNedFoundAfterBecomingAdmin + " was found by ned (user id " + ned.getId() + ")");
        assertEquals(idHomerFound, idNedFoundAfterBecomingAdmin);

        nedAdminOnRootAssignment = getRoleAssignmentId(grantNedAdminOnRoot);
        timer = Stopwatch.createStarted();
        Response revokeNedAdminOnRoot = revokeRole(dataverseToCreateDataset1In, nedAdminOnRootAssignment, homer.getApiToken());
//        System.out.println(revokeNedAdminOnRoot.prettyPrint());
        System.out.println("Method took: " + timer.stop());
        assertEquals(200, revokeNedAdminOnRoot.getStatusCode());

        Integer idNedFoundAfterNoLongerAdmin = printDatasetId(dataset1, ned);
        assertEquals(null, idNedFoundAfterNoLongerAdmin);

        Response disableNonPublicSearch = deleteSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        assertEquals(200, disableNonPublicSearch.getStatusCode());

    }

    @Test
    public void homerGivesNedPermissionAtNewDv() {
        if (homerGivesNedPermissionAtNewDvDisabled) {
            return;
        }

        Response enableNonPublicSearch = enableSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        assertEquals(200, enableNonPublicSearch.getStatusCode());

        TestDataverse dataverseToCreate = new TestDataverse(dv2, dv2, Dataverse.DataverseType.ORGANIZATIONS_INSTITUTIONS);
        Response createDvResponse = createDataverse(dataverseToCreate, homer);
        assertEquals(201, createDvResponse.getStatusCode());

        String xmlIn = getDatasetXml(homer.getUsername(), homer.getUsername(), homer.getUsername());
        Response createDataset1Response = createDataset(xmlIn, dataverseToCreateDataset2In, homer.getApiToken());
        assertEquals(201, createDataset1Response.getStatusCode());

        dataset2 = getGlobalId(createDataset1Response);
//        String zipFileName = "noSuchFile.zip";
        String zipFileName = "trees.zip";
//        String zipFileName = "100files.zip";
//        String zipFileName = "1000files.zip";

        timer = Stopwatch.createStarted();
        Response uploadZipFileResponse;
        try {
            uploadZipFileResponse = uploadZipFile(dataset2, zipFileName, homer.getApiToken());
        } catch (FileNotFoundException ex) {
            System.out.println("Problem uploading " + zipFileName + ": " + ex.getMessage());
        }
        System.out.println("Method took: " + timer.stop());

        Integer idHomerFound = printDatasetId(dataset2, homer);
        List<Integer> idsOfFilesUploaded = getIdsOfFilesUploaded(dataset2, idHomerFound, homer.getApiToken());
        System.out.println("file ids found: " + idsOfFilesUploaded);
        if (!idsOfFilesUploaded.isEmpty()) {
            int firstFileId = idsOfFilesUploaded.get(0);
            checkPermissionsOnDvObject(firstFileId, homer.getApiToken());
        }
        assertEquals(true, idHomerFound != null);
//
//        Integer idNedFoundBeforeBecomingAdmin = printDatasetId(dataset1, ned);
        String roleToAssign = "admin";
//        assertEquals(null, idNedFoundBeforeBecomingAdmin);

        timer = Stopwatch.createStarted();
        Response grantNedAdmin = grantRole(dataverseToCreateDataset2In, roleToAssign, ned.getUsername(), homer.getApiToken());
//        System.out.println(grantNedAdmin.prettyPrint());
        System.out.println("Method took: " + timer.stop());
//        assertEquals(200, grantNedAdmin.getStatusCode());

        Integer idNedFoundAfterBecomingAdmin = printDatasetId(dataset2, ned);

        if (!idsOfFilesUploaded.isEmpty()) {
            int firstFileId = idsOfFilesUploaded.get(0);
            checkPermissionsOnDvObject(firstFileId, homer.getApiToken());
            Response solrQueryPerms = querySolr(SearchFields.DEFINITION_POINT_DVOBJECT_ID + ":" + firstFileId);
//            solrQueryPerms.prettyPrint();
            Response solrQueryContent = querySolr(SearchFields.ENTITY_ID + ":" + firstFileId);
//            solrQueryContent.prettyPrint();
        }

        System.out.println(idHomerFound + " was found by homer (user id " + homer.getId() + ")");
        System.out.println(idNedFoundAfterBecomingAdmin + " was found by ned (user id " + ned.getId() + ")");

        Response disableNonPublicSearch = deleteSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        assertEquals(200, disableNonPublicSearch.getStatusCode());

    }

    @Test
    public void dataverseCategory() {
        Response enableNonPublicSearch = enableSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        assertEquals(200, enableNonPublicSearch.getStatusCode());

        /**
         * Unfortunately, it appears that the ability to specify the category of
         * a dataverse when creating it is a GUI-only feature. It can't
         * currently be done via the API, to our knowledge. You also can't tell
         * from the API which category was persisted but it always seems to be
         * "UNCATEGORIZED"
         */
        TestDataverse dataverseToCreate = new TestDataverse(dv1, "dv1", Dataverse.DataverseType.ORGANIZATIONS_INSTITUTIONS);
        Response createDvResponse = createDataverse(dataverseToCreate, homer);
        assertEquals(201, createDvResponse.getStatusCode());

        TestSearchQuery query = new TestSearchQuery("dv1");
        Response searchResponse = search(query, homer);
        JsonPath jsonPath = JsonPath.from(searchResponse.body().asString());
        String dv1Category = jsonPath.get("data.facets." + SearchFields.DATAVERSE_CATEGORY).toString();
        String msg = "dv1Category: " + dv1Category;
        assertEquals("dv1Category: [null]", msg);

        Response disableNonPublicSearch = deleteSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        assertEquals(200, disableNonPublicSearch.getStatusCode());
    }

    @AfterClass
    public static void cleanup() {

        boolean enabled = true;
        if (!enabled) {
            return;
        }

        logger.info("Running cleanup...");

        /**
         * We revoke roles here just in case an assertion failed because role
         * assignments are currently not deleted when you delete a user per
         * https://github.com/IQSS/dataverse/issues/1929
         *
         * You can also delete the role assignments manually like this:
         *
         * "DELETE FROM roleassignment WHERE assigneeidentifier='@ned';"
         */
        Response revokeNedAdminOnRoot = revokeRole(dataverseToCreateDataset1In, nedAdminOnRootAssignment, homer.getApiToken());
//        System.out.println(revokeNedAdminOnRoot.prettyPrint());
        System.out.println("cleanup - status code revoking admin on root from ned: " + revokeNedAdminOnRoot.getStatusCode());

        if (!homerGivesNedPermissionAtRootTestDisabled) {
            Response deleteDataset1Response = deleteDataset(dataset1, homer.getApiToken());
            assertEquals(204, deleteDataset1Response.getStatusCode());
        }

        if (!homerGivesNedPermissionAtNewDvDisabled) {
            Response deleteDataset2Response = deleteDataset(dataset2, homer.getApiToken());
            assertEquals(204, deleteDataset2Response.getStatusCode());
        }

        Response deleteDv1Response = deleteDataverse(dv1, homer);
        assertEquals(200, deleteDv1Response.getStatusCode());

        if (!homerGivesNedPermissionAtNewDvDisabled) {
            Response deleteDv2Response = deleteDataverse(dv2, homer);
            assertEquals(200, deleteDv2Response.getStatusCode());
        }

        deleteUser(homer.getUsername());
        deleteUser(ned.getUsername());
    }

    private Response enableSetting(SettingsServiceBean.Key settingKey) {
        Response response = given().body("true").when().put("/api/admin/settings/" + settingKey);
        return response;
    }

    private Response deleteSetting(SettingsServiceBean.Key settingKey) {
        Response response = given().when().delete("/api/admin/settings/" + settingKey);
        return response;
    }

    private Response checkSetting(SettingsServiceBean.Key settingKey) {
        Response response = given().when().get("/api/admin/settings/" + settingKey);
        return response;
    }

    private static String getDataverseAlias(long dataverseId, String apiToken) {
        Response getDataverse = given()
                .get("api/dataverses/" + dataverseId + "?key=" + apiToken);
        JsonPath jsonPath = JsonPath.from(getDataverse.body().asString());
        String dataverseAlias = jsonPath.get("data.alias");
        return dataverseAlias;
    }

    private static Response createDataverse(TestDataverse dataverseToCreate, TestUser creator) {
        JsonArrayBuilder contactArrayBuilder = Json.createArrayBuilder();
        contactArrayBuilder.add(Json.createObjectBuilder().add("contactEmail", creator.getEmail()));
        JsonArrayBuilder subjectArrayBuilder = Json.createArrayBuilder();
        subjectArrayBuilder.add("Other");
        JsonObject dvData = Json.createObjectBuilder()
                .add("alias", dataverseToCreate.alias)
                .add("name", dataverseToCreate.name)
                .add("dataverseContacts", contactArrayBuilder)
                .add("dataverseSubjects", subjectArrayBuilder)
                .build();
        Response createDataverseResponse = given()
                .body(dvData.toString()).contentType(ContentType.JSON)
                .when().post("/api/dataverses/:root?key=" + creator.apiToken);
        return createDataverseResponse;
    }

    private Response createDataset(String xmlIn, String dataverseToCreateDatasetIn, String apiToken) {
        Response createDatasetResponse = given()
                .auth().basic(apiToken, EMPTY_STRING)
                .body(xmlIn)
                .contentType("application/atom+xml")
                .post("/dvn/api/data-deposit/v1.1/swordv2/collection/dataverse/" + dataverseToCreateDatasetIn);
        return createDatasetResponse;
    }

    private Response querySolr(String query) {
        Response querySolrResponse = given().get("http://localhost:8983/solr/collection1/select?wt=json&indent=true&q=" + query);
        return querySolrResponse;
    }

    private static JsonObject createUser(String jsonStr) {
        JsonObjectBuilder createdUser = Json.createObjectBuilder();
        Response response = createUserViaApi(jsonStr, getPassword(jsonStr));
//        System.out.println(response.prettyPrint());
        Assert.assertEquals(200, response.getStatusCode());
        JsonPath jsonPath = JsonPath.from(response.body().asString());
        createdUser.add(idKey, jsonPath.getInt("data.user." + idKey));
        createdUser.add(usernameKey, jsonPath.get("data.user." + usernameKey).toString());
        createdUser.add(apiTokenKey, jsonPath.get("data." + apiTokenKey).toString());
        return createdUser.build();
    }

    private static String getPassword(String jsonStr) {
        String password = JsonPath.from(jsonStr).get(usernameKey);
        return password;
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

    private static String getEmailFromUserName(String username) {
        return username + "@mailinator.com";
    }

    private static Response createUserViaApi(String jsonStr, String password) {
        Response response = given().body(jsonStr).contentType(ContentType.JSON).when().post("/api/builtin-users?key=" + builtinUserKey + "&password=" + password);
        return response;
    }

    private static Response makeSuperuser(String userToMakeSuperuser) {
        Response response = given().post("/api/admin/superuser/" + userToMakeSuperuser);
        return response;
    }

    private Response grantRole(String definitionPoint, String role, String roleAssignee, String apiToken) {
        JsonObjectBuilder roleBuilder = Json.createObjectBuilder();
        roleBuilder.add("assignee", "@" + roleAssignee);
        roleBuilder.add("role", role);
        JsonObject roleObject = roleBuilder.build();
        System.out.println("Granting role on dataverse alias \"" + definitionPoint + "\": " + roleObject);
        return given()
                .body(roleObject).contentType(ContentType.JSON)
                .post("api/dataverses/" + definitionPoint + "/assignments?key=" + apiToken);
    }

    private static Response revokeRole(String definitionPoint, long doomed, String apiToken) {
        System.out.println("Attempting to revoke role assignment id " + doomed);
        /**
         * OUTPUT=`curl -s -X DELETE
         * "http://localhost:8080/api/dataverses/$BIRDS_DATAVERSE/assignments/$SPRUCE_ADMIN_ON_BIRDS?key=$FINCHKEY"`
         */
        return given()
                .delete("api/dataverses/" + definitionPoint + "/assignments/" + doomed + "?key=" + apiToken);
    }

    private String getGlobalId(Response createDatasetResponse) {
        String xml = createDatasetResponse.body().asString();
        String datasetSwordIdUrl = from(xml).get("entry.id");
        /**
         * @todo stop assuming the last 22 characters are the doi/globalId
         */
        return datasetSwordIdUrl.substring(datasetSwordIdUrl.length() - 22);
    }

    /**
     * Assumes you have turned on experimental non-public search
     * https://github.com/IQSS/dataverse/issues/1299
     *
     * curl -X PUT -d true
     * http://localhost:8080/api/admin/settings/:SearchApiNonPublicAllowed
     *
     * @return The Integer found or null.
     */
    private static Integer findDatasetIdFromGlobalId(String globalId, String apiToken) {
        Response searchForGlobalId = given()
                .get("api/search?key=" + apiToken
                        + "&q=dsPersistentId:\""
                        + globalId.replace(":", "\\:")
                        + "\"&show_entity_ids=true");
        JsonPath jsonPath = JsonPath.from(searchForGlobalId.body().asString());
        int id;
        try {
            id = jsonPath.get("data.items[0].entity_id");
        } catch (IllegalArgumentException ex) {
            return null;
        }
        return id;
    }

    private String getDatasetXml(String title, String author, String description) {
        String xmlIn = "<?xml version=\"1.0\"?>\n"
                + "<entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
                + "   <dcterms:title>" + title + "</dcterms:title>\n"
                + "   <dcterms:creator>" + author + "</dcterms:creator>\n"
                + "   <dcterms:description>" + description + "</dcterms:description>\n"
                + "</entry>\n"
                + "";
        return xmlIn;
    }

    private static Response deleteDataverse(String doomed, TestUser user) {
//        System.out.println("deletingn dataverse " + doomed);
        return given().delete("/api/dataverses/" + doomed + "?key=" + user.getApiToken());
    }

    private static Response deleteDataset(String globalId, String apiToken) {
        return given()
                .auth().basic(apiToken, EMPTY_STRING)
                .relaxedHTTPSValidation()
                .delete("/dvn/api/data-deposit/v1.1/swordv2/edit/study/" + globalId);
    }

    private static void deleteUser(String username) {
        Response deleteUserResponse = given().delete("/api/admin/authenticatedUsers/" + username + "/");
        assertEquals(200, deleteUserResponse.getStatusCode());
    }

    private long getRoleAssignmentId(Response response) {
        JsonPath jsonPath = JsonPath.from(response.body().asString());
        return jsonPath.getInt("data.id");
    }

    private Integer printDatasetId(String dataset1, TestUser user) {
        Integer datasetIdFound = findDatasetIdFromGlobalId(dataset1, user.getApiToken());
        System.out.println(dataset1 + " id " + datasetIdFound + " found by " + user);
        return datasetIdFound;
    }

    private Response search(TestSearchQuery query, TestUser user) {
        return given()
                .get("api/search?key=" + user.getApiToken()
                        + "&q=" + query.getQuery()
                        + "&show_facets=" + true
                );
    }

    private Response uploadZipFile(String persistentId, String zipFileName, String apiToken) throws FileNotFoundException {
        String pathToFileName = "scripts/search/data/binary/" + zipFileName;
        Response swordStatementResponse = given()
                .multiPart(new File(pathToFileName))
                .header("Packaging", "http://purl.org/net/sword/package/SimpleZip")
                .header("Content-Disposition", "filename=" + zipFileName)
                .auth().basic(apiToken, EMPTY_STRING)
                .post("/dvn/api/data-deposit/v1.1/swordv2/edit-media/study/" + persistentId);
        return swordStatementResponse;
    }

    private List<Integer> getIdsOfFilesUploaded(String persistentId, Integer datasetId, String apiToken) {
        getSwordStatement(persistentId, apiToken);
        if (datasetId != null) {
            List fileList = getFilesFromDatasetEndpoint(datasetId, apiToken);
            if (!fileList.isEmpty()) {
                return fileList;
            }
        }
        return Collections.emptyList();
    }

    private Response getSwordStatement(String persistentId, String apiToken) {
        Response swordStatementResponse = given()
                .auth().basic(apiToken, EMPTY_STRING)
                .get("/dvn/api/data-deposit/v1.1/swordv2/statement/study/" + persistentId);
        return swordStatementResponse;
    }

    private List getFilesFromDatasetEndpoint(Integer datasetId, String apiToken) {
        List<Integer> fileList = new ArrayList<>();
        Response getDatasetFilesResponse = given()
                .get("api/datasets/" + datasetId + "/versions/:latest/files?key=" + apiToken);
        JsonPath jsonPath = JsonPath.from(getDatasetFilesResponse.body().asString());
//        Integer fileId = jsonPath.get("data[0].datafile.id");
        Map dataFiles = jsonPath.get("data[0]");
        if (dataFiles != null) {
            Map datafile = (Map) dataFiles.get("datafile");
            if (datafile != null) {
                Integer fileId = (Integer) datafile.get("id");
                if (fileId != null) {
                    fileList.add(fileId);
                }
            }
        }
        return fileList;
    }

    private void checkPermissionsOnDvObject(int dvObjectId, String apiToken) {
        Response debugPermsResponse = given()
                .get("api/admin/index/permsDebug/?id=" + dvObjectId + "&key=" + apiToken);
//        debugPermsResponse.prettyPrint();
    }

    private static class TestUser {

        private long id;
        private String username;
        private String apiToken;

        private TestUser(JsonObject json) {
            this.id = json.getInt(idKey);
            this.username = json.getString(usernameKey);
            this.apiToken = json.getString(apiTokenKey);
        }

        public long getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getApiToken() {
            return apiToken;
        }

        public String getEmail() {
            return getEmailFromUserName(username);
        }

        @Override
        public String toString() {
            return "TestUser{" + "id=" + id + ", username=" + username + '}';
        }

    }

    private static class TestDataverse {

        String alias;
        String name;
        Dataverse.DataverseType category;

        public TestDataverse(String alias, String name, Dataverse.DataverseType category) {
            this.alias = alias;
            this.name = name;
            this.category = category;
        }

    }

    private static class TestSearchQuery {

        private String query;
        private List<String> filterQueries = new ArrayList<>();

        private TestSearchQuery(String query) {
            this.query = query;
        }

        public TestSearchQuery(String query, List<String> filterQueries) {
            this.query = query;
            if (!filterQueries.isEmpty()) {
                this.filterQueries = filterQueries;
            }
        }

        public String getQuery() {
            return query;
        }

        public List<String> getFilterQueries() {
            return filterQueries;
        }

    }
}
