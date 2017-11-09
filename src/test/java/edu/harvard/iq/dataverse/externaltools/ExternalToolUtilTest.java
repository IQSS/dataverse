package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ExternalToolUtilTest {

    @Test
    public void testParseAddExternalToolInput() throws IOException {
        assertEquals(null, ExternalToolUtil.parseAddExternalToolInput(null));
        assertEquals(null, ExternalToolUtil.parseAddExternalToolInput(""));
        assertEquals(null, ExternalToolUtil.parseAddExternalToolInput(Json.createObjectBuilder().build().toString()));
        String psiTool = new String(Files.readAllBytes(Paths.get("doc/sphinx-guides/source/_static/installation/files/root/external-tools/psi.json")));
        System.out.println("psiTool: " + psiTool);
        ExternalTool externalTool = ExternalToolUtil.parseAddExternalToolInput(psiTool);
        assertEquals("Privacy-Preserving Data Preview", externalTool.getDisplayName());
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        ExternalToolHandler externalToolHandler = new ExternalToolHandler(externalTool, dataFile, apiToken);
        String toolUrl = ExternalToolUtil.getToolUrlWithQueryParams(externalToolHandler, externalTool);
        System.out.println("result: " + toolUrl);
        assertEquals("https://beta.dataverse.org/custom/DifferentialPrivacyPrototype/UI/code/interface.html?fileid=42&key=7196b5ce-f200-4286-8809-03ffdbc255d7", toolUrl);

    }

    @Test
    public void testGetToolUrlWithOptionalQueryParameters() {
        String toolUrl = "http://example.com";
        ExternalTool externalTool = new ExternalTool("displayName", "description", toolUrl, "{}");

        // One query parameter.
        externalTool.setToolParameters(Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("key1", "value1")
                        )
                )
                .build().toString());
        DataFile nullDataFile = null;
        ApiToken nullApiToken = null;
        ExternalToolHandler externalToolHandler1 = new ExternalToolHandler(externalTool, nullDataFile, nullApiToken);
        String result1 = ExternalToolUtil.getQueryParametersForUrl(externalToolHandler1, externalTool);
        System.out.println("result1: " + result1);
        assertEquals("?key1=value1", result1);

        // Two query parameters.
        externalTool.setToolParameters(Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("key1", "value1")
                        )
                        .add(Json.createObjectBuilder()
                                .add("key2", "value2")
                        )
                )
                .build().toString());
        ExternalToolHandler externalToolHandler2 = new ExternalToolHandler(externalTool, nullDataFile, nullApiToken);
        String result2 = ExternalToolUtil.getQueryParametersForUrl(externalToolHandler2, externalTool);
        System.out.println("result2: " + result2);
        assertEquals("?key1=value1&key2=value2", result2);

        // Two query parameters, both reserved words
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
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        ExternalToolHandler externalToolHandler3 = new ExternalToolHandler(externalTool, dataFile, apiToken);
        String result3 = ExternalToolUtil.getQueryParametersForUrl(externalToolHandler3, externalTool);
        System.out.println("result3: " + result3);
        assertEquals("?key1=42&key2=7196b5ce-f200-4286-8809-03ffdbc255d7", result3);

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
        ExternalToolHandler externalToolHandler4 = new ExternalToolHandler(externalTool, dataFile, nullApiToken);
        String result4 = ExternalToolUtil.getQueryParametersForUrl(externalToolHandler4, externalTool);
        System.out.println("result4: " + result4);
        assertEquals("?key1=42&key2=null", result4);
    }

    @Test
    public void testfindAll() {
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        ExternalTool externalTool = new ExternalTool("displayName", "description", "http://foo.com", "{}");
        ExternalToolHandler externalToolHandler4 = new ExternalToolHandler(externalTool, dataFile, apiToken);
        List<ExternalTool> externalTools = new ArrayList<>();
        externalTools.add(externalTool);
        List<ExternalToolHandler> externalToolHandlers = ExternalToolUtil.findAll(externalTools, dataFile, apiToken);
        assertEquals(dataFile.getId(), externalToolHandlers.get(0).getDataFile().getId());
    }

}
