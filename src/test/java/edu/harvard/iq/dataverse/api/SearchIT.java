package edu.harvard.iq.dataverse.api;

import com.google.common.base.Stopwatch;
import static com.jayway.restassured.RestAssured.given;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.internal.path.xml.NodeChildrenImpl;
import com.jayway.restassured.path.json.JsonPath;
import static com.jayway.restassured.path.json.JsonPath.with;
import com.jayway.restassured.path.xml.XmlPath;
import static com.jayway.restassured.path.xml.XmlPath.from;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Thread.sleep;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
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
    private static final String keyString = "X-Dataverse-key";
    private static final String EMPTY_STRING = "";
    private static final String idKey = "id";
    private static final String apiTokenKey = "apiToken";
    private static final String usernameKey = "userName";
    private static final String emailKey = "email";
    private static TestUser homer;
    private static TestUser ned;
    private static TestUser clancy;
    private static final String categoryTestDataverse = "categoryTestDataverse";
    private static final String dvForPermsTesting = "dvForPermsTesting";
    private static String dataset1;
    private static String dataset2;
    private static String dataset3;
    private static Integer dataset2Id;
    private static Integer dataset3Id;
    private static long nedAdminOnRootAssignment;
    private static String dataverseToCreateDataset1In = "root";
    /**
     * @todo Figure out why we sometimes get database deadlocks when all tests
     * are enabled: https://github.com/IQSS/dataverse/issues/2460
     */
    private static final boolean disableTestPermsonRootDv = false;
    private static final boolean disableTestPermsOnNewDv = false;
    private static final boolean homerPublishesVersion2AfterDeletingFile = false;
    private static final boolean disableTestCategory = false;
    private Stopwatch timer;
    private boolean haveToUseCurlForUpload = true;

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
        int homerIdFromDatabase = getUserIdFromDatabase(homer.getUsername());
        if (homerIdFromDatabase != homer.getId()) {
            // should never reach here: https://github.com/IQSS/dataverse/issues/2418
            homer.setId(homerIdFromDatabase);
        }

        Response makeSuperUserResponse = makeSuperuser(homer.getUsername());
        assertEquals(200, makeSuperUserResponse.getStatusCode());

        JsonObject nedJsonObject = createUser(getUserAsJsonString("ned", "Ned", "Flanders"));
        ned = new TestUser(nedJsonObject);
        int nedIdFromDatabase = getUserIdFromDatabase(ned.getUsername());
        if (nedIdFromDatabase != ned.getId()) {
            // should never reach here: https://github.com/IQSS/dataverse/issues/2418
            ned.setId(nedIdFromDatabase);
        }

        JsonObject clancyJsonObject = createUser(getUserAsJsonString("clancy", "Clancy", "Wiggum"));
        clancy = new TestUser(clancyJsonObject);
        int clancyIdFromDatabase = getUserIdFromDatabase(clancy.getUsername());
        if (clancyIdFromDatabase != clancy.getId()) {
            // should never reach here: https://github.com/IQSS/dataverse/issues/2418
            clancy.setId(clancyIdFromDatabase);
        }

    }

    @Test
    public void homerGivesNedPermissionAtRoot() {

        if (disableTestPermsonRootDv) {
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

        if (haveToUseCurlForUpload) {
            Process uploadZipFileProcess = uploadZipFileWithCurl(dataset1, zipFileName, homer.getApiToken());
//            printCommandOutput(uploadZipFileProcess);
        } else {
            try {
                Response uploadZipFileResponse = uploadZipFile(dataset1, zipFileName, homer.getApiToken());
            } catch (FileNotFoundException ex) {
                System.out.println("Problem uploading " + zipFileName + ": " + ex.getMessage());
            }
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

        if (disableTestPermsOnNewDv) {
            return;
        }

        Response enableNonPublicSearch = enableSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        assertEquals(200, enableNonPublicSearch.getStatusCode());

        TestDataverse dataverseToCreate = new TestDataverse(dvForPermsTesting, dvForPermsTesting, Dataverse.DataverseType.ORGANIZATIONS_INSTITUTIONS);
        Response createDvResponse = createDataverse(dataverseToCreate, homer);
        assertEquals(201, createDvResponse.getStatusCode());

        String xmlIn = getDatasetXml(homer.getUsername(), homer.getUsername(), homer.getUsername());
        Response createDataset1Response = createDataset(xmlIn, dvForPermsTesting, homer.getApiToken());
        assertEquals(201, createDataset1Response.getStatusCode());

        dataset2 = getGlobalId(createDataset1Response);

        Integer datasetIdHomerFound = printDatasetId(dataset2, homer);
        assertEquals(true, datasetIdHomerFound != null);
        dataset2Id = datasetIdHomerFound;

        Map<String, String> datasetTimestampsAfterCreate = checkPermissionsOnDvObject(datasetIdHomerFound, homer.apiToken).jsonPath().getMap("data.timestamps", String.class, String.class);
        assertEquals(true, datasetTimestampsAfterCreate.get(Index.contentChanged) != null);
        assertEquals(true, datasetTimestampsAfterCreate.get(Index.contentIndexed) != null);
        assertEquals(true, datasetTimestampsAfterCreate.get(Index.permsChanged) != null);
        assertEquals(true, datasetTimestampsAfterCreate.get(Index.permsIndexed) != null);

//        String zipFileName = "noSuchFile.zip";
        String zipFileName = "trees.zip";
//        String zipFileName = "100files.zip";
//        String zipFileName = "1000files.zip";

        timer = Stopwatch.createStarted();
        if (haveToUseCurlForUpload) {
            Process uploadZipFileProcess = uploadZipFileWithCurl(dataset2, zipFileName, homer.getApiToken());
//            printCommandOutput(uploadZipFileProcess);
        } else {
            Response uploadZipFileResponse;
            try {
                uploadZipFileResponse = uploadZipFile(dataset2, zipFileName, homer.getApiToken());
            } catch (FileNotFoundException ex) {
                System.out.println("Problem uploading " + zipFileName + ": " + ex.getMessage());
            }
        }
        System.out.println("Uploading zip file took " + timer.stop());

        List<Integer> idsOfFilesUploaded = getIdsOfFilesUploaded(dataset2, datasetIdHomerFound, homer.getApiToken());
        int numFilesFound = idsOfFilesUploaded.size();
        System.out.println("num files found: " + numFilesFound);

        Integer idNedFoundBeforeRoleGranted = printDatasetId(dataset2, ned);
        assertEquals(null, idNedFoundBeforeRoleGranted);

        String roleToAssign = "admin";
        timer = Stopwatch.createStarted();
        Response grantNedAdmin = grantRole(dvForPermsTesting, roleToAssign, ned.getUsername(), homer.getApiToken());
//        System.out.println(grantNedAdmin.prettyPrint());
        System.out.println("granting role took " + timer.stop());
        assertEquals(200, grantNedAdmin.getStatusCode());

        Integer idNedFoundAfterRoleGranted = printDatasetId(dataset2, ned);
        assertEquals(datasetIdHomerFound, idNedFoundAfterRoleGranted);

        clearIndexTimesOnDvObject(datasetIdHomerFound);
        reindexDataset(datasetIdHomerFound);
        Map<String, String> datasetTimestampsAfterReindex = checkPermissionsOnDvObject(datasetIdHomerFound, homer.apiToken).jsonPath().getMap("data.timestamps", String.class, String.class);
        assertEquals(true, datasetTimestampsAfterReindex.get(Index.contentChanged) != null);
        assertEquals(true, datasetTimestampsAfterReindex.get(Index.contentIndexed) != null);
        assertEquals(true, datasetTimestampsAfterReindex.get(Index.permsChanged) != null);
        assertEquals(true, datasetTimestampsAfterReindex.get(Index.permsIndexed) != null);

        if (!idsOfFilesUploaded.isEmpty()) {
            Random random = new Random();
            int randomFileIndex = random.nextInt(numFilesFound);
            System.out.println("picking random file with index of " + randomFileIndex + " from list of " + numFilesFound);

            int randomFileId = idsOfFilesUploaded.get(randomFileIndex);

            Set<String> expectedSet = new HashSet<>();
            expectedSet.add(IndexServiceBean.getGroupPerUserPrefix() + homer.getId());
            expectedSet.add(IndexServiceBean.getGroupPerUserPrefix() + ned.getId());

            Response checkPermsReponse = checkPermissionsOnDvObject(randomFileId, homer.getApiToken());
//            checkPermsReponse.prettyPrint();
            // [0] because there's only one "permissions" Solr doc (a draft)
            List<String> permListFromDebugEndpoint = JsonPath.from(checkPermsReponse.getBody().asString()).get("data.perms[0]." + SearchFields.DISCOVERABLE_BY);
            Set<String> setFoundFromPermsDebug = new TreeSet<>();
            for (String perm : permListFromDebugEndpoint) {
                setFoundFromPermsDebug.add(perm);
            }
            Map<String, String> timeStamps = JsonPath.from(checkPermsReponse.getBody().asString()).get("data.timestamps");
            for (Map.Entry<String, String> entry : timeStamps.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                System.out.println(key + ":" + value);
            }

            assertEquals(expectedSet, setFoundFromPermsDebug);

            Response solrQueryPerms = querySolr(SearchFields.DEFINITION_POINT_DVOBJECT_ID + ":" + randomFileId);
//            solrQueryPerms.prettyPrint();

            Set<String> setFoundFromSolr = new TreeSet<>();
            List<String> perms = JsonPath.from(solrQueryPerms.getBody().asString()).getList("response.docs[0]." + SearchFields.DISCOVERABLE_BY);
            for (String perm : perms) {
                setFoundFromSolr.add(perm);
            }
//            System.out.println(setFoundFromSolr + " found");
            assertEquals(expectedSet, setFoundFromSolr);

            Response solrQueryContent = querySolr(SearchFields.ENTITY_ID + ":" + randomFileId);
//            solrQueryContent.prettyPrint();
        }

        long rootDataverseId = 1;
        String rootDataverseAlias = getDataverseAlias(rootDataverseId, homer.getApiToken());
        Response publishRootDataverseResponse = publishDataverseAsCreator(rootDataverseId);
//        publishRootDataverseResponse.prettyPrint();

        Response publishDataverseResponse = publishDataverse(dvForPermsTesting, homer.apiToken);
//        publishDataverseResponse.prettyPrint();

        Response publishDatasetResponse = publishDatasetViaNative(datasetIdHomerFound, homer.apiToken);
//        publishDatasetResponse.prettyPrint();

        Integer idClancyFoundAfterPublished = printDatasetId(dataset2, clancy);
        assertEquals(datasetIdHomerFound, idClancyFoundAfterPublished);

        if (!idsOfFilesUploaded.isEmpty()) {
            Random random = new Random();
            int randomFileIndex = random.nextInt(numFilesFound);
            System.out.println("picking random file with index of " + randomFileIndex + " from list of " + numFilesFound);

            int randomFileId = idsOfFilesUploaded.get(randomFileIndex);

            Set<String> expectedSet = new HashSet<>();
            expectedSet.add(IndexServiceBean.getPublicGroupString());

            Response checkPermsReponse = checkPermissionsOnDvObject(randomFileId, homer.getApiToken());
//            checkPermsReponse.prettyPrint();
            // [0] because there's only one "permissions" Solr doc (a published file)
            List<String> permListFromDebugEndpoint = JsonPath.from(checkPermsReponse.getBody().asString()).get("data.perms[0]." + SearchFields.DISCOVERABLE_BY);
            Set<String> setFoundFromPermsDebug = new TreeSet<>();
            for (String perm : permListFromDebugEndpoint) {
                setFoundFromPermsDebug.add(perm);
            }

            assertEquals(expectedSet, setFoundFromPermsDebug);

            Response solrQueryPerms = querySolr(SearchFields.DEFINITION_POINT_DVOBJECT_ID + ":" + randomFileId);
//            solrQueryPerms.prettyPrint();

            Set<String> setFoundFromSolr = new TreeSet<>();
            String publishedId = IndexServiceBean.solrDocIdentifierFile + randomFileId + IndexServiceBean.discoverabilityPermissionSuffix;
            List<Map> docs = with(solrQueryPerms.getBody().asString()).param("name", publishedId).get("response.docs.findAll { docs -> docs.id == name }");
            List<String> permsPublished = with(solrQueryPerms.getBody().asString()).param("name", publishedId).getList("response.docs.findAll { docs -> docs.id == name }[0]." + SearchFields.DISCOVERABLE_BY);

            for (String perm : permsPublished) {
                setFoundFromSolr.add(perm);
            }
            assertEquals(expectedSet, setFoundFromSolr);

            String draftId = IndexServiceBean.solrDocIdentifierFile + randomFileId + IndexServiceBean.draftSuffix + IndexServiceBean.discoverabilityPermissionSuffix;
            /**
             * @todo The fact that we're able to find the permissions document
             * for a file that has been published is a bug. It should be
             * deleted, ideally, when the dataset goes from draft to published.
             */
            List<String> permsFormerDraft = with(solrQueryPerms.getBody().asString()).param("name", draftId).getList("response.docs.findAll { docs -> docs.id == name }[0]." + SearchFields.DISCOVERABLE_BY);
//            System.out.println("permsDraft: " + permsFormerDraft);

            Response solrQueryContent = querySolr(SearchFields.ENTITY_ID + ":" + randomFileId);
//            solrQueryContent.prettyPrint();
        }

        Response disableNonPublicSearch = deleteSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        assertEquals(200, disableNonPublicSearch.getStatusCode());

    }

    @Test
    public void homerPublishesVersion2AfterDeletingFile() throws InterruptedException {
        if (homerPublishesVersion2AfterDeletingFile) {
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
        Response createDatasetResponse = createDataset(xmlIn, dataverseToCreateDataset1In, homer.getApiToken());
//        createDatasetResponse.prettyPrint();
        assertEquals(201, createDatasetResponse.getStatusCode());
        dataset3 = getGlobalId(createDatasetResponse);
//        System.out.println("dataset persistent id: " + dataset3);

        String zipFileName = "3files.zip";

        Process uploadZipFileProcess = uploadZipFileWithCurl(dataset3, zipFileName, homer.getApiToken());
//        printCommandOutput(uploadZipFileProcess);

        sleep(200);
        Integer datasetIdHomerFound = printDatasetId(dataset3, homer);
        assertEquals(true, datasetIdHomerFound != null);
        dataset3Id = datasetIdHomerFound;
        List<Integer> idsOfFilesUploaded = getIdsOfFilesUploaded(dataset3, datasetIdHomerFound, homer.getApiToken());
//        System.out.println("file IDs: " + idsOfFilesUploaded);

        Set<String> expectedInitialFilesHomer = new HashSet<String>() {
            {
                add("file1.txt");
                add("file2.txt");
                add("file3.txt");
            }
        };
        String DRAFT = "DRAFT";
        Response fileDataBeforePublishingV1Homer = getFileSearchData(dataset3, DRAFT, homer.getApiToken());
//        System.out.println("Files before publishing 1.0 as seen by creator...");
//        fileDataBeforePublishingV1Homer.prettyPrint();
        Set<String> actualInitialFilesHomer = getFileData(fileDataBeforePublishingV1Homer);
        assertEquals(expectedInitialFilesHomer, actualInitialFilesHomer);

//        System.out.println("Files before publishing 1.0 as seen by non-creator...");
        Response fileDataBeforePublishingV1Ned = getFileSearchData(dataset3, DRAFT, ned.getApiToken());
//        fileDataBeforePublishingV1Ned.prettyPrint();
        Set<String> actualInitialFilesed = getFileData(fileDataBeforePublishingV1Ned);
        assertEquals(new HashSet<String>(), actualInitialFilesed);

        Response publishDatasetResponse = publishDatasetViaSword(dataset3, homer.getApiToken());
//        publishDatasetResponse.prettyPrint();
        Response datasetAsJson = getDatasetAsJson(dataset3Id, homer.getApiToken());
//        datasetAsJson.prettyPrint();

//        Response fileDataAfterPublishingV1Ned = getFileSearchData(dataset3, ned.getApiToken());
        Response fileDataAfterPublishingV1Guest = getFileSearchData(dataset3, DRAFT, EMPTY_STRING);
//        System.out.println("Files after publishing 1.0 as seen by non-creator...");
//        fileDataAfterPublishingV1Guest.prettyPrint();
        Set<String> actualFilesAfterPublishingV1Guest = getFileData(fileDataAfterPublishingV1Guest);
        assertEquals(expectedInitialFilesHomer, actualFilesAfterPublishingV1Guest);

//        getSwordStatement(dataset3, homer.getApiToken()).prettyPrint();
//        List<String> getfiles = getFileNameFromSearchDebug(dataset3, homer.getApiToken());
//        System.out.println("some files: " + getfiles);
        Response datasetFiles = getDatasetFilesEndpoint(dataset3Id, homer.getApiToken());
//        datasetFiles.prettyPrint();
        String fileToDelete = "file2.txt";

//        getSwordStatement(dataset3, homer.getApiToken()).prettyPrint();
//        System.out.println("### BEFORE TOUCHING PUBLISHED DATASET");
        Response atomEntryBeforeDeleteReponse = getSwordAtomEntry(dataset3, homer.getApiToken());
//        atomEntryBeforeDeleteReponse.prettyPrint();
        /**
         * @todo The "SWORD: deleting a file from a published version (not a
         * draft) creates a draft but doesn't delete the file" bug at
         * https://github.com/IQSS/dataverse/issues/2464 means we must first
         * create a draft via the "update metadata" endpoint before deleting the
         * file. Otherwise, the file won't be properly deleted!
         */
        System.out.println("Updating metadata before delete because of https://github.com/IQSS/dataverse/issues/2464");
        Response updateMetadataResponse = updateDatasetMetadataViaSword(dataset3, xmlIn, homer.getApiToken());
//        updateMetadataResponse.prettyPrint();
//        System.out.println("### AFTER UPDATING METADATA");
        Response atomEntryAfterDeleteReponse = getSwordAtomEntry(dataset3, homer.getApiToken());
//        atomEntryAfterDeleteReponse.prettyPrint();
        int fileId = getFileIdFromDatasetEndpointFileListing(datasetFiles, fileToDelete);
        Response deleteFileResponse = deleteFile(fileId, homer.getApiToken());
//        deleteFileResponse.prettyPrint();
        assertEquals(204, deleteFileResponse.statusCode());
//        System.out.println("### AFTER DELETING FILE");
        Response swordStatementAfterDelete = getSwordStatement(dataset3, homer.getApiToken());
//        swordStatementAfterDelete.prettyPrint();
        XmlPath xmlPath = new XmlPath(swordStatementAfterDelete.body().asString());
        String firstFileName = xmlPath.get("feed.entry[0].id").toString().split("/")[11];
//        System.out.println("first file name:" + firstFileName);
        String secondFileName = xmlPath.get("feed.entry[1].id").toString().split("/")[11];
//        System.out.println("second file name: " + secondFileName);
        Set<String> filesFoundInSwordStatement = new HashSet<>();
        filesFoundInSwordStatement.add(firstFileName);
        filesFoundInSwordStatement.add(secondFileName);
        Set<String> expectedFilesInSwordStatementAfterDelete = new HashSet<String>() {
            {
                add("file1.txt");
                add("file3.txt");
            }
        };
        assertEquals(expectedFilesInSwordStatementAfterDelete, filesFoundInSwordStatement);

        NodeChildrenImpl thirdFileNode = xmlPath.get("feed.entry[2].id");
        /**
         * If you get "java.lang.String cannot be cast to
         * com.jayway.restassured.internal.path.xml.NodeChildrenImpl" here it
         * means that the third file was found and not deleted! See the note
         * above about https://github.com/IQSS/dataverse/issues/2464
         */
        assertEquals(true, thirdFileNode.isEmpty());

        Set<String> expectedV1FilesAfterDeleteGuest = new HashSet<String>() {
            {
                add("file1.txt");
                add("file2.txt");
                add("file3.txt");
            }
        };
        String v1dot0 = "1.0";
        Response fileDataAfterDelete = getFileSearchData(dataset3, v1dot0, EMPTY_STRING);
//        System.out.println("Files guest sees after Homer deletes a file from 1.0, creating a draft...");
//        fileDataAfterDelete.prettyPrint();
        Set<String> actualFilesAfterDelete = getFileData(fileDataAfterDelete);
        assertEquals(expectedV1FilesAfterDeleteGuest, actualFilesAfterDelete);

        Set<String> expectedDraftFilesAfterDeleteHomerAfterIssue2455Implemented = expectedFilesInSwordStatementAfterDelete;
        Response fileDataAfterDeleteHomer = getFileSearchData(dataset3, DRAFT, homer.getApiToken());
//        System.out.println("Files Homer sees in draft after deleting a file from v1.0...");
//        fileDataAfterDeleteHomer.prettyPrint();
        Set<String> actualDraftFilesAfterDeleteHomer = getFileData(fileDataAfterDeleteHomer);
        Response querySolrResponse = querySolr(SearchFields.PARENT_ID + ":" + dataset3Id);
//        querySolrResponse.prettyPrint();
        logger.info("files found: " + JsonPath.from(querySolrResponse.asString()).get("response.docs.name").toString());

        /**
         * @todo In order for this test to pass we'll probably need to change
         * the indexing rules defined in "Only show draft file card if file has
         * changed from published version"
         * https://github.com/IQSS/dataverse/issues/528 . From the "Use Solr for
         * file listing on dataset page" issue at
         * https://github.com/IQSS/dataverse/issues/2455 we'd like Homer to be
         * able to look at a post v1 draft and see that one of his three files
         * has been deleted in that draft. With current indexing rules, this is
         * not possible. There are only three files indexed into Solr and they
         * all belong to the publish v1 dataset. We don't index drafts unless
         * the content has changed (again per issue 528).
         */
        System.out.println(new TreeSet(expectedDraftFilesAfterDeleteHomerAfterIssue2455Implemented) + " expected after issue 2455 implemented");
        System.out.println(new TreeSet(actualDraftFilesAfterDeleteHomer) + " actual");
//        assertEquals(expectedDraftFilesAfterDeleteHomer, actualDraftFilesAfterDeleteHomer);

        Response disableNonPublicSearch = deleteSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        assertEquals(200, disableNonPublicSearch.getStatusCode());

    }

    @Test
    public void dataverseCategory() {

        if (disableTestCategory) {
            return;
        }

        Response enableNonPublicSearch = enableSetting(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
        assertEquals(200, enableNonPublicSearch.getStatusCode());

        /**
         * Unfortunately, it appears that the ability to specify the category of
         * a dataverse when creating it is a GUI-only feature. It can't
         * currently be done via the API, to our knowledge. You also can't tell
         * from the API which category was persisted but it always seems to be
         * "UNCATEGORIZED"
         */
        TestDataverse dataverseToCreate = new TestDataverse(categoryTestDataverse, categoryTestDataverse, Dataverse.DataverseType.ORGANIZATIONS_INSTITUTIONS);
        Response createDvResponse = createDataverse(dataverseToCreate, homer);
        assertEquals(201, createDvResponse.getStatusCode());

        TestSearchQuery query = new TestSearchQuery(categoryTestDataverse);
        Response searchResponse = search(query, homer);
//        searchResponse.prettyPrint();
        JsonPath jsonPath = JsonPath.from(searchResponse.body().asString());
        String category = jsonPath.get("data.facets." + SearchFields.DATAVERSE_CATEGORY).toString();
        String msg = "category: " + category;
        assertEquals("category: [null]", msg);

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
//        Response revokeNedAdminOnRoot = revokeRole(dataverseToCreateDataset1In, nedAdminOnRootAssignment, homer.getApiToken());
//        System.out.println(revokeNedAdminOnRoot.prettyPrint());
//        System.out.println("cleanup - status code revoking admin on root from ned: " + revokeNedAdminOnRoot.getStatusCode());
        /**
         *
         */
        if (!disableTestPermsonRootDv) {
            Response deleteDataset1Response = deleteDataset(dataset1, homer.getApiToken());
            assertEquals(204, deleteDataset1Response.getStatusCode());
        }

        if (!disableTestPermsOnNewDv) {
            Response destroyDatasetResponse = destroyDataset(dataset2Id, homer.getApiToken());
            assertEquals(200, destroyDatasetResponse.getStatusCode());
        }

        if (!homerPublishesVersion2AfterDeletingFile) {
            Response destroyDataset = destroyDataset(dataset3Id, homer.getApiToken());
            assertEquals(200, destroyDataset.getStatusCode());
        }

        if (!disableTestCategory) {
            Response deleteCategoryDataverseResponse = deleteDataverse(categoryTestDataverse, homer);
            assertEquals(200, deleteCategoryDataverseResponse.getStatusCode());
        }

        if (!disableTestPermsOnNewDv) {
            Response deleteDvResponse = deleteDataverse(dvForPermsTesting, homer);
            assertEquals(200, deleteDvResponse.getStatusCode());
        }

        deleteUser(homer.getUsername());
        deleteUser(ned.getUsername());
        deleteUser(clancy.getUsername());
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

    private Response updateDatasetMetadataViaSword(String persistentId, String xmlIn, String apiToken) {
        return given()
                .auth().basic(apiToken, EMPTY_STRING)
                .body(xmlIn)
                .contentType("application/atom+xml")
                .put("/dvn/api/data-deposit/v1.1/swordv2/edit/study/" + persistentId);
    }

    private Response querySolr(String query) {
        Response querySolrResponse = given().get("http://localhost:8983/solr/collection1/select?wt=json&indent=true&q=" + query);
        return querySolrResponse;
    }

    private static JsonObject createUser(String jsonStr) {
        JsonObjectBuilder createdUser = Json.createObjectBuilder();
        Response response = createUserViaApi(jsonStr, getPassword(jsonStr));
//        response.prettyPrint();
        Assert.assertEquals(200, response.getStatusCode());
        JsonPath jsonPath = JsonPath.from(response.body().asString());
        int userId = jsonPath.getInt("data.user." + idKey);
        createdUser.add(idKey, userId);
        String username = jsonPath.get("data.user." + usernameKey).toString();
        createdUser.add(usernameKey, username);
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

    private static Response destroyDataset(Integer datasetId, String apiToken) {
        return given()
                .header(keyString, apiToken)
                .delete("/api/datasets/" + datasetId + "/destroy");
    }

    private static void deleteUser(String username) {
        Response deleteUserResponse = given().delete("/api/admin/authenticatedUsers/" + username + "/");
        assertEquals(200, deleteUserResponse.getStatusCode());
    }

    private static int getUserIdFromDatabase(String username) {
        Response getUserResponse = given().get("/api/admin/authenticatedUsers/" + username + "/");
        JsonPath getUserJson = JsonPath.from(getUserResponse.body().asString());
        int userIdFromDatabase = getUserJson.getInt("data.id");
        return userIdFromDatabase;
    }

    private long getRoleAssignmentId(Response response) {
        JsonPath jsonPath = JsonPath.from(response.body().asString());
        return jsonPath.getInt("data.id");
    }

    private Integer printDatasetId(String dataset1, TestUser user) {
        Integer datasetIdFound = findDatasetIdFromGlobalId(dataset1, user.getApiToken());
//        System.out.println(dataset1 + " id " + datasetIdFound + " found by " + user);
        return datasetIdFound;
    }

    private Response search(TestSearchQuery query, TestUser user) {
        return given()
                .get("api/search?key=" + user.getApiToken()
                        + "&q=" + query.getQuery()
                        + "&show_facets=" + true
                );
    }

    /**
     * @todo Get this version that doesn't require curl working. Use body
     * instead of multiPart?
     */
    private Response uploadZipFile(String persistentId, String zipFileName, String apiToken) throws FileNotFoundException {
        String pathToFileName = "scripts/search/data/binary/" + zipFileName;
        Path path = Paths.get(pathToFileName);
        byte[] data = null;
        Response swordStatementResponse = given()
                .multiPart(new File(pathToFileName))
                .header("Packaging", "http://purl.org/net/sword/package/SimpleZip")
                .header("Content-Disposition", "filename=" + zipFileName)
                .auth().basic(apiToken, EMPTY_STRING)
                .post("/dvn/api/data-deposit/v1.1/swordv2/edit-media/study/" + persistentId);
        return swordStatementResponse;
    }

    /**
     * @todo Delete this once you get the REST-assured version working
     */
    private Process uploadZipFileWithCurl(String globalId, String zipfilename, String apiToken) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[]{"bash", "-c", "curl -s --insecure --data-binary @scripts/search/data/binary/" + zipfilename + " -H \"Content-Disposition: filename=trees.zip\" -H \"Content-Type: application/zip\" -H \"Packaging: http://purl.org/net/sword/package/SimpleZip\" -u " + apiToken + ": https://localhost:8181/dvn/api/data-deposit/v1.1/swordv2/edit-media/study/" + globalId});
        } catch (IOException ex) {
            Logger.getLogger(SearchIT.class.getName()).log(Level.SEVERE, null, ex);
        }
        return p;
    }

    private void printCommandOutput(Process p) {
        try {
            p.waitFor();
        } catch (InterruptedException ex) {
            Logger.getLogger(SearchIT.class.getName()).log(Level.SEVERE, null, ex);
        }
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        try {
            while ((line = input.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException ex) {
            Logger.getLogger(SearchIT.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            input.close();
        } catch (IOException ex) {
            Logger.getLogger(SearchIT.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private List<Integer> getIdsOfFilesUploaded(String persistentId, Integer datasetId, String apiToken) {
        Response swordStatentResponse = getSwordStatement(persistentId, apiToken);
//        swordStatentResponse.prettyPrint();
        if (datasetId != null) {
            List<Integer> fileList = getFilesFromDatasetEndpoint(datasetId, apiToken);
            if (!fileList.isEmpty()) {
                return fileList;
            }
        }
        return Collections.emptyList();
    }

    private Response getSwordAtomEntry(String persistentId, String apiToken) {
        Response response = given()
                .auth().basic(apiToken, EMPTY_STRING)
                .get("/dvn/api/data-deposit/v1.1/swordv2/edit/study/" + persistentId);
        return response;
    }

    private Response getSwordStatement(String persistentId, String apiToken) {
        Response swordStatementResponse = given()
                .auth().basic(apiToken, EMPTY_STRING)
                .get("/dvn/api/data-deposit/v1.1/swordv2/statement/study/" + persistentId);
        return swordStatementResponse;
    }

    private List<Integer> getFilesFromDatasetEndpoint(Integer datasetId, String apiToken) {
        List<Integer> fileList = new ArrayList<>();
        Response getDatasetFilesResponse = getDatasetFilesEndpoint(datasetId, apiToken);
//        getDatasetFilesResponse.prettyPrint();
        JsonPath jsonPath = JsonPath.from(getDatasetFilesResponse.body().asString());
        List<Map> filesMap = jsonPath.get("data.datafile");
        for (Map map : filesMap) {
            int fileId = (int) map.get("id");
            fileList.add(fileId);
        }
        return fileList;
    }

    private Response getDatasetFilesEndpoint(Integer datasetId, String apiToken) {
        Response getDatasetFilesResponse = given()
                .get("api/datasets/" + datasetId + "/versions/:latest/files?key=" + apiToken);
        return getDatasetFilesResponse;
    }

    private Response checkPermissionsOnDvObject(int dvObjectId, String apiToken) {
        Response debugPermsResponse = given()
                .get("api/admin/index/permsDebug/?id=" + dvObjectId + "&key=" + apiToken);
//        debugPermsResponse.prettyPrint();
        return debugPermsResponse;
    }

    private Response clearIndexTimesOnDvObject(int dvObjectId) {
        Response debugPermsResponse = given()
                .delete("api/admin/index/timestamps/" + dvObjectId);
        return debugPermsResponse;
    }

    private Response reindexDataset(int datasetId) {
        return given().get("api/admin/index/datasets/" + datasetId);
    }

    private Response publishDataverse(String alias, String apiToken) {
        return given()
                .header(keyString, apiToken)
                .urlEncodingEnabled(false)
                .post("/api/dataverses/" + alias + "/actions/:publish");
    }

    private Response publishDataverseAsCreator(long id) {
        return given()
                .post("/api/admin/publishDataverseAsCreator/" + id);
    }

    private Response getDatasetAsJson(long datasetId, String apiToken) {
        return given()
                .header(keyString, apiToken)
                .urlEncodingEnabled(false)
                .get("/api/datasets/" + datasetId);
    }

    private Response publishDatasetViaSword(String persistentId, String apiToken) {
        return given()
                .auth().basic(apiToken, EMPTY_STRING)
                .header("In-Progress", "false")
                .post("/dvn/api/data-deposit/v1.1/swordv2/edit/study/" + persistentId);
    }

    private Response publishDatasetViaNative(long datasetId, String apiToken) {
        /**
         * This should probably be a POST rather than a GET:
         * https://github.com/IQSS/dataverse/issues/2431
         *
         * Allows version less than v1.0 to be published (i.e. v0.1):
         * https://github.com/IQSS/dataverse/issues/2461
         *
         */
        return given()
                .header(keyString, apiToken)
                .urlEncodingEnabled(false)
                .get("/api/datasets/" + datasetId + "/actions/:publish?type=minor");
    }

    private Response getFileSearchData(String persistentId, String semanticVersion, String apiToken) {
        /**
         * Note In all commands below, dataset versions can be referred to as:
         *
         * :draft the draft version, if any
         *
         * :latest either a draft (if exists) or the latest published version.
         *
         * :latest-published the latest published version
         *
         * x.y a specific version, where x is the major version number and y is
         * the minor version number.
         *
         * x same as x.0
         *
         * http://guides.dataverse.org/en/latest/api/native-api.html#datasets
         */
//        String semanticVersion = null;
        return given()
                .header(keyString, apiToken)
                .urlEncodingEnabled(false)
                .get("/api/admin/index/filesearch?persistentId=" + persistentId + "&semanticVersion=" + semanticVersion);
    }

    private Response deleteFile(int fileId, String apiToken) {
//        System.out.println("deleting file id " + fileId);
        return given()
                .auth().basic(apiToken, EMPTY_STRING)
                .delete("/dvn/api/data-deposit/v1.1/swordv2/edit-media/file/" + fileId);
    }

    private List<String> getFileNameFromSearchDebug(String datasetPersistentId, String apiToken) {
        Response fileDataResponse = getFileSearchData(datasetPersistentId, "DRAFT", apiToken);
//        fileDataResponse.prettyPrint();
        return JsonPath.from(fileDataResponse.body().asString()).getList("data.cards");
    }

    private int getFileIdFromDatasetEndpointFileListing(Response datasetFiles, String filename) {
        return with(datasetFiles.getBody().asString())
                .param("name", filename)
                .getInt("data.findAll { data -> data.label == name }[0].datafile.id");
    }

    private Set<String> getFileData(Response fileDataResponse) {
        Set<String> filesFound = new HashSet<>();
        List<String> files1 = JsonPath.from(fileDataResponse.body().asString()).getList("data.cards");
        for (String file : files1) {
            filesFound.add(file);
        }
        return filesFound;
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

        public void setId(long id) {
            this.id = id;
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
