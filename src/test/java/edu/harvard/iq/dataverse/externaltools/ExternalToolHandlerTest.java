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
        assertEquals("https://beta.dataverse.org/custom/DifferentialPrivacyPrototype/", ExternalToolHandler.parseAddExternalToolInput(psiTool).getToolUrl());
    }

}
