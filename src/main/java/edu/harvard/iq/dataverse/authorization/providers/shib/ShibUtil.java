package edu.harvard.iq.dataverse.authorization.providers.shib;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ShibUtil {

    /**
     * @todo Use this to display "Harvard University", for example, based on
     * https://dataverse.harvard.edu/Shibboleth.sso/DiscoFeed
     */
    public String getDisplayNameFromDiscoFeed(String entityIdToFind, String discoFeed) {
        JsonParser jsonParser = new JsonParser();
        JsonElement root = jsonParser.parse(discoFeed);
        JsonArray identityProviders = root.getAsJsonArray();
        for (JsonElement identityProvider : identityProviders) {
            JsonObject provider = identityProvider.getAsJsonObject();
            JsonElement entityId = provider.get("entityID");
            if (entityId != null) {
                if (entityId.getAsString().equals(entityIdToFind)) {
                    JsonArray displayNames = provider.get("DisplayNames").getAsJsonArray();
                    JsonElement firstDisplayName = displayNames.get(0);
                    String friendlyName = firstDisplayName.getAsJsonObject().get("value").getAsString();
                    return friendlyName;
                }
            }
        }
        return null;
    }

}
