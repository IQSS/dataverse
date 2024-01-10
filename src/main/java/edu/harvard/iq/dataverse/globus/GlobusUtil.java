package edu.harvard.iq.dataverse.globus;

import java.util.List;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.GlobusAccessibleStore;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

public class GlobusUtil {

    public static JsonObject getFilesMap(List<DataFile> dataFiles, Dataset d) {
        JsonObjectBuilder filesBuilder = Json.createObjectBuilder();
        for (DataFile df : dataFiles) {
            String storageId = df.getStorageIdentifier();
            String[] parts = DataAccess
                    .getDriverIdAndStorageLocation(DataAccess.getLocationFromStorageId(storageId, d));
            String driverId = parts[0];
            String fileLocation = parts[1];
            if (GlobusAccessibleStore.isDataverseManaged(driverId)) {
                String endpointWithBasePath = GlobusAccessibleStore.getTransferEnpointWithPath(driverId);
                fileLocation = endpointWithBasePath + "/" + fileLocation;
            } else {
                fileLocation = storageId.substring(storageId.lastIndexOf("//") + 2);
            }
            filesBuilder.add(df.getId().toString(), fileLocation);
        }
        return filesBuilder.build();
    }
}