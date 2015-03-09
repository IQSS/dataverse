package edu.harvard.iq.dataverse.authorization.providers.shib;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.logging.Logger;

public class ShibUtil {

    private static final Logger logger = Logger.getLogger(ShibUtil.class.getCanonicalName());

    /**
     * @todo Use this to display "Harvard University", for example, based on
     * https://dataverse.harvard.edu/Shibboleth.sso/DiscoFeed
     */
    public static String getDisplayNameFromDiscoFeed(String entityIdToFind, String discoFeed) {
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

    /**
     * @param displayName Not (yet) used. See @todo.
     *
     * @todo Do something with displayName. By comparing displayName to the
     * firstName and lastName strings, we should be able to figure out where the
     * proper split is, like this:
     *
     * - "Guido|van Rossum"
     *
     * - "Philip Seymour|Hoffman"
     *
     * We're not sure how many Identity Providers (IdP) will send us
     * "displayName" so we'll hold off on implementing anything for now.
     */
    public static ShibUserNameFields findBestFirstAndLastName(String firstName, String lastName, String displayName) {
        firstName = getSingleName(firstName);
        lastName = getSingleName(lastName);
        return new ShibUserNameFields(firstName, lastName);
    }

    private static String getSingleName(String name) {
        String[] parts = name.split(";");
        if (parts.length != 1) {
            logger.fine("parts (before sorting): " + Arrays.asList(parts));
            // predictable order (sorted alphabetically)
            Arrays.sort(parts);
            logger.fine("parts (after sorting): " + Arrays.asList(parts));
            try {
                String first = parts[0];
                name = first;
            } catch (ArrayIndexOutOfBoundsException ex) {
                logger.info("Couldn't find first part of " + name);
            }
        }
        return name;
    }
}
