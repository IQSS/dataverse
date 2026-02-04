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
    
    public static boolean isTaskCompleted(GlobusTaskState task) {
        if (task != null) {
            String status = task.getStatus();
            if (status != null) {
                if (status.equalsIgnoreCase("ACTIVE")) {
                    // We will take "ACTIVE" for face value, i.e., assume that 
                    // this really means that the task is still ongoing.
                    // (prior to 6.6 we used to assume that was only the case in 
                    // combination with "nice_status" being "ok" or "queued")
                    /*if (task.getNice_status().equalsIgnoreCase("ok")
                            || task.getNice_status().equalsIgnoreCase("queued")) {*/
                        return false;
                }
                return true;
            }
        }
        // if either task, or status is null - it likely indicates that there 
        // was an error contacting the task management api, and NOT that it has 
        // completed one way or another
        return false;
    }
    
    public static boolean isTaskSucceeded(GlobusTaskState task) {
        String status = null;
        if (task != null) {
            status = task.getStatus();
            if (status != null) {
                status = status.toUpperCase();
                if (status.equals("ACTIVE") || status.startsWith("FAILED") || status.startsWith("INACTIVE")) {
                    return false;
                } 
                return true;
            } 
        } 
        return false;
    }
    /**
     * Produces a human-readable Status label of a completed task
     * @param GlobusTaskState task - a looked-up state of a task as reported by Globus API
     */
    public static String getCompletedTaskStatus(GlobusTaskState task) {
        String status = null;
        if (task != null) {
            status = task.getStatus();
            if (status != null) {
                // The task is in progress but is not ok or queued
                // (L.A.) I think the assumption here is that this method is called 
                // exclusively on tasks that have already completed. So that's why
                // the code below assumes that "ACTIVE" means "FAILED". 
                if (status.equalsIgnoreCase("ACTIVE")) {
                    status = "FAILED" + "#" + task.getNice_status() + "#" + task.getNice_status_short_description();
                } else {
                    // The task is either succeeded, failed or inactive.
                    status = status + "#" + task.getNice_status() + "#" + task.getNice_status_short_description();
                }
            } else {
                status = "FAILED";
            }
        } else {
            // @todo are we sure? 
            status = "FAILED";
        }
        return status;
    }
}