package edu.harvard.iq.dataverse.api;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jakarta.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ISO639IT {

    /*
    This is a manual test that requires the following set-up and execution
    Not to be run CI/CD
    1. run appendCitationProperties() to modify the properties file
    2. Build and deploy dataverse (will deploy with the new properties file)
    3. run mergeISO639Languages() to test the /api/admin/datasetfield/mergeLanguageList endpoint
    4. Check in new citation.properties file
    */

    @Test
    public void appendCitationProperties() throws IOException {
        // load the existing properties file
        Map<String,String> props = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader("src/main/java/propertyFiles/citation.properties"));
        br.readLine(); // ignore first line as it is a header
        String line;
        while ((line = br.readLine()) != null) {
            String[] kv = line.split("=");
            if (kv.length == 2) {
                props.put(kv[0], kv[1]);
            }
        }
        br.close();

        // append any new properties to the existing file
        br = new BufferedReader(new FileReader("scripts/api/data/metadatablocks/iso-639-3_Code_Tables_20240415/iso-639-3.tab"));
        BufferedWriter bw = new BufferedWriter(new FileWriter("src/main/java/propertyFiles/citation.properties", true));
        br.readLine(); // ignore first line as it is a header
        while ((line = br.readLine()) != null) {
            String[] fields = line.split("\t");
            String name = fields[6];
            String sanitized = "controlledvocabulary.language." + StringUtils.stripAccents(name.trim().toLowerCase().replace(" ", "_"));
            if (!props.containsKey(sanitized)) {
                bw.write(sanitized + "=" + name + "\n");
            }
        }
        bw.flush();
        bw.close();
        br.close();
    }

    @Test
    public void mergeISO639Languages() throws IOException {

        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());
        byte[] updatedContent = Files.readAllBytes(Paths.get("scripts/api/data/metadatablocks/iso-639-3_Code_Tables_20240415/iso-639-3.tab"));

        Response response = UtilIT.mergeLanguages(superuserApiToken, updatedContent);
        String body = response.getBody().asString();

        assertTrue(200 == response.getStatusCode(), body);
        response.then().assertThat().statusCode(OK.getStatusCode());

        String status = JsonPath.from(body).getString("status");
        assertEquals("OK", status);

        Map<String, List<Map<String, String>>> data = JsonPath.from(body).getMap("data");
        assertEquals(1, data.size());
        List<Map<String, String>> addedElements = data.get("added");
        //Note -test depends on the number of elements in the production citation block, so any changes to the # of elements there can break this test
        assertEquals(7920, addedElements.size());

        Map<String, Integer> statistics = new HashMap<>();
        for (Map<String, String> unit : addedElements) {
            assertEquals(2, unit.size());
            assertTrue(unit.containsKey("name"));
            assertTrue(unit.containsKey("type"));
            String type = unit.get("type");
            if (!statistics.containsKey(type))
                statistics.put(type, 0);
            statistics.put(type, statistics.get(type) + 1);
        }

        assertEquals(1, statistics.size());
        assertEquals(7920, (int) statistics.get("Controlled Vocabulary"));
    }
}
