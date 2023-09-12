package edu.harvard.iq.dataverse.api.filesystem;

/*
   Copyright (C) 2005-2016, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
*/

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import edu.harvard.iq.dataverse.api.UtilIT;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.batch.entities.JobExecutionEntity;
import edu.harvard.iq.dataverse.batch.entities.StepExecutionEntity;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.batch.runtime.BatchStatus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Batch File System Import Job Integration Tests
 *
 * Gothchas:
 *
 * - if you are testing from a dev server rebuilt from scratch, edit permissions such that:
 *   "Anyone with a Dataverse account can add sub dataverses and datasets"
 *
 */
public class FileRecordJobIT {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static ClassLoader classLoader = FileRecordJobIT.class.getClassLoader();
    private static final String SEP = File.separator;

    // test properties
    private static String testName;
    private static String token;

    // dataset properties
    private static String dsGlobalId;
    private static String dsDoi;
    private static String dsDir;
    private static int dsId;
    private static JsonPath dsPath;
    private static boolean isDraft = true;

    private static Properties props = new Properties();
    private static final String API_TOKEN_HTTP_HEADER = "X-Dataverse-key";
    private static final String BUILTIN_USER_KEY = "burrito";

    @BeforeAll
    public static void setUpClass() throws Exception {

        // this allows for testing on dataverse staging servers via jvm setting
        String restAssuredBaseUri = "http://localhost:8080";
        String specifiedUri = System.getProperty("dataverse.test.baseurl");
        if (specifiedUri != null) {
            restAssuredBaseUri = specifiedUri;
        }
        System.out.println("Base URL for tests: " + restAssuredBaseUri +  "\n");
        RestAssured.baseURI = restAssuredBaseUri;

        // use properties file for everything else
        InputStream input = null;
        try {
            input = classLoader.getResourceAsStream("FileRecordJobIT.properties");
            props.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @BeforeEach
    public void setUpDataverse() {

        try {
            String dsIdentifier;
            // create random test name
            testName = UUID.randomUUID().toString().substring(0, 8);
            // create user and set token
            token = given()
                    .body("{" +
                            "   \"userName\": \"" + testName + "\"," +
                            "   \"firstName\": \"" + testName + "\"," +
                            "   \"lastName\": \"" + testName + "\"," +
                            "   \"email\": \"" + testName + "@mailinator.com\"" +
                            "}")
                    .contentType(ContentType.JSON)
                    .request()
                    .post("/api/builtin-users/secret/" + BUILTIN_USER_KEY)
                    .then().assertThat().statusCode(200)
                    .extract().jsonPath().getString("data.apiToken");
            System.out.println("TOKEN: " + token);
            // create dataverse
            given().body("{" +
                    "    \"name\": \"" + testName + "\"," +
                    "    \"alias\": \"" + testName + "\"," +
                    "    \"affiliation\": \"Test-Driven University\"," +
                    "    \"dataverseContacts\": [" +
                    "        {" +
                    "            \"contactEmail\": \"test@example.com\"" +
                    "        }," +
                    "        {" +
                    "            \"contactEmail\": \"test@example.org\"" +
                    "        }" +
                    "    ]," +
                    "    \"permissionRoot\": true," +
                    "    \"description\": \"test Description.\"" +
                    "}")
                    .contentType(ContentType.JSON).request()
                    .post("/api/dataverses/:root?key=" + token)
                    .then().assertThat().statusCode(201);
            System.out.println("DATAVERSE: http://localhost:8080/dataverse/" + testName);
            // create dataset and set id
            String json = IOUtils.toString(classLoader.getResourceAsStream("json/dataset-finch1.json"));
            dsId = given()
                    .header(API_TOKEN_HTTP_HEADER, token)
                    .body(json)
                    .contentType("application/json")
                    .post("/api/dataverses/" + testName + "/datasets")
                    .then().assertThat().statusCode(201)
                    .extract().jsonPath().getInt("data.id");
            // get dataset identifier
            dsIdentifier = given()
                    .header(API_TOKEN_HTTP_HEADER, token)
                    .get("/api/datasets/" + dsId)
                    .then().assertThat().statusCode(200)
                    .extract().jsonPath().getString("data.identifier");
            dsGlobalId = "doi:" + props.getProperty("authority") + SEP + dsIdentifier;
            System.out.println("IDENTIFIER: " + dsIdentifier);
            dsDoi = props.getProperty("authority") + SEP + dsIdentifier;
            dsDir = props.getProperty("data.dir") + dsIdentifier + SEP;
            System.out.println("DATA DIR: " + dsDir);
        } catch (IOException ioe) {
            System.out.println("Error creating test dataset: " + ioe.getMessage());
            fail();
        }
    }

    @AfterAll
    public static void tearDownClass() {
        RestAssured.reset();
    }

    @AfterEach
    public void tearDownDataverse() {
        try {

            // delete dataset
            if (isDraft) {
                given().header(API_TOKEN_HTTP_HEADER, token)
                        .delete("/api/datasets/" + dsId)
                        .then().assertThat().statusCode(200);
            } else {
                given().post("/api/admin/superuser/" + testName);
                given().header(API_TOKEN_HTTP_HEADER, token)
                        .delete("/api/datasets/" + dsId + "/destroy")
                        .then().assertThat().statusCode(200);
            }
            // delete dataverse
            given().header(API_TOKEN_HTTP_HEADER, token)
                    .delete("/api/dataverses/" + testName)
                    .then().assertThat().statusCode(200);
            // delete user
            given().header(API_TOKEN_HTTP_HEADER, token)
                    .delete("/api/admin/authenticatedUsers/" + testName + "/")
                    .then().assertThat().statusCode(200);
            FileUtils.deleteDirectory(new File(dsDir));
        } catch (IOException ioe) {
            System.out.println("Error creating test dataset: " + ioe.getMessage());
            ioe.printStackTrace();
            fail();
        }
    }

    /**
     * Import the same file in different directories, in the same dataset.
     * This is not permitted via HTTP file upload since identical checksums are not allowed in the same dataset.
     * Ignores failed checksum manifest import.
     */
    @Test
    @Disabled
    public void testSameFileInDifferentDirectories() {

        try {

            // create a single test file and put it in two places
            String file1 =  "testfile.txt";
            String file2 = "subdir/testfile.txt";
            File file = createTestFile(dsDir, file1, 0.25);
            if (file != null) {
                FileUtils.copyFile(file, new File(dsDir + file2));
            } else {
                System.out.println("Unable to copy file: " + dsDir + file2);
                fail();
            }

            // mock the checksum manifest
            String checksum1 = "asfdasdfasdfasdf";
            String checksum2 = "sgsdgdsgfsdgsdgf";
            if (file1 != null && file2 != null) {
                PrintWriter pw = new PrintWriter(new FileWriter(dsDir + "/files.sha"));
                pw.write(checksum1 + " " + file1);
                pw.write("\n");
                pw.write(checksum2 + " " + file2);
                pw.write("\n");
                pw.close();
            } else {
                fail();
            }

            // validate job
            JobExecutionEntity job = getJob();
            assertEquals(job.getSteps().size(), 1);
            StepExecutionEntity step1 = job.getSteps().get(0);
            Map<String, Long> metrics = step1.getMetrics();
            assertEquals(job.getExitStatus(), BatchStatus.COMPLETED.name());
            assertEquals(job.getStatus(), BatchStatus.COMPLETED);
            assertEquals(step1.getExitStatus(), BatchStatus.COMPLETED.name());
            assertEquals(step1.getStatus(), BatchStatus.COMPLETED);
            assertEquals(step1.getName(), "import-files");
            assertEquals((long) metrics.get("write_skip_count"), 0);
            assertEquals((long) metrics.get("commit_count"), 1);
            assertEquals((long) metrics.get("process_skip_count"), 0);
            assertEquals((long) metrics.get("read_skip_count"), 0);
            assertEquals((long) metrics.get("write_count"), 2);
            assertEquals((long) metrics.get("rollback_count"), 0);
            assertEquals((long) metrics.get("filter_count"), 0);
            assertEquals((long) metrics.get("read_count"), 2);
            assertEquals(step1.getPersistentUserData(), null);

            // confirm data files were imported
            updateDatasetJsonPath();
            List<String> storageIds = new ArrayList<>();
            storageIds.add(dsPath.getString("data.latestVersion.files[0].dataFile.storageIdentifier"));
            storageIds.add(dsPath.getString("data.latestVersion.files[1].dataFile.storageIdentifier"));
            assert(storageIds.contains(file1));
            assert(storageIds.contains(file2));

            // test the reporting apis
            given()
                    .header(API_TOKEN_HTTP_HEADER, token)
                    .get(props.getProperty("job.status.api") + job.getId())
                    .then().assertThat()
                    .body("status", equalTo("COMPLETED"));
            List<Integer> ids =  given()
                    .header(API_TOKEN_HTTP_HEADER, token)
                    .get(props.getProperty("job.status.api"))
                    .then().extract().jsonPath()
                    .getList("jobs.id");
            assertTrue(ids.contains((int)job.getId()));

        } catch (Exception e) {
            System.out.println("Error testIdenticalFilesInDifferentDirectories: " + e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

    @Test
    @Disabled
    public void testNewEditor() {

        try {

            // create contributor user
            String contribUser = UUID.randomUUID().toString().substring(0, 8);
            String contribToken = given()
                    .body("{" +
                            "   \"userName\": \"" + contribUser + "\"," +
                            "   \"firstName\": \"" + contribUser + "\"," +
                            "   \"lastName\": \"" + contribUser + "\"," +
                            "   \"email\": \"" + contribUser + "@mailinator.com\"" +
                            "}")
                    .contentType(ContentType.JSON)
                    .request()
                    .post("/api/builtin-users/secret/" + props.getProperty("builtin.user.key"))
                    .then().assertThat().statusCode(200)
                    .extract().jsonPath().getString("data.apiToken");

            Response grantRole = UtilIT.grantRoleOnDataverse(testName, DataverseRole.EDITOR.toString(),
                    "@" + contribUser, token);

            //grantRole.prettyPrint();

            // create a single test file and put it in two places
            String file1 =  "testfile.txt";
            String file2 = "subdir/testfile.txt";
            File file = createTestFile(dsDir, file1, 0.25);
            if (file != null) {
                FileUtils.copyFile(file, new File(dsDir + file2));
            } else {
                System.out.println("Unable to copy file: " + dsDir + file2);
                fail();
            }

            // mock the checksum manifest
            String checksum1 = "asfdasdfasdfasdf";
            String checksum2 = "sgsdgdsgfsdgsdgf";
            if (file1 != null && file2 != null) {
                PrintWriter pw = new PrintWriter(new FileWriter(dsDir + "/files.sha"));
                pw.write(checksum1 + " " + file1);
                pw.write("\n");
                pw.write(checksum2 + " " + file2);
                pw.write("\n");
                pw.close();
            } else {
                fail();
            }

            // validate job
            JobExecutionEntity job = getJobWithToken(contribToken);
            assertEquals(job.getSteps().size(), 1);
            StepExecutionEntity step1 = job.getSteps().get(0);
            Map<String, Long> metrics = step1.getMetrics();
            assertEquals(job.getExitStatus(), BatchStatus.COMPLETED.name());
            assertEquals(job.getStatus(), BatchStatus.COMPLETED);
            assertEquals(step1.getExitStatus(), BatchStatus.COMPLETED.name());
            assertEquals(step1.getStatus(), BatchStatus.COMPLETED);
            assertEquals(step1.getName(), "import-files");
            assertEquals((long) metrics.get("write_skip_count"), 0);
            assertEquals((long) metrics.get("commit_count"), 1);
            assertEquals((long) metrics.get("process_skip_count"), 0);
            assertEquals((long) metrics.get("read_skip_count"), 0);
            assertEquals((long) metrics.get("write_count"), 2);
            assertEquals((long) metrics.get("rollback_count"), 0);
            assertEquals((long) metrics.get("filter_count"), 0);
            assertEquals((long) metrics.get("read_count"), 2);
            assertEquals(step1.getPersistentUserData(), null);

            // confirm data files were imported
            updateDatasetJsonPath();
            List<String> storageIds = new ArrayList<>();
            storageIds.add(dsPath.getString("data.latestVersion.files[0].dataFile.storageIdentifier"));
            storageIds.add(dsPath.getString("data.latestVersion.files[1].dataFile.storageIdentifier"));
            assert(storageIds.contains(file1));
            assert(storageIds.contains(file2));

            // test the reporting apis
            given()
                    .header(API_TOKEN_HTTP_HEADER, contribToken)
                    .get(props.getProperty("job.status.api") + job.getId())
                    .then().assertThat()
                    .body("status", equalTo("COMPLETED"));
            List<Integer> ids =  given()
                    .header(API_TOKEN_HTTP_HEADER, contribToken)
                    .get(props.getProperty("job.status.api"))
                    .then().extract().jsonPath()
                    .getList("jobs.id");
            assertTrue(ids.contains((int)job.getId()));

        } catch (Exception e) {
            System.out.println("Error testNewEditor: " + e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Import the same file in different directories, in the same dataset.
     * This is not permitted via HTTP file upload since identical checksums are not allowed in the same dataset.
     * Ignores failed checksum manifest import.
     */
    @Test
    @Disabled
    public void testSameFileInDifferentDirectoriesUnauthorizedUser() {

        try {

            // create unauthorized user
            String unauthUser = UUID.randomUUID().toString().substring(0, 8);
            String unauthToken = given()
                    .body("{" +
                            "   \"userName\": \"" + unauthUser + "\"," +
                            "   \"firstName\": \"" + unauthUser + "\"," +
                            "   \"lastName\": \"" + unauthUser + "\"," +
                            "   \"email\": \"" + unauthUser + "@mailinator.com\"" +
                            "}")
                    .contentType(ContentType.JSON)
                    .request()
                    .post("/api/builtin-users/secret/" + props.getProperty("builtin.user.key"))
                    .then().assertThat().statusCode(200)
                    .extract().jsonPath().getString("data.apiToken");

            // create a single test file and put it in two places
            String file1 =  "testfile.txt";
            String file2 = "subdir/testfile.txt";
            File file = createTestFile(dsDir, file1, 0.25);
            if (file != null) {
                FileUtils.copyFile(file, new File(dsDir + file2));
            } else {
                System.out.println("Unable to copy file: " + dsDir + file2);
                fail();
            }

            // mock the checksum manifest
            String checksum1 = "asfdasdfasdfasdf";
            String checksum2 = "sgsdgdsgfsdgsdgf";
            if (file1 != null && file2 != null) {
                PrintWriter pw = new PrintWriter(new FileWriter(dsDir + "/files.sha"));
                pw.write(checksum1 + " " + file1);
                pw.write("\n");
                pw.write(checksum2 + " " + file2);
                pw.write("\n");
                pw.close();
            } else {
                fail();
            }

            // should return 403
            given()
                    .header(API_TOKEN_HTTP_HEADER, unauthToken)
                    .post(props.getProperty("filesystem.api") + "/" + dsDoi)
                    .then().assertThat().statusCode(403);

            // delete unauthorized user
            given().header(API_TOKEN_HTTP_HEADER, token)
                    .delete("/api/admin/authenticatedUsers/"+unauthUser+"/")
                    .then().assertThat().statusCode(200);

        } catch (Exception e) {
            System.out.println("Error testSameFileInDifferentDirectoriesUnauthorizedUser: " + e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

//    @Test
//    /**
//     * Delete a file in REPLACE mode
//     */
//    public void testDeleteFileInReplaceMode() {
//
//        try {
//
//            // create a single test file and put it in two places
//            String file1 =  "testfile.txt";
//            String file2 = "subdir/testfile.txt";
//            File file = createTestFile(dsDir, file1, 0.25);
//            if (file != null) {
//                FileUtils.copyFile(file, new File(dsDir + file2));
//            } else {
//                System.out.println("Unable to copy file: " + dsDir + file2);
//                fail();
//            }
//
//            // mock the checksum manifest
//            String checksum1 = "asfdasdfasdfasdf";
//            String checksum2 = "sgsdgdsgfsdgsdgf";
//            if (file1 != null && file2 != null) {
//                PrintWriter pw = new PrintWriter(new FileWriter(dsDir + "/files.sha"));
//                pw.write(checksum1 + " " + file1);
//                pw.write("\n");
//                pw.write(checksum2 + " " + file2);
//                pw.write("\n");
//                pw.close();
//            } else {
//                fail();
//            }
//
//            // validate job
//            JobExecutionEntity job = getJob();
//            assertEquals(job.getSteps().size(), 2);
//            StepExecutionEntity step1 = job.getSteps().get(0);
//            Map<String, Long> metrics = step1.getMetrics();
//            assertEquals(job.getExitStatus(), BatchStatus.COMPLETED.name());
//            assertEquals(job.getStatus(), BatchStatus.COMPLETED);
//            assertEquals(step1.getExitStatus(), BatchStatus.COMPLETED.name());
//            assertEquals(step1.getStatus(), BatchStatus.COMPLETED);
//            assertEquals(step1.getName(), "import-files");
//            assertEquals((long) metrics.get("write_skip_count"), 0);
//            assertEquals((long) metrics.get("commit_count"), 1);
//            assertEquals((long) metrics.get("process_skip_count"), 0);
//            assertEquals((long) metrics.get("read_skip_count"), 0);
//            assertEquals((long) metrics.get("write_count"), 2);
//            assertEquals((long) metrics.get("rollback_count"), 0);
//            assertEquals((long) metrics.get("filter_count"), 0);
//            assertEquals((long) metrics.get("read_count"), 2);
//            assertEquals(step1.getPersistentUserData(), null);
//
//            // confirm data files were imported
//            updateDatasetJsonPath();
//            List<String> storageIds = new ArrayList<>();
//            storageIds.add(dsPath.getString("data.latestVersion.files[0].dataFile.storageIdentifier"));
//            storageIds.add(dsPath.getString("data.latestVersion.files[1].dataFile.storageIdentifier"));
//            assert(storageIds.contains(file1));
//            assert(storageIds.contains(file2));
//
//            // test the reporting apis
//            given()
//                    .header(API_TOKEN_HTTP_HEADER, token)
//                    .get(props.getProperty("job.status.api") + job.getId())
//                    .then().assertThat()
//                    .body("status", equalTo("COMPLETED"));
//            List<Integer> ids =  given()
//                    .header(API_TOKEN_HTTP_HEADER, token)
//                    .get(props.getProperty("job.status.api"))
//                    .then().extract().jsonPath()
//                    .getList("jobs.id");
//            assertTrue(ids.contains((int)job.getId()));
//
//
//            // delete one file and run the job again
//            file.delete();
//
//            // mock the checksum manifest
//            if (file1 != null) {
//                PrintWriter pw = new PrintWriter(new FileWriter(dsDir + "/files.sha"));
//                pw.write(checksum1 + " " + file1);
//                pw.write("\n");
//                pw.close();
//            } else {
//                fail();
//            }
//
//            // validate job again
//            JobExecutionEntity newJob = getJobWithMode("REPLACE");
//            assertEquals(newJob.getSteps().size(), 2);
//            StepExecutionEntity newSteps = newJob.getSteps().get(0);
//            Map<String, Long> newMetrics = newSteps.getMetrics();
//            assertEquals(newJob.getExitStatus(), BatchStatus.COMPLETED.name());
//            assertEquals(newJob.getStatus(), BatchStatus.COMPLETED);
//            assertEquals(newSteps.getExitStatus(), BatchStatus.COMPLETED.name());
//            assertEquals(newSteps.getStatus(), BatchStatus.COMPLETED);
//            assertEquals(newSteps.getName(), "import-files");
//            assertEquals(0, (long) newMetrics.get("write_skip_count"));
//            assertEquals(1, (long) newMetrics.get("commit_count"));
//            assertEquals(0, (long) newMetrics.get("process_skip_count"));
//            assertEquals(0, (long) newMetrics.get("read_skip_count"));
//            assertEquals(1, (long) newMetrics.get("write_count"));
//            assertEquals(0, (long) newMetrics.get("rollback_count"));
//            assertEquals(0, (long) newMetrics.get("filter_count"));
//            assertEquals(1, (long) newMetrics.get("read_count"));
//            assertEquals(newSteps.getPersistentUserData(), null);
//
//            // confirm data files were imported
//            updateDatasetJsonPath();
//            //System.out.println("DATASET JSON: " + dsPath.prettyPrint());
//            List<String> newStorageIds = new ArrayList<>();
//            newStorageIds.add(dsPath.getString("data.latestVersion.files[0].dataFile.storageIdentifier"));
//            assert(newStorageIds.contains(file2)); // should contain subdir/testfile.txt still
//
//            // test the reporting apis
//            given()
//                    .header(API_TOKEN_HTTP_HEADER, token)
//                    .get(props.getProperty("job.status.api") + newJob.getId())
//                    .then().assertThat()
//                    .body("status", equalTo("COMPLETED"));
//            List<Integer> newIds =  given()
//                    .header(API_TOKEN_HTTP_HEADER, token)
//                    .get(props.getProperty("job.status.api"))
//                    .then().extract().jsonPath()
//                    .getList("jobs.id");
//            assertTrue(newIds.contains((int)job.getId()));
//
//        } catch (Exception e) {
//            System.out.println("Error testIdenticalFilesInDifferentDirectories: " + e.getMessage());
//            e.printStackTrace();
//            fail();
//        }
//    }

    @Test
    @Disabled
    /**
     * Add a file in MERGE mode (default), should only need to commit the new file
     */
    public void testAddingFilesInMergeMode() {

        try {

            // create a single test file and put it in two places
            String file1 =  "testfile.txt";
            String file2 = "subdir/testfile.txt";
            File file = createTestFile(dsDir, file1, 0.25);
            if (file != null) {
                FileUtils.copyFile(file, new File(dsDir + file2));
            } else {
                System.out.println("Unable to copy file: " + dsDir + file2);
                fail();
            }

            // mock the checksum manifest
            String checksum1 = "asfdasdfasdfasdf";
            String checksum2 = "sgsdgdsgfsdgsdgf";
            if (file1 != null && file2 != null) {
                PrintWriter pw = new PrintWriter(new FileWriter(dsDir + "/files.sha"));
                pw.write(checksum1 + " " + file1);
                pw.write("\n");
                pw.write(checksum2 + " " + file2);
                pw.write("\n");
                pw.close();
            } else {
                fail();
            }

            // validate job
            JobExecutionEntity job = getJob();
            assertEquals(job.getSteps().size(), 1);
            StepExecutionEntity step1 = job.getSteps().get(0);
            Map<String, Long> metrics = step1.getMetrics();
            assertEquals(job.getExitStatus(), BatchStatus.COMPLETED.name());
            assertEquals(job.getStatus(), BatchStatus.COMPLETED);
            assertEquals(step1.getExitStatus(), BatchStatus.COMPLETED.name());
            assertEquals(step1.getStatus(), BatchStatus.COMPLETED);
            assertEquals(step1.getName(), "import-files");
            assertEquals((long) metrics.get("write_skip_count"), 0);
            assertEquals((long) metrics.get("commit_count"), 1);
            assertEquals((long) metrics.get("process_skip_count"), 0);
            assertEquals((long) metrics.get("read_skip_count"), 0);
            assertEquals((long) metrics.get("write_count"), 2);
            assertEquals((long) metrics.get("rollback_count"), 0);
            assertEquals((long) metrics.get("filter_count"), 0);
            assertEquals((long) metrics.get("read_count"), 2);
            assertEquals(step1.getPersistentUserData(), null);

            // confirm data files were imported
            updateDatasetJsonPath();
            List<String> storageIds = new ArrayList<>();
            storageIds.add(dsPath.getString("data.latestVersion.files[0].dataFile.storageIdentifier"));
            storageIds.add(dsPath.getString("data.latestVersion.files[1].dataFile.storageIdentifier"));
            assert(storageIds.contains(file1));
            assert(storageIds.contains(file2));

            // test the reporting apis
            given()
                    .header(API_TOKEN_HTTP_HEADER, token)
                    .get(props.getProperty("job.status.api") + job.getId())
                    .then().assertThat()
                    .body("status", equalTo("COMPLETED"));
            List<Integer> ids =  given()
                    .header(API_TOKEN_HTTP_HEADER, token)
                    .get(props.getProperty("job.status.api"))
                    .then().extract().jsonPath()
                    .getList("jobs.id");
            assertTrue(ids.contains((int)job.getId()));


            // add a new file and run the job again
            String file3 =  "addedfile.txt";
            File addedFile = createTestFile(dsDir, file3, 0.25);

            // mock the checksum manifest
            String checksum3 = "asfdasdfasdfasdf";
            if (file1 != null && file2 != null && file3 != null) {
                PrintWriter pw = new PrintWriter(new FileWriter(dsDir + "/files.sha"));
                pw.write(checksum1 + " " + file1);
                pw.write("\n");
                pw.write(checksum2 + " " + file2);
                pw.write("\n");
                pw.write(checksum3 + " " + file3);
                pw.write("\n");
                pw.close();
            } else {
                fail();
            }

            // validate job again
            JobExecutionEntity newJob = getJobWithMode("MERGE");
            assertEquals(newJob.getSteps().size(), 1);
            StepExecutionEntity newSteps = newJob.getSteps().get(0);
            Map<String, Long> newMetrics = newSteps.getMetrics();
            assertEquals(newJob.getExitStatus(), BatchStatus.COMPLETED.name());
            assertEquals(newJob.getStatus(), BatchStatus.COMPLETED);
            assertEquals(newSteps.getExitStatus(), BatchStatus.COMPLETED.name());
            assertEquals(newSteps.getStatus(), BatchStatus.COMPLETED);
            assertEquals(newSteps.getName(), "import-files");
            assertEquals(0, (long) newMetrics.get("write_skip_count"));
            assertEquals(1, (long) newMetrics.get("commit_count"));
            assertEquals(0, (long) newMetrics.get("process_skip_count"));
            assertEquals(0, (long) newMetrics.get("read_skip_count"));
            assertEquals(1, (long) newMetrics.get("write_count"));
            assertEquals(0, (long) newMetrics.get("rollback_count"));
            assertEquals(2, (long) newMetrics.get("filter_count"));
            assertEquals(3, (long) newMetrics.get("read_count"));
            assertEquals(newSteps.getPersistentUserData(), null);

            // confirm data files were imported
            updateDatasetJsonPath();
            List<String> newStorageIds = new ArrayList<>();
            newStorageIds.add(dsPath.getString("data.latestVersion.files[0].dataFile.storageIdentifier"));
            newStorageIds.add(dsPath.getString("data.latestVersion.files[1].dataFile.storageIdentifier"));
            newStorageIds.add(dsPath.getString("data.latestVersion.files[2].dataFile.storageIdentifier"));
            assert(newStorageIds.contains(file1));
            assert(newStorageIds.contains(file2));
            assert(newStorageIds.contains(file3));

            // test the reporting apis
            given()
                    .header(API_TOKEN_HTTP_HEADER, token)
                    .get(props.getProperty("job.status.api") + newJob.getId())
                    .then().assertThat()
                    .body("status", equalTo("COMPLETED"));
            List<Integer> newIds =  given()
                    .header(API_TOKEN_HTTP_HEADER, token)
                    .get(props.getProperty("job.status.api"))
                    .then().extract().jsonPath()
                    .getList("jobs.id");
            assertTrue(newIds.contains((int)job.getId()));

        } catch (Exception e) {
            System.out.println("Error testIdenticalFilesInDifferentDirectories: " + e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

    @Test
    @Disabled
    /**
     * The success case: all files uploaded and present in checksum manifest
     */
    public void testFilesWithChecksumManifest() {

        try {
            // create test files and checksum manifest
            File file1 = createTestFile(dsDir, "testfile1.txt", 0.25);
            File file2 = createTestFile(dsDir, "testfile2.txt", 0.25);
            String checksum1 = "asfdasdfasdfasdf";
            String checksum2 = "sgsdgdsgfsdgsdgf";
            if (file1 != null && file2 != null) {
                PrintWriter pw = new PrintWriter(new FileWriter(dsDir + "/files.sha"));
                pw.write(checksum1 + " " + file1.getName());
                pw.write("\n");
                pw.write(checksum2 + " " + file2.getName());
                pw.write("\n");
                pw.close();
            } else {
                fail();
            }

            JobExecutionEntity job = getJob();
            assertEquals(job.getSteps().size(), 1);
            StepExecutionEntity step1 = job.getSteps().get(0);
            Map<String, Long> metrics1 = step1.getMetrics();
            // check job status
            assertEquals(BatchStatus.COMPLETED.name(), job.getExitStatus());
            assertEquals(BatchStatus.COMPLETED, job.getStatus());
            // check step 1 status and name
            assertEquals(step1.getExitStatus(), BatchStatus.COMPLETED.name());
            assertEquals(step1.getStatus(), BatchStatus.COMPLETED);
            assertEquals(step1.getName(), "import-files");
            // verify step 1 metrics
            assertEquals((long) metrics1.get("write_skip_count"), 0);
            assertEquals((long) metrics1.get("commit_count"), 1);
            assertEquals((long) metrics1.get("process_skip_count"), 0);
            assertEquals((long) metrics1.get("read_skip_count"), 0);
            assertEquals((long) metrics1.get("write_count"), 2);
            assertEquals((long) metrics1.get("rollback_count"), 0);
            assertEquals((long) metrics1.get("filter_count"), 0);
            assertEquals((long) metrics1.get("read_count"), 2);
            // should be no user data (error messages)
            assertEquals(step1.getPersistentUserData(), null);

            // confirm files were imported
            updateDatasetJsonPath();
            List<String> filenames = new ArrayList<>();
            filenames.add(dsPath.getString("data.latestVersion.files[0].dataFile.filename"));
            filenames.add(dsPath.getString("data.latestVersion.files[1].dataFile.filename"));
            assert(filenames.contains("testfile1.txt"));
            assert(filenames.contains("testfile2.txt"));

            // confirm checksums were imported
            List<String> checksums = new ArrayList<>();
            checksums.add(dsPath.getString("data.latestVersion.files[0].dataFile.checksum.value"));
            checksums.add(dsPath.getString("data.latestVersion.files[1].dataFile.checksum.value"));
            assert(checksums.contains(checksum1));
            assert(checksums.contains(checksum2));

        } catch (Exception e) {
            System.out.println("Error testChecksumImport: " + e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

    @Test
    @Disabled
    /**
     * No checksum manifest found
     */
    public void testFilesWithoutChecksumManifest() {

        try {

            // create test files and NO checksum manifest
            createTestFile(dsDir, "testfile1.txt", 0.25);
            createTestFile(dsDir, "testfile2.txt", 0.25);

            JobExecutionEntity job = getJob();
            assertEquals(job.getSteps().size(), 1);
            StepExecutionEntity step1 = job.getSteps().get(0);
            Map<String, Long> metrics1 = step1.getMetrics();
            // check job status
            assertEquals(job.getExitStatus(), BatchStatus.FAILED.name());
            assertEquals(job.getStatus(), BatchStatus.FAILED);
            // check step 1 status and name
            assertEquals(step1.getExitStatus(), BatchStatus.FAILED.name());
            assertEquals(step1.getStatus(), BatchStatus.FAILED);
            assertEquals(step1.getName(), "import-files");
            // verify step 1 metrics
            assertEquals((long) metrics1.get("write_skip_count"), 0);
            assertEquals((long) metrics1.get("commit_count"), 0);
            assertEquals((long) metrics1.get("process_skip_count"), 0);
            assertEquals((long) metrics1.get("read_skip_count"), 0);
            assertEquals((long) metrics1.get("write_count"), 0);
            assertEquals((long) metrics1.get("rollback_count"), 0);
            assertEquals((long) metrics1.get("filter_count"), 0);
            assertEquals((long) metrics1.get("read_count"), 0);
            // should be no user data (error messages)
            assertEquals(step1.getPersistentUserData(), null);

            // confirm files were imported and checksums unknown
            updateDatasetJsonPath();
            List<String> filenames = new ArrayList<>();
            filenames.add(dsPath.getString("data.latestVersion.files[0].dataFile.filename"));
            filenames.add(dsPath.getString("data.latestVersion.files[1].dataFile.filename"));
            assert(filenames.contains("testfile1.txt"));
            assert(filenames.contains("testfile2.txt"));
            assert(dsPath.getString("data.latestVersion.files[0].dataFile.checksum.value").equalsIgnoreCase("unknown"));
            assert(dsPath.getString("data.latestVersion.files[1].dataFile.checksum.value").equalsIgnoreCase("unknown"));

        } catch (Exception e) {
            System.out.println("Error testChecksumImportMissingManifest: " + e.getMessage());
            e.printStackTrace();
            fail();
        }

    }

    @Test
    @Disabled
    /**
     * Checksum manifest is missing an uploaded file
     */
    public void testFileMissingInChecksumManifest() {

        try {

            // create test files and checksum manifest with just one of the files
            File file1 = createTestFile(dsDir, "testfile1.txt", 0.25);
            File file2 = createTestFile(dsDir, "testfile2.txt", 0.25);
            String checksum1 = "";
            if (file1 != null && file2 != null) {
                PrintWriter pw = new PrintWriter(new FileWriter(dsDir + "/files.sha"));
                checksum1 = "asasdlfkj880asfdasflj";
                pw.write(checksum1 + " " + file1.getName());
                pw.write("\n");
                pw.close();
            } else {
                fail();
            }

            JobExecutionEntity job = getJob();
            assertEquals(job.getSteps().size(), 1);
            StepExecutionEntity step1 = job.getSteps().get(0);
            Map<String, Long> metrics1 = step1.getMetrics();
            // check job status
            assertEquals(job.getExitStatus(), BatchStatus.COMPLETED.name());
            assertEquals(job.getStatus(), BatchStatus.COMPLETED);
            // check step 1 status and name
            assertEquals(step1.getExitStatus(), BatchStatus.COMPLETED.name());
            assertEquals(step1.getStatus(), BatchStatus.COMPLETED);
            assertEquals(step1.getName(), "import-files");
            // verify step 1 metrics
            assertEquals((long) metrics1.get("write_skip_count"), 0);
            assertEquals((long) metrics1.get("commit_count"), 1);
            assertEquals((long) metrics1.get("process_skip_count"), 0);
            assertEquals((long) metrics1.get("read_skip_count"), 0);
            assertEquals((long) metrics1.get("write_count"), 2);
            assertEquals((long) metrics1.get("rollback_count"), 0);
            assertEquals((long) metrics1.get("filter_count"), 0);
            assertEquals((long) metrics1.get("read_count"), 2);
            // should be no user data (error messages)
            assertEquals(step1.getPersistentUserData(), null);

            // confirm files were imported
            updateDatasetJsonPath();
            List<String> filenames = new ArrayList<>();
            filenames.add(dsPath.getString("data.latestVersion.files[0].dataFile.filename"));
            filenames.add(dsPath.getString("data.latestVersion.files[1].dataFile.filename"));
            assert(filenames.contains("testfile1.txt"));
            assert(filenames.contains("testfile2.txt"));
            // confirm one checksums was imported, one not
            List<String> checksums = new ArrayList<>();
            checksums.add(dsPath.getString("data.latestVersion.files[0].dataFile.checksum.value"));
            checksums.add(dsPath.getString("data.latestVersion.files[1].dataFile.checksum.value"));
            assert(checksums.contains(checksum1));
            assert(checksums.contains("Unknown"));

        } catch (Exception e) {
            System.out.println("Error testChecksumImport: " + e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

    @Test
    @Disabled
    /**
     * Checksum manifest references a file that isn't present, it should return failed status and detailed 
     * message in persistentUserData
     */
    public void testFileInChecksumManifestDoesntExist() {

        try {

            // create test files and checksum manifest with record that doesn't exist
            File file1 = createTestFile(dsDir, "testfile1.txt", 0.25);
            File file2 = createTestFile(dsDir, "testfile2.txt", 0.25);
            String checksum1 = "aorjsonaortargj848";
            String checksum2 = "ldgklrrshfdsnosri4948";
            if (file1 != null && file2 != null) {
                PrintWriter pw = new PrintWriter(new FileWriter(dsDir + "/files.sha"));
                pw.write(checksum1 + " " + file1.getName());
                pw.write("\n");
                pw.write(checksum2 + " " + file2.getName());
                pw.write("\n");
                pw.write("asdfae34034asfaf9r3  fileThatDoesntExist.txt");
                pw.write("\n");
                pw.close();
            } else {
                fail();
            }

            JobExecutionEntity job = getJob();
            assertEquals(job.getSteps().size(), 1);
            StepExecutionEntity step1 = job.getSteps().get(0);
            Map<String, Long> metrics1 = step1.getMetrics();
            // check job status
            assertEquals(job.getExitStatus(), BatchStatus.COMPLETED.name());
            assertEquals(job.getStatus(), BatchStatus.COMPLETED);
            // check step 1 status and name
            assertEquals(step1.getExitStatus(), BatchStatus.COMPLETED.name());
            assertEquals(step1.getStatus(), BatchStatus.COMPLETED);
            assertEquals(step1.getName(), "import-files");
            // verify step 1 metrics
            assertEquals((long) metrics1.get("write_skip_count"), 0);
            assertEquals((long) metrics1.get("commit_count"), 1);
            assertEquals((long) metrics1.get("process_skip_count"), 0);
            assertEquals((long) metrics1.get("read_skip_count"), 0);
            assertEquals((long) metrics1.get("write_count"), 2);
            assertEquals((long) metrics1.get("rollback_count"), 0);
            assertEquals((long) metrics1.get("filter_count"), 0);
            assertEquals((long) metrics1.get("read_count"), 2);
            // should be no user data (error messages)
            assertEquals(step1.getPersistentUserData(), null);

            // confirm files were imported
            updateDatasetJsonPath();
            List<String> filenames = new ArrayList<>();
            filenames.add(dsPath.getString("data.latestVersion.files[0].dataFile.filename"));
            filenames.add(dsPath.getString("data.latestVersion.files[1].dataFile.filename"));
            assert(filenames.contains("testfile1.txt"));
            assert(filenames.contains("testfile2.txt"));
            // confirm checksums were imported
            List<String> checksums = new ArrayList<>();
            checksums.add(dsPath.getString("data.latestVersion.files[0].dataFile.checksum.value"));
            checksums.add(dsPath.getString("data.latestVersion.files[1].dataFile.checksum.value"));
            assert(checksums.contains(checksum1));
            assert(checksums.contains(checksum2));

        } catch (Exception e) {
            System.out.println("Error testChecksumImport: " + e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

    @Test
    @Disabled
    /**
     * Published datasets should not allow import jobs for now since it isn't in DRAFT mode
     */
    public void testPublishedDataset() {

        try {

            RestAssured.urlEncodingEnabled = false;

            // publish the dataverse
            System.out.println("DATAVERSE: http://localhost:8080/api/dataverses/"+testName+"/actions/:publish?key="+token);
            given().body("{}").contentType("application/json")
                    .post("/api/dataverses/" + testName + "/actions/:publish?key="+token)
                    .then().assertThat().statusCode(200);

            // publish the dataset
            System.out.println("DATASET: http://localhost:8080/api/datasets/"+dsId+"/actions/:publish?type=major&key="+token);
            given()
                    .get("/api/datasets/" + dsId + "/actions/:publish?type=major&key="+token)
                    .then().assertThat().statusCode(200);

            isDraft = false;

            JsonPath jsonPath = getFaileJobJson();
            assertTrue(jsonPath.getString("status").equalsIgnoreCase("ERROR"));
            assertTrue(jsonPath.getString("message").contains("Dataset isn't in DRAFT mode."));

        } catch (Exception e) {
            System.out.println("Error testChecksumImport: " + e.getMessage());
            e.printStackTrace();
            fail();
        }
    }


//  todo: figure out how to create a new version using the native api - sorry, i can't get this to work...
//    @Test
//    /**
//     * Datasets with more than one version not allowed
//     */
//    public void testMoreThanOneVersion() {
//
//        try {
//
//            RestAssured.urlEncodingEnabled = false;
//            
//            // publish the dataverse
//            System.out.println("DATAVERSE: http://localhost:8080/api/dataverses/"+testName+"/actions/:publish?key="+token);
//            given().body("{}").contentType("application/json")
//                    .post("/api/dataverses/" + testName + "/actions/:publish?key="+token)
//                    .then().assertThat().statusCode(200);
//
//            // publish the dataset
//            System.out.println("DATASET: http://localhost:8080/api/datasets/"+dsId+"/actions/:publish?type=major&key="+token);
//            given()
//                    .get("/api/datasets/" + dsId + "/actions/:publish?type=major&key="+token)
//                    .then().assertThat().statusCode(200);
//
//            // create a new draft version - can't get this to work, responds with 500 and stacktrace
//            System.out.println("NEW DRAFT: http://localhost:8080/api/datasets/"+dsId+"/versions/:draft?key="+token);
//            given()
//                    .header(API_TOKEN_HTTP_HEADER, token)
//                    .body(IOUtils.toString(classLoader.getResourceAsStream("json/dataset-finch1.json")))
//                    .contentType("application/json")
//                    .put("/api/datasets/" + dsId + "/versions/:draft?key="+token)
//                    .then().assertThat().statusCode(201);
//
//
//            JsonPath jsonPath = getFaileJobJson();
//            jsonPath.prettyPrint();
//            assertTrue(jsonPath.getString("status").equalsIgnoreCase("ERROR"));
//            assertTrue(jsonPath.getString("message").contains("Dataset has more than one version."));
//
//        } catch (Exception e) {
//            System.out.println("Error testMoreThanOneVersion: " + e.getMessage());
//            e.printStackTrace();
//            fail();
//        }
//    }

    @Test
    @Disabled
    /**
     * No dataset found responses (bad dataset id, etc.)
     */
    public void testNoDatasetFound() {
        try {
            String fakeDoi = "10.0001/FK2/FAKE";
            // run batch job
            String dsNotFound  = given()
                    .header(API_TOKEN_HTTP_HEADER, token)
                    .post(props.getProperty("filesystem.api") + "/" + fakeDoi)
                    .then().assertThat().statusCode(400)
                    .extract().jsonPath().getString("message");
            assertEquals("Can't find dataset with ID: doi:" + fakeDoi, dsNotFound);

        } catch (Exception e) {
            System.out.println("Error testNoDatasetFound: " + e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

    // UTILS

    /***
     * Create a test file with a size in GB
     *
     * @param dir where to save the file (directory will be created if it doesn't exist)
     * @param name the test file name
     * @param size the desired size in GB
     * @return the file
     */
    static File createTestFile(String dir, String name, double size) {

        try {
            File myDir = new File(dir);
            Random random = new Random();
            boolean isDirCreated = myDir.exists();
            if (!isDirCreated) {
                isDirCreated = myDir.mkdirs();
            }
            if (isDirCreated) {
                File file = new File(dir + SEP + name);
                long start = System.currentTimeMillis();
                PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(file), "UTF-8")), false);
                int counter = 0;
                while (true) {
                    String sep = "";
                    for (int i = 0; i < 100; i++) {
                        int number = random.nextInt(1000) + 1;
                        writer.print(sep);
                        writer.print(number / 1e3);
                        sep = " ";
                    }
                    writer.println();
                    if (++counter == 20000) {
                        //System.out.printf("Size: %.3f GB%n", file.length() / 1e9);
                        if (file.length() >= size * 1e9) {
                            writer.close();
                            break;
                        } else {
                            counter = 0;
                        }
                    }
                }
                long time = System.currentTimeMillis() - start;
                //System.out.printf("Took %.1f seconds to create a file of %.3f GB", time / 1e3, file.length() / 1e9);
                return file;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /**
     * Get the SHA1 checksum for a file.
     *
     * @param file absolute path to file
     * @param format the checksum format (e.g., SHA1, MD5)
     * @return the checksum for the file as a hex string
     */
    static String getFileChecksum(String file, String format) {
        try {
            MessageDigest md = MessageDigest.getInstance(format);
            FileInputStream fis = new FileInputStream(file);
            byte[] dataBytes = new byte[1024];

            int nread;
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }

            byte[] mdbytes = md.digest();

            //convert the byte to hex format
            StringBuilder sb = new StringBuilder("");
            for (byte b : mdbytes) {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();

        } catch (Exception e) {
            System.out.println("Error getting " + format + " checksum for " + file + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Poll the import job, pending completion
     * @param jobId job execution id
     * @param apiToken user token
     * @param retry max number of retry attempts
     * @param sleep milliseconds to wait between attempts
     * @return json job status
     */
    public static String pollJobStatus(String jobId, String apiToken, int retry, long sleep) {
        int maxTries = 0;
        String json = "";
        String status = BatchStatus.STARTED.name();
        try {
            while (!status.equalsIgnoreCase(BatchStatus.COMPLETED.name())) {
                if (maxTries < retry) {
                    maxTries++;
                    Thread.sleep(sleep);
                    Response jobResponse = given()
                            .header(API_TOKEN_HTTP_HEADER, apiToken)
                            .get(props.getProperty("job.status.api") + jobId);
                    json = jobResponse.body().asString();
                    status = JsonPath.from(json).getString("status");
                    System.out.println("JOB STATUS RETRY ATTEMPT: " + maxTries);
                } else {
                    System.out.println("JOB STATUS ERROR: Failed to get job status after " + maxTries
                            + " attempts.");
                    break;
                }
            }
        }catch (InterruptedException ie) {
            System.out.println(ie.getMessage());
            ie.printStackTrace();
        }
        return json;
    }

    /**
     * A failed job is expected, get the json failure response
     * @return
     */
    private JsonPath getFaileJobJson() {
        System.out.println("JOB API: " + props.getProperty("filesystem.api") + "/" + dsDoi);
        JsonPath jsonPath = given()
                .header(API_TOKEN_HTTP_HEADER, token)
                .post(props.getProperty("filesystem.api") + "/" + dsDoi)
                .then().assertThat().statusCode(400)
                .extract().jsonPath();
        return jsonPath;
    }

    /**
     * Kick off a job with default mode (MERGE)
     * @return a job execution entity
     */
    private JobExecutionEntity getJob() {
        System.out.println("JOB API: " + props.getProperty("filesystem.api") + "/" + dsDoi);
        try {
            // run batch job and wait for result
            String jobId = given()
                    .header(API_TOKEN_HTTP_HEADER, token)
                    .post(props.getProperty("filesystem.api") + "/" + dsDoi)
                    .then().assertThat().statusCode(200)
                    .extract().jsonPath().getString("data.executionId");
            String jobResult = pollJobStatus(jobId, token, Integer.valueOf(props.getProperty("polling.retries")),
                    Integer.valueOf(props.getProperty("polling.wait")));
            System.out.println("JOB JSON: " + jobResult);
            return mapper.readValue(jobResult, JobExecutionEntity.class);
        } catch (IOException ioe) {
            System.out.println("Error getting job execution entity: " + ioe.getMessage());
            return null;
        }
    }

    /**
     * Kick off a job with default mode (MERGE)
     * @return a job execution entity
     */
    private JobExecutionEntity getJobWithToken(String userToken) {
        System.out.println("JOB API: " + props.getProperty("filesystem.api") + "/" + dsDoi);
        System.out.println("TOKEN USED: " + userToken);
        try {
            // run batch job and wait for result
            String jobId = given()
                    .header(API_TOKEN_HTTP_HEADER, userToken)
                    .post(props.getProperty("filesystem.api") + "/" + dsDoi)
                    .then().assertThat().statusCode(200)
                    .extract().jsonPath().getString("data.executionId");
            String jobResult = pollJobStatus(jobId, token, Integer.valueOf(props.getProperty("polling.retries")),
                    Integer.valueOf(props.getProperty("polling.wait")));
            System.out.println("JOB JSON: " + jobResult);
            return mapper.readValue(jobResult, JobExecutionEntity.class);
        } catch (IOException ioe) {
            System.out.println("Error getting job execution entity: " + ioe.getMessage());
            return null;
        }
    }
    /**
     * Kick off a job with a specified mode: MERGE, UPDATE, REPLACE
     * @param mode
     * @return a job entity
     */
    private JobExecutionEntity getJobWithMode(String mode) {
        System.out.println("JOB API: " + props.getProperty("filesystem.api") + "/" + dsDoi + "?mode=" + mode.toUpperCase());
        try {
            // run batch job and wait for result
            String jobId = given()
                    .header(API_TOKEN_HTTP_HEADER, token)
                    .post(props.getProperty("filesystem.api") + "/" + dsDoi + "?mode=" + mode.toUpperCase())
                    .then().assertThat().statusCode(200)
                    .extract().jsonPath().getString("data.executionId");
            String jobResult = pollJobStatus(jobId, token, Integer.valueOf(props.getProperty("polling.retries")),
                    Integer.valueOf(props.getProperty("polling.wait")));
            System.out.println("JOB JSON: " + jobResult);
            return mapper.readValue(jobResult, JobExecutionEntity.class);
        } catch (IOException ioe) {
            System.out.println("Error getting job execution entity: " + ioe.getMessage());
            return null;
        }
    }

    /**
     * Update the dataset json
     */
    private void updateDatasetJsonPath() {
        System.out.println("API: " + props.getProperty("dataset.api") + dsGlobalId);
        dsPath = given()
                .header(API_TOKEN_HTTP_HEADER, token)
                .contentType(ContentType.JSON)
                .get(props.getProperty("dataset.api") + dsGlobalId)
                .then().assertThat().statusCode(200)
                .extract().jsonPath();
    }

}
