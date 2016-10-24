package edu.harvard.iq.dataverse.api;

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

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * FileMetadata Integration Tests
 * 
 * @author bmckinney 
 */
public class FileMetadataIT {

    private static ClassLoader classLoader = FileMetadataIT.class.getClassLoader();

    // test properties
    private static String testName;
    private static String token;
    private static final String builtinUserKey = "burrito";
    private static final String keyString = "X-Dataverse-key";

    // dataset properties
    private static int dsId;
    
    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Before
    public void setUpDataverse() {
        try {
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
                    .post("/api/builtin-users/secret/" + builtinUserKey)
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
            System.out.println("DATAVERSE: " + RestAssured.baseURI + "/dataverse/" + testName);
        } catch (Exception e) {
            System.out.println("Error setting up test dataverse: " + e.getMessage());
            fail();
        }
    }

    @AfterClass
    public static void tearDownClass() {
        RestAssured.reset();
    }

    @After
    public void tearDownDataverse() {
        try {
            // delete dataset
            given().header(keyString, token)
                    .delete("/api/datasets/" + dsId)
                    .then().assertThat().statusCode(200);
            // delete dataverse
            given().header(keyString, token)
                    .delete("/api/dataverses/" + testName)
                    .then().assertThat().statusCode(200);
            // delete user
            given().header(keyString, token)
                    .delete("/api/admin/authenticatedUsers/"+testName+"/")
                    .then().assertThat().statusCode(200);
        } catch (Exception e) {
            System.out.println("Error tearing down test Dataverse: " + e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Create a dataset with files and directoryLabels using json.
     */
    @Test
    public void testJsonParserWithDirectoryLabels() {
        try {

            // try to create a dataset with directory labels that contain both leading and trailing file separators
            Response shouldFailDueToLeadingAndTrailingSeparators = given()
                    .header(keyString, token)
                    .body(IOUtils.toString(classLoader.getResourceAsStream(
                            "json/complete-dataset-with-files-invalid-directory-labels.json")))
                    .contentType("application/json")
                    .post("/api/dataverses/" + testName + "/datasets");
            shouldFailDueToLeadingAndTrailingSeparators.prettyPrint();
            shouldFailDueToLeadingAndTrailingSeparators.then().assertThat()
                    // Note that the JSON under test actually exercises leading too but only the first (trailing) is exercised here.
                    .body("message", equalTo("Validation failed: Directory Name cannot contain leading or trailing file separators. Invalid value: 'data/subdir1/'."))
                    .statusCode(BAD_REQUEST.getStatusCode());

            // create dataset and set id
            dsId = given()
                    .header(keyString, token)
                    .body(IOUtils.toString(classLoader.getResourceAsStream("json/complete-dataset-with-files.json")))
                    .contentType("application/json")
                    .post("/api/dataverses/" + testName + "/datasets")
                    .then().assertThat().statusCode(201)
                    .extract().jsonPath().getInt("data.id");
            // check that both directory labels were persisted
            String dirLabel1 = given()
                    .header(keyString, token)
                    .get("/api/datasets/" + dsId)
                    .then().assertThat().statusCode(200)
                    .extract().jsonPath().getString("data.latestVersion.files[0].directoryLabel");
            System.out.println("directoryLabel 1: " + dirLabel1);
            assertEquals("data/subdir1", dirLabel1);
            String dirLabel2 = given()
                    .header(keyString, token)
                    .get("/api/datasets/" + dsId)
                    .then().assertThat().statusCode(200)
                    .extract().jsonPath().getString("data.latestVersion.files[1].directoryLabel");
            System.out.println("directoryLabel 2: " + dirLabel2);
            assertEquals("data/subdir2", dirLabel2);

        } catch (Exception e) {
            System.out.println("Error testJsonParserWithDirectoryLabels: " + e.getMessage());
            e.printStackTrace();
            fail();
        }
    }
    
}