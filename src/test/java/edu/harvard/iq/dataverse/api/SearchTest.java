package edu.harvard.iq.dataverse.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import javax.json.Json;
import javax.json.JsonObject;
import static org.junit.Assert.assertEquals;
import org.junit.Ignore;
import org.junit.Test;

public class SearchTest {

    @Test
    public void testSearchNoParameters() {
        Search search = new Search();
        String actual = search.search(null);
        String expected = "{\"message\":\"Validation Failed\",\"documentation_url\":\"http://thedata.org\",\"errors\":[{\"field\":\"q\",\"code\":\"missing\"}]}";
        assertEquals(expected, actual);
    }

    // skipping for now... this is more of an integration test... it requires a web server
//    @Test
//    @Ignore
//    public void testSearchHttpGet() throws Exception {
//        URL url = new URL("http://localhost:8080/api/search");
//        URLConnection uRLConnection = url.openConnection();
//        BufferedReader in = new BufferedReader(new InputStreamReader(uRLConnection.getInputStream()));
//        assertEquals("{\"message\":\"Validation Failed\",\"documentation_url\":\"http://thedata.org\"}", in.readLine());
//    }

}
