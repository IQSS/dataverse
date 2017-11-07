package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import static javax.ws.rs.core.Response.Status.OK;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExternalToolsIT {

    @BeforeClass
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testGetExternalTools() {
        Response getExternalTools = UtilIT.getExternalTools();
        getExternalTools.prettyPrint();
    }

    @Test
    public void getExternalToolsByFileId() {
        // FIXME: Don't hard code the file id. Make a dataset.
        long fileId = 12;
        Response getExternalTools = UtilIT.getExternalToolsByFileId(fileId);
        getExternalTools.prettyPrint();
    }

    @Test
    public void testAddExternalTool() throws IOException {
        String psiTool = new String(java.nio.file.Files.readAllBytes(Paths.get("doc/sphinx-guides/source/_static/installation/files/root/external-tools/psi.json")));
        JsonReader jsonReader = Json.createReader(new StringReader(psiTool));
        JsonObject obj = jsonReader.readObject();
        Response addExternalTool = UtilIT.addExternalTool(obj);
        addExternalTool.prettyPrint();
        addExternalTool.then().assertThat()
                .body("data.displayName", CoreMatchers.equalTo("Privacy-Preserving Data Preview"))
                .statusCode(OK.getStatusCode());
    }

}
