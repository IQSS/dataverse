package edu.harvard.iq.dataverse.repositorystorageabstractionlayer;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.locality.StorageSite;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

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
        boolean leafDirectoryOnly = true;
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
            String leafDirectory = dataset.getIdentifierForFileStorage();
            if (leafDirectoryOnly) {
		    File f = new File( leafDirectory );
		    return f.getName();
            } else {
                // The "authority" is something like "FK2".
                String relativePathToLeafDir = dataset.getAuthorityForFileStorage();
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
    static JsonArray getStorageSitesAsJson(List<StorageSite> storageSites) {
        JsonArrayBuilder arraybuilder = Json.createArrayBuilder();
        if (storageSites == null || storageSites.isEmpty()) {
            return arraybuilder.build();
        }
        // Right now we have all the data right in the database setting but we should probably query RSAL to get the list.
        int countOfPrimarySites = 0;
        for (StorageSite storageSite : storageSites) {
            if (storageSite.isPrimaryStorage()) {
                countOfPrimarySites++;
            }
            arraybuilder.add(Json.createObjectBuilder()
                    .add("fqdn", storageSite.getHostname())
                    .add("name", storageSite.getName()));
        }
        int numExpectedPrimarySites = 1;
        if (countOfPrimarySites != numExpectedPrimarySites) {
            logger.warning("The number of expected primary sites is " + numExpectedPrimarySites + " but " + countOfPrimarySites + " were found.");
        }
        return arraybuilder.build();
    }

}
