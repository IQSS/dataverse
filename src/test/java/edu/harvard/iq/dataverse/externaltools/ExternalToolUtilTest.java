package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.json.Json;
import org.junit.Test;
import static org.junit.Assert.*;

public class ExternalToolUtilTest {

    @Test
    public void testParseAddExternalToolInput() throws IOException {
        assertEquals(null, ExternalToolUtil.parseAddExternalToolInput(null));
        assertEquals(null, ExternalToolUtil.parseAddExternalToolInput(""));
        assertEquals(null, ExternalToolUtil.parseAddExternalToolInput(Json.createObjectBuilder().build().toString()));
        String psiTool = new String(Files.readAllBytes(Paths.get("doc/sphinx-guides/source/_static/installation/files/root/external-tools/psi.json")));
        System.out.println("psiTool: " + psiTool);
        ExternalTool externalTool = ExternalToolUtil.parseAddExternalToolInput(psiTool);
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        externalTool.setDataFile(dataFile);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        externalTool.setApiToken(apiToken);
        String toolUrl = externalTool.getToolUrlWithQueryParams();
        System.out.println("result: " + toolUrl);
        assertEquals("https://beta.dataverse.org/custom/DifferentialPrivacyPrototype/UI/code/interface.html?fileid=42&key=7196b5ce-f200-4286-8809-03ffdbc255d7", toolUrl);

    }

}
