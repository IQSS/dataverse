package edu.harvard.iq.dataverse.externaltools;

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
        assertEquals("https://beta.dataverse.org/custom/DifferentialPrivacyPrototype/UI/code/interface.html", ExternalToolHandler.parseAddExternalToolInput(psiTool).getToolUrl());
        ExternalTool externalTool = ExternalToolHandler.parseAddExternalToolInput(psiTool);
        // TODO: How do we put the real file id in here?
        assertEquals("?fileid={fileId}", ExternalToolHandler.getQueryParametersForUrl(externalTool));
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
    }

}
