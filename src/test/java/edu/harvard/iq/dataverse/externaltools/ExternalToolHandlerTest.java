package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

public class ExternalToolHandlerTest {

    // TODO: It would probably be better to split these into individual tests.
    @Test
    public void testGetToolUrlWithOptionalQueryParameters() {
        List<ExternalToolType> externalToolTypes = new ArrayList<>();
        ExternalToolType externalToolType = new ExternalToolType();
        externalToolType.setType(ExternalTool.Type.EXPLORE);
        externalToolTypes.add(externalToolType);
        ExternalTool.Scope scope = ExternalTool.Scope.FILE;
        String toolUrl = "http://example.com";
        ExternalTool externalTool = new ExternalTool("displayName", "toolName", "description", externalToolTypes, scope, toolUrl, "{}", DataFileServiceBean.MIME_TYPE_TSV_ALT);

        // One query parameter, not a reserved word, no {fileId} (required) used.
        externalTool.setToolParameters(Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("mode", "mode1")
                        )
                )
                .build().toString());
        DataFile nullDataFile = null;
        ApiToken nullApiToken = null;
        FileMetadata nullFileMetadata = null;
        Exception expectedException1 = null;
        String nullLocaleCode = null;
        try {
            ExternalToolHandler externalToolHandler1 = new ExternalToolHandler(externalTool, nullDataFile, nullApiToken, nullFileMetadata, nullLocaleCode);
        } catch (Exception ex) {
            expectedException1 = ex;
        }
        assertNotNull(expectedException1);
        assertEquals("A DataFile is required.", expectedException1.getMessage());

        // One query parameter, not a reserved word, no {fileMetadata} (required) used.
        externalTool.setToolParameters(Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("mode", "mode1")
                        )
                )
                .build().toString());
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        try {
            ExternalToolHandler externalToolHandler1 = new ExternalToolHandler(externalTool, dataFile, nullApiToken, nullFileMetadata, nullLocaleCode);
        } catch (Exception ex) {
            expectedException1 = ex;
        }
        assertNotNull(expectedException1);
        assertEquals("A FileMetadata is required.", expectedException1.getMessage());

        
        // Two query parameters.
        externalTool.setToolParameters(Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("mode", "mode1")
                        )
                        .add(Json.createObjectBuilder()
                                .add("key2", "value2")
                        )
                )
                .build().toString());
        Exception expectedException2 = null;
        try {
            ExternalToolHandler externalToolHandler2 = new ExternalToolHandler(externalTool, nullDataFile, nullApiToken, nullFileMetadata, nullLocaleCode);
        } catch (Exception ex) {
            expectedException2 = ex;
        }
        assertNotNull(expectedException2);
        assertEquals("A DataFile is required.", expectedException2.getMessage());

        // Two query parameters, both reserved words, one is {fileId} which is required.
        externalTool.setToolParameters(Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("key1", "{fileId}")
                        )
                        .add(Json.createObjectBuilder()
                                .add("key2", "{apiToken}")
                        )
                )
                .build().toString());

        FileMetadata fmd = new FileMetadata();
        DatasetVersion dv = new DatasetVersion();
        Dataset ds = new Dataset();
        dv.setDataset(ds);
        fmd.setDatasetVersion(dv);
        List<FileMetadata> fmdl = new ArrayList<FileMetadata>();
        fmdl.add(fmd);
        dataFile.setFileMetadatas(fmdl);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        ExternalToolHandler externalToolHandler3 = new ExternalToolHandler(externalTool, dataFile, apiToken, fmd, nullLocaleCode);
        String result3 = externalToolHandler3.handleRequest();
        System.out.println("result3: " + result3);
        assertEquals("?key1=42&key2=7196b5ce-f200-4286-8809-03ffdbc255d7", result3);

        // Three query parameters, all reserved words, two {fileId}{fileMetadataId} which are required.
        fmd.setId(2L);
        externalTool.setToolParameters(Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("key1", "{fileId}")
                        )
                        .add(Json.createObjectBuilder()
                                .add("key2", "{apiToken}")
                        )
                        .add(Json.createObjectBuilder()
                                .add("key3", "{fileMetadataId}")
                        )
                )
                .build().toString());
        ExternalToolHandler externalToolHandler6 = new ExternalToolHandler(externalTool, dataFile, apiToken, fmd, nullLocaleCode);
        String result6 = externalToolHandler6.handleRequest();
        System.out.println("result6: " + result6);
        assertEquals("?key1=42&key2=7196b5ce-f200-4286-8809-03ffdbc255d7&key3=2", result6);

        // Two query parameters, both reserved words, no apiToken
        externalTool.setToolParameters(Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("key1", "{fileId}")
                        )
                        .add(Json.createObjectBuilder()
                                .add("key2", "{apiToken}")
                        )
                )
                .build().toString());
        ExternalToolHandler externalToolHandler4 = new ExternalToolHandler(externalTool, dataFile, nullApiToken, fmd, nullLocaleCode);
        String result4 = externalToolHandler4.handleRequest();
        System.out.println("result4: " + result4);
        assertEquals("?key1=42", result4);

        //localeCode test
        externalTool.setToolParameters(Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("key1", "{fileId}")
                        )
                        .add(Json.createObjectBuilder()
                                .add("key2", "{apiToken}")
                        )
                        .add(Json.createObjectBuilder()
                                .add("key3", "{fileMetadataId}")
                        )
                        .add(Json.createObjectBuilder()
                                .add("key4", "{localeCode}")
                        )
                )
                .build().toString());
        ExternalToolHandler externalToolHandler7 = new ExternalToolHandler(externalTool, dataFile, apiToken, fmd, "en");
        String result7 = externalToolHandler7.handleRequest();
        System.out.println("result7: " + result7);
        assertEquals("?key1=42&key2=7196b5ce-f200-4286-8809-03ffdbc255d7&key3=2&key4=en", result7);

        // Two query parameters, attempt to use a reserved word that doesn't exist.
        externalTool.setToolParameters(Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("key1", "{junk}")
                        )
                        .add(Json.createObjectBuilder()
                                .add("key2", "{apiToken}")
                        )
                )
                .build().toString());
        Exception expectedException = null;
        try {
            ExternalToolHandler externalToolHandler5 = new ExternalToolHandler(externalTool, dataFile, nullApiToken, fmd, nullLocaleCode);
            String result5 = externalToolHandler5.handleRequest();
            System.out.println("result5: " + result5);
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("Unknown reserved word: {junk}", expectedException.getMessage());

    }
    
    
    @Test
    @JvmSetting(key = JvmSettings.SITE_URL, value = "https://librascholar.org")
    public void testGetToolUrlWithAllowedApiCalls() {
        System.out.println("allowedApiCalls test");
        Dataset ds = new Dataset();
        ds.setId(1L);
        ApiToken at = new ApiToken();
        AuthenticatedUser au = new AuthenticatedUser();
        au.setUserIdentifier("dataverseAdmin");
        at.setAuthenticatedUser(au);
        at.setTokenString("1234");
        ExternalTool et = ExternalToolServiceBeanTest.getAllowedApiCallsTool();
        assertTrue(et != null);
        System.out.println("allowedApiCalls et created");
        System.out.println(et.getAllowedApiCalls());
        ExternalToolHandler externalToolHandler = new ExternalToolHandler(et, ds, at, null);
        System.out.println("allowedApiCalls eth created");
        JsonObject jo = externalToolHandler
                .createPostBody(externalToolHandler.getParams(JsonUtil.getJsonObject(et.getToolParameters()))).build();
        assertEquals(1, jo.getJsonObject("queryParameters").getInt("datasetId"));
        String signedUrl = jo.getJsonArray("signedUrls").getJsonObject(0).getString("signedUrl");
        // The date and token will change each time but check for the constant parts of
        // the response
        assertTrue(signedUrl.contains("https://librascholar.org/api/v1/datasets/1"));
        assertTrue(signedUrl.contains("&user=dataverseAdmin"));
        assertTrue(signedUrl.contains("&method=GET"));
        assertTrue(signedUrl.contains("&token="));
        System.out.println(JsonUtil.prettyPrint(jo));
    }
}
