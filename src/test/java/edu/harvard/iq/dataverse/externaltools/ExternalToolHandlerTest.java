package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.json.Json;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ExternalToolHandlerTest {

    @Test
    public void testParseAddExternalToolInput() throws Exception {
        assertEquals(null, ExternalToolHandler.parseAddExternalToolInput(null));
        assertEquals(null, ExternalToolHandler.parseAddExternalToolInput(""));
        assertEquals(null, ExternalToolHandler.parseAddExternalToolInput(Json.createObjectBuilder().build().toString()));
        String psiTool = new String(Files.readAllBytes(Paths.get("doc/sphinx-guides/source/_static/installation/files/root/external-tools/psi.json")));
        System.out.println("psiTool: " + psiTool);
        ExternalTool externalTool = ExternalToolHandler.parseAddExternalToolInput(psiTool);
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        externalTool.setDataFile(dataFile);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        externalTool.setApiToken(apiToken);
        String toolUrl = externalTool.getToolUrl();
        System.out.println("result: " + toolUrl);
        assertEquals("https://beta.dataverse.org/custom/DifferentialPrivacyPrototype/UI/code/interface.html?fileid=42&key=7196b5ce-f200-4286-8809-03ffdbc255d7", toolUrl);

    }

    @Test
    public void testGetToolUrlWithOptionalQueryParameters() {
        ExternalTool externalTool = new ExternalTool();
        externalTool.setToolUrl("http://example.com");

        // One query parameter.
        externalTool.setToolParameters(Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("key1", "value1")
                        )
                )
                .build().toString());
        String result1 = ExternalToolHandler.getQueryParametersForUrl(externalTool);
        System.out.println("result1: " + result1);
        assertEquals("?key1=value1", ExternalToolHandler.getQueryParametersForUrl(externalTool));

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
        String result2 = ExternalToolHandler.getQueryParametersForUrl(externalTool);
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
        String result3 = ExternalToolHandler.getQueryParametersForUrl(externalTool, dataFile, apiToken);
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
        ApiToken nullApiToken = null;
        String result4 = ExternalToolHandler.getQueryParametersForUrl(externalTool, dataFile, nullApiToken);
        System.out.println("result4: " + result4);
        assertEquals("?key1=42&key2=null", result4);

    }

}
