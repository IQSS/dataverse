package edu.harvard.iq.dataverse.github;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObjectBuilder;

public class GithubUtil {

    private static final Logger logger = Logger.getLogger(GithubUtil.class.getCanonicalName());

    public static JsonObjectBuilder fetchGithubMetadata(URI githubRepoUrl) throws MalformedURLException, URISyntaxException, UnirestException {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        String org = githubRepoUrl.getPath().split("/")[1];
        String repo = githubRepoUrl.getPath().split("/")[2];
        URI githubApiUrl = new URI("https://api.github.com/repos/" + org + "/" + repo);
        HttpResponse<JsonNode> response = Unirest.get(githubApiUrl.toURL().toString()).asJson();
        String jsonOut = response.getBody().getObject().toString();
        jsonObjectBuilder.add("repoUrl", githubRepoUrl.toURL().toString());
        jsonObjectBuilder.add("githubApiUrl", githubApiUrl.toURL().toString());
        jsonObjectBuilder.add("metadata", jsonOut);
        return jsonObjectBuilder;
    }

}
