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
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * FileMetadata Integration Tests
 */
public class FileMetadataIT {

    private static ClassLoader classLoader = FileMetadataIT.class.getClassLoader();

    // test properties
    private static String testName;
    private static String token;

    // dataset properties
    private static int dsId;

    private static Properties props = new Properties();

    @BeforeClass
    public static void setUpClass() throws Exception {

        InputStream input = null;

        try {
            input = classLoader.getResourceAsStream("FileMetadataIT.properties");
            props.load(input);
            RestAssured.baseURI = props.getProperty("baseuri");
            String port = props.getProperty("port");
            RestAssured.port = Integer.valueOf(port);
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
                    .post("/api/builtin-users/secret/" + props.getProperty("builtin.user.key"))
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
            given().header(props.getProperty("api.token.http.header"), token)
                    .delete("/api/datasets/" + dsId)
                    .then().assertThat().statusCode(200);
            // delete dataverse
            given().header(props.getProperty("api.token.http.header"), token)
                    .delete("/api/dataverses/" + testName)
                    .then().assertThat().statusCode(200);
            // delete user
            given().header(props.getProperty("api.token.http.header"), token)
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
            // create dataset and set id
            dsId = given()
                    .header(props.getProperty("api.token.http.header"), token)
                    .body(IOUtils.toString(classLoader.getResourceAsStream("json/complete-dataset-with-files.json")))
                    .contentType("application/json")
                    .post("/api/dataverses/" + testName + "/datasets")
                    .then().assertThat().statusCode(201)
                    .extract().jsonPath().getInt("data.id");
            // check that both directory labels were persisted
            String dirLabel1 = given()
                    .header(props.getProperty("api.token.http.header"), token)
                    .get("/api/datasets/" + dsId)
                    .then().assertThat().statusCode(200)
                    .extract().jsonPath().getString("data.latestVersion.files[0].directoryLabel");
            System.out.println("directoryLabel 1: " + dirLabel1);
            assertEquals("data/subdir1/", dirLabel1);
            String dirLabel2 = given()
                    .header(props.getProperty("api.token.http.header"), token)
                    .get("/api/datasets/" + dsId)
                    .then().assertThat().statusCode(200)
                    .extract().jsonPath().getString("data.latestVersion.files[1].directoryLabel");
            System.out.println("directoryLabel 2: " + dirLabel2);
            assertEquals("data/subdir2/", dirLabel2);

        } catch (Exception e) {
            System.out.println("Error testJsonParserWithDirectoryLabels: " + e.getMessage());
            e.printStackTrace();
            fail();
        }

    }

}