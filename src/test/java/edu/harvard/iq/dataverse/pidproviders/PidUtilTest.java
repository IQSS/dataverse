package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.util.json.JsonUtil;
import java.io.IOException;
import javax.json.JsonObjectBuilder;
import org.junit.Test;
import org.junit.Ignore;

public class PidUtilTest {

    /**
     * Useful for testing but requires DataCite credentials, etc.
     */
    @Ignore
    @Test
    public void testGetDoi() throws IOException {
        String username = System.getenv("DataCiteUsername");
        String password = System.getenv("DataCitePassword");
        String baseUrl = "https://api.test.datacite.org/dois/";
        String persistentId = "";
        persistentId = "10.70122/FK2/9BXT5O"; // findable
        persistentId = "10.70122/FK2/DOES-NOT-EXIST"; // does not exist
        persistentId = "10.70122/87W6-F672"; // draft
        JsonObjectBuilder result = PidUtil.queryDoi(persistentId, baseUrl, username, password);
        String out = JsonUtil.prettyPrint(result.build());
        System.out.println("out: " + out);
    }

}
