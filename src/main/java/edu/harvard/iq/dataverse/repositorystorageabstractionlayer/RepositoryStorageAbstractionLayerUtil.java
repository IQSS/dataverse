package edu.harvard.iq.dataverse.repositorystorageabstractionlayer;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.StorageLocation;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class RepositoryStorageAbstractionLayerUtil {

    private static final Logger logger = Logger.getLogger(RepositoryStorageAbstractionLayerUtil.class.getCanonicalName());

    public static List<RsyncSite> getRsyncSites(Dataset dataset, JsonArray rsalSitesAsJson) {
        List<RsyncSite> rsalSites = new ArrayList<>();
        boolean leafDirectoryOnly = false;
        String fullRemotePathToDirectory = getDirectoryContainingTheData(dataset, leafDirectoryOnly);
        for (JsonObject site : rsalSitesAsJson.getValuesAs(JsonObject.class)) {
            String name = site.getString("name");
            String fqdn = site.getString("fqdn");
            RsyncSite rsyncSite = new RsyncSite(name, fqdn, fullRemotePathToDirectory);
            rsalSites.add(rsyncSite);
        }
        return rsalSites;
    }

    static String getLocalDataAccessDirectory(String localDataAccessParentDir, Dataset dataset) {
        if (localDataAccessParentDir == null) {
            localDataAccessParentDir = File.separator + "UNCONFIGURED ( " + SettingsServiceBean.Key.LocalDataAccessPath + " )";
        }
        boolean leafDirectoryOnly = false;
        return localDataAccessParentDir + File.separator + getDirectoryContainingTheData(dataset, leafDirectoryOnly);
    }

    static String getVerifyDataCommand(Dataset dataset) {
        boolean leafDirectoryOnly = true;
        // TODO: if "files.sha" is defined somewhere, use it.
        return "cd " + getDirectoryContainingTheData(dataset, leafDirectoryOnly) + " ; shasum -c files.sha";
    }

    /**
     * @param leafDirectoryOnly By "leaf" directory, we mean "4LKKNW" rather
     * than "10.5072/FK2/4LKKNW". On Unix if you run `basename /usr/local/bin`
     * you get `bin`, which is what we want when we specify "true" for
     * leafDirectoryOnly. See also
     * http://www.gnu.org/software/coreutils/manual/html_node/basename-invocation.html
     */
    public static String getDirectoryContainingTheData(Dataset dataset, boolean leafDirectoryOnly) {
        /**
         * FIXME: What if there is more than one package in the dataset?
         * Shouldn't the directory be based on the package rather than the
         * "authority" and "identifier" values of the persistent ID of the
         * dataset? How will we support multiple packages in one dataset?
         *
         * By "leaf" we mean "4LKKNW" rather than "10.5072/FK2/4LKKNW"
         */
        boolean onlyOnPackagePerDatasetIsSupported = true;
        if (onlyOnPackagePerDatasetIsSupported) {
            String leafDirectory = dataset.getIdentifier();
            if (leafDirectoryOnly) {
                return leafDirectory;
            } else {
                // The "authority" is something like "10.5072/FK2".
                String relativePathToLeafDir = dataset.getAuthority();
                return relativePathToLeafDir + File.separator + leafDirectory;
            }
        } else {
            throw new RuntimeException("Sorry, only one package per dataset is supported.");
        }
    }

    /**
     * The reason this in JSON is that it probably makes sense to someday query
     * RSAL or some other "big data" component live for a list of remotes sites
     * to which a particular dataset is replicated to.
     */
    static JsonArray getSitesFromDb(List<StorageLocation> storageLocations) {
        JsonArrayBuilder arraybuilder = Json.createArrayBuilder();
        if (storageLocations == null || storageLocations.isEmpty()) {
            return arraybuilder.build();
        }
        // Right now we have all the data right in the database setting but we should probably query RSAL to get the list.
        for (StorageLocation storageLocation : storageLocations) {
            arraybuilder.add(Json.createObjectBuilder()
                    .add("fqdn", storageLocation.getHostname())
                    .add("name", storageLocation.getName()));
        }
        return arraybuilder.build();
    }

}
