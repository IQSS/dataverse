package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;


import edu.harvard.iq.dataverse.datavariable.VarGroup;
import edu.harvard.iq.dataverse.datavariable.VariableMetadata;
import edu.harvard.iq.dataverse.datavariable.VariableMetadataDDIParser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import java.io.InputStream;
import java.io.IOException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;


import static jakarta.ws.rs.core.Response.Status.OK;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class EditDDIIT {

    private static final Logger logger = Logger.getLogger(EditDDIIT.class.getCanonicalName());

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testUpdateVariableMetadata() throws InterruptedException {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);


        String pathToJsonFile = "scripts/api/data/dataset-create-new.json";
        Response createDatasetResponse = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, apiToken);
        createDatasetResponse.prettyPrint();
        Long datasetId = JsonPath.from(createDatasetResponse.body().asString()).getLong("data.id");
        Integer datasetIdInt = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");

        String pathToFileThatGoesThroughIngest = "src/test/resources/sav/dct.sav";
        Response uploadIngestableFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFileThatGoesThroughIngest, apiToken);
        uploadIngestableFile.then().assertThat()
                .statusCode(OK.getStatusCode());
        uploadIngestableFile.prettyPrint();

        String origFileId = JsonPath.from(uploadIngestableFile.body().asString()).getString("data.files[0].dataFile.id");

        System.out.println("Orig file id " + origFileId);

        logger.fine("Orig file id: " + origFileId);
        assertNotNull(origFileId);
        assertNotEquals("",origFileId);

        // Give file time to ingest
        
        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToFileThatGoesThroughIngest);
        
        Response origXml = UtilIT.getFileMetadata(origFileId, null, apiToken);
        assertEquals(200, origXml.getStatusCode());


        String stringOrigXml = origXml.getBody().prettyPrint();

        InputStream variableData = origXml.body().asInputStream();

        Map<Long, VariableMetadata> mapVarToVarMet = new HashMap<Long, VariableMetadata>();
        Map<Long,VarGroup> varGroupMap = new HashMap<Long, VarGroup>();
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader xmlr = factory.createXMLStreamReader(variableData);
            VariableMetadataDDIParser vmdp = new VariableMetadataDDIParser();

            vmdp.processDataDscr(xmlr, mapVarToVarMet, varGroupMap);

        } catch (XMLStreamException e) {
            logger.warning(e.getMessage());
            assertEquals(0,1);
        }


        //Test here
        String updatedContent = "";
        try {
            updatedContent = new String(Files.readAllBytes(Paths.get("src/test/resources/xml/dct.xml")));
        } catch (IOException e) {
            logger.warning(e.getMessage());
            assertEquals(0,1);
        }
        Long replV1168 = 0L;
        Long replV1169 = 0L;
        Long replV1170 = 0L;
        int numberVariables = 0;
        for (VariableMetadata var : mapVarToVarMet.values()) {
            if (var.getLabel().equals("gender")) {
                replV1170 = var.getDataVariable().getId();
                numberVariables = numberVariables +1;
            } else if (var.getLabel().equals("weight")) {
                replV1168 = var.getDataVariable().getId();
                numberVariables = numberVariables +1;
            } else if (var.getLabel().equals("age_rollup")) {
                replV1169 = var.getDataVariable().getId();
                numberVariables = numberVariables +1;
            }
        }
        assertEquals(3, numberVariables);

        updatedContent = updatedContent.replaceAll("v1168", "v" + replV1168 );
        updatedContent = updatedContent.replaceAll("v1169", "v" + replV1169 );
        updatedContent = updatedContent.replaceAll("v1170", "v" + replV1170 );

        //edit draft vesrsion
        Response editDDIResponse = UtilIT.editDDI(updatedContent, origFileId, apiToken);

        editDDIResponse.prettyPrint();
        assertEquals(200, editDDIResponse.getStatusCode());

        //publish draft version
        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());

        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetIdInt, "major", apiToken);
        assertEquals(200, publishDataset.getStatusCode());

        Response editDDIResponseNewDraft = UtilIT.editDDI(stringOrigXml, origFileId, apiToken);
        editDDIResponseNewDraft.prettyPrint();
        assertEquals(200, editDDIResponseNewDraft.getStatusCode());

        //not authorized
        Response editDDINotAuthResponse = UtilIT.editDDI(updatedContent, origFileId, null);
        assertEquals(401, editDDINotAuthResponse.getStatusCode());


        //cleanup
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());

        Response destroyDatasetResponse = UtilIT.destroyDataset(datasetIdInt, apiToken);
        destroyDatasetResponse.prettyPrint();
        assertEquals(200, destroyDatasetResponse.getStatusCode());


        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverseResponse.prettyPrint();
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());
    }

}
