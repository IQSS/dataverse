package edu.harvard.iq.dataverse.api.helpers;

import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;

import java.util.Map;

/**
 * A class to represent a random user, retrieved from a Dataverse instance via its API
 *
 * There are some often used detail fields accessible via methods added for convenience.
 * All fields from the JSON response may be accessed, too.
 */
public class RandomUser {
    
    private static final String statusPath = "status";
    private static final String dataPath = "data";
    private static final String apiTokenPath = dataPath+".apiToken";
    private static final String usernamePath = dataPath+".user.userName";
    private static final String authUserPath = dataPath+".authenticatedUser";
    private static final String persistentUserIdPath = authUserPath+".persistentUserId";
    
    private final JsonPath json;
    
    /**
     * Package-private on intention - should only be created by {@link RandomUserExtension}
     */
    RandomUser(Response response) {
        this.json = response.jsonPath();
    }
    
    public String getStatus() {
        return json.get(statusPath);
    }
    
    public String getApiToken() {
        return json.get(apiTokenPath);
    }
    
    public String getUsername() {
        return json.get(usernamePath);
    }
    
    public Map<String, String> getAuthenticatedUser() {
        return json.get(authUserPath);
    }
    
    public String getPersistentUserId() {
        return json.get(persistentUserIdPath);
    }
    
    public String toString() {
        return "RandomUser["+json.getJsonObject("").toString()+"]";
    }
}
