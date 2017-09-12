package edu.harvard.iq.dataverse.repositorystorageabstractionlayer;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.FileMetadata;
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

    public static List<RsyncSite> getRsyncSites(Dataset dataset, FileMetadata fileMetadata, JsonArray rsalSitesAsJson) {
        List<RsyncSite> rsalSites = new ArrayList<>();
        String fullRemotePathToDirectory = getDirectoryContainingTheData(dataset, fileMetadata);
        for (JsonObject site : rsalSitesAsJson.getValuesAs(JsonObject.class)) {
            String name = site.getString("name");
            String fqdn = site.getString("fqdn");
            String country = site.getString("country");
            RsyncSite rsyncSite = new RsyncSite(name, fqdn, country, fullRemotePathToDirectory);
            rsalSites.add(rsyncSite);
        }
        return rsalSites;
    }

    static String getLocalDataAccessDirectory(String localDataAccessParentDir, Dataset dataset, FileMetadata fileMetadata) {
        if (localDataAccessParentDir == null) {
            localDataAccessParentDir = File.separator + "UNCONFIGURED ( " + SettingsServiceBean.Key.LocalDataAccessPath + " )";
        }
        dataset = findDatasetOrDie(dataset, fileMetadata);
        return localDataAccessParentDir + File.separator + getDirectoryContainingTheData(dataset, fileMetadata);
    }

    static String getVerifyDataCommand(Dataset dataset, FileMetadata fileMetadata) {
        // TODO: if "files.sha" is defined somewhere, use it.
        return "cd " + getDirectoryContainingTheData(dataset, fileMetadata) + " ; shasum -c files.sha";
    }

    private static String getDirectoryContainingTheData(Dataset dataset, FileMetadata fileMetadata) {
        if (fileMetadata != null) {
            dataset = fileMetadata.getDatasetVersion().getDataset();
        }
        /**
         * FIXME: What if there is more than one package in the dataset?
         * Shouldn't the directory be based on the package rather than the
         * "identifier" part of the persistent ID of the dataset? How will we
         * support multiple packages in one dataset?
         */
        return dataset.getIdentifier();
    }

    private static Dataset findDatasetOrDie(Dataset dataset, FileMetadata fileMetadata) {
        if (dataset != null) {
            return dataset;
        }
        if (fileMetadata != null) {
            dataset = fileMetadata.getDatasetVersion().getDataset();
            return dataset;
        }
        throw new RuntimeException("Cannot find dataset!");
    }

    /**
     * The reason this in JSON is that it probably makes sense to someday query
     * RSAL or some other "big data" component live for a list of remotes sites
     * to which a particular dataset is replicated to.
     */
    public static JsonArray getSitesFromDb(String replicationSitesInDB) {
        JsonArrayBuilder arraybuilder = Json.createArrayBuilder();
        if (replicationSitesInDB == null || replicationSitesInDB.isEmpty()) {
            return arraybuilder.build();
        }
        // Right now we have all the data right in the database setting but we should probably query RSAL to get the list.
        String[] sites = replicationSitesInDB.split(",");
        for (String site : sites) {
            String[] parts = site.split(":");
            arraybuilder.add(Json.createObjectBuilder()
                    .add("fqdn", parts[0])
                    .add("name", parts[1])
                    .add("country", parts[2])
            );
        }
        return arraybuilder.build();
    }

}
