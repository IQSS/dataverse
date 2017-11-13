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

public class ExternalToolServiceBeanTest {

    public ExternalToolServiceBeanTest() {
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
        List<ExternalToolHandler> externalToolHandlers = ExternalToolServiceBean.findAll(externalTools, dataFile, apiToken);
        assertEquals(dataFile.getId(), externalToolHandlers.get(0).getDataFile().getId());
    }

    @Test
    public void testParseAddExternalToolInput() throws IOException {
        assertEquals(null, ExternalToolServiceBean.parseAddExternalToolInput(null));
        assertEquals(null, ExternalToolServiceBean.parseAddExternalToolInput(""));
        assertEquals(null, ExternalToolServiceBean.parseAddExternalToolInput(Json.createObjectBuilder().build().toString()));
        String psiTool = new String(Files.readAllBytes(Paths.get("doc/sphinx-guides/source/_static/installation/files/root/external-tools/psi.json")));
        System.out.println("psiTool: " + psiTool);
        ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolInput(psiTool);
        assertEquals("Privacy-Preserving Data Preview", externalTool.getDisplayName());
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        ExternalToolHandler externalToolHandler = new ExternalToolHandler(externalTool, dataFile, apiToken);
        String toolUrl = externalToolHandler.getToolUrlWithQueryParams();
        System.out.println("result: " + toolUrl);
        assertEquals("https://beta.dataverse.org/custom/DifferentialPrivacyPrototype/UI/code/interface.html?fileid=42&key=7196b5ce-f200-4286-8809-03ffdbc255d7", toolUrl);
    }

}
