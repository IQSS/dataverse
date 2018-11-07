package edu.harvard.iq.dataverse.github;

import com.mashape.unirest.http.exceptions.UnirestException;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.json.JsonObject;
import org.junit.Test;

public class GithubUtilTest {

    @Test
    public void testFetchGithubMetadata() throws MalformedURLException, URISyntaxException, UnirestException {
        String githubUrl = "https://github.com/IQSS/metrics.dataverse.org";
        URI githubRepo = new URI(githubUrl);
        JsonObject jsonObject = GithubUtil.fetchGithubMetadata(githubRepo).build();
        System.out.println(JsonUtil.prettyPrint(jsonObject));
    }

}
