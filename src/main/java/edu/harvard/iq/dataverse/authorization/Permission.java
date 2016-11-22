package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * All the permissions in the system are implemented as enum values in this
 * class. For performance, the permissions are stored internally in a bit field
 * (in effect, a {@code long}). This brings database fetches to a single action
 * rather than a join, and in-memory permission set unions to a bitwise or 
 * rather than a tree merge. But some caution must be practiced when making
 * changes to this class.
 * 
 * =========================================================
 * IMPORTANT NOTES, READ BEFORE MAKING CHANGES TO THIS FILE
 * =========================================================
 * 
 * 1. Number of permissions must be kept under 64. If more 
 *    than 64 permissions are needed, storage must be updated
 *    to include two {@code long}s, rather then the current one.
 * 2. Do not change the order of the enum values, and add new values only
 *    after the last enum value. If you wish to change the order or add a
 *    permission in between existing ones (or at the beginning), ALSO PROVIDE
 *    A MIGRATION SCRIPT FOR THE DATABASE. Otherwise, permissions in the
 *    database will be mis-assigned. This may be a major security issue.
 *
 * @author michael
 */
public enum Permission implements java.io.Serializable {
    
    // Create
    AddDataverse(java.util.ResourceBundle.getBundle("Bundle").getString("permission.addDataverseDataverse"), true, Dataverse.class),
    AddDataset(java.util.ResourceBundle.getBundle("Bundle").getString("permission.addDatasetDataverse"), true, Dataverse.class),     
    // Read
    ViewUnpublishedDataverse(java.util.ResourceBundle.getBundle("Bundle").getString("permission.viewUnpublishedDataverse"), false, Dataverse.class),
    ViewUnpublishedDataset(java.util.ResourceBundle.getBundle("Bundle").getString("permission.viewUnpublishedDataset"), false, Dataset.class),    
    DownloadFile(java.util.ResourceBundle.getBundle("Bundle").getString("permission.downloadFile"), false, DataFile.class),
    // Update
    EditDataverse(java.util.ResourceBundle.getBundle("Bundle").getString("permission.editDataverse"), true, Dataverse.class),
    EditDataset(java.util.ResourceBundle.getBundle("Bundle").getString("permission.editDataset"), true, Dataset.class),
    ManageDataversePermissions(java.util.ResourceBundle.getBundle("Bundle").getString("permission.managePermissionsDataverse"), true, Dataverse.class),
    ManageDatasetPermissions(java.util.ResourceBundle.getBundle("Bundle").getString("permission.managePermissionsDataset"), true, Dataset.class), 
    PublishDataverse(java.util.ResourceBundle.getBundle("Bundle").getString("permission.publishDataverse"), true, Dataverse.class),
    PublishDataset(java.util.ResourceBundle.getBundle("Bundle").getString("permission.publishDataset"), true, Dataset.class),     
    // Delete
    DeleteDataverse(java.util.ResourceBundle.getBundle("Bundle").getString("permission.deleteDataverse"), true, Dataverse.class),    
    DeleteDatasetDraft(java.util.ResourceBundle.getBundle("Bundle").getString("permission.deleteDataset"), true, Dataset.class);

    // FUTURE:
    //RestrictMetadata("Mark metadata as restricted", DvObject.class),
    //AccessRestrictedMetadata("Access metadata marked as\"restricted\"", DvObject.class),
    
    /**
     * A human readable name for the permission.
     */
    private final String humanName;

    /**
     * Which types of {@link DvObject}s this permission applies to.
     */
    private final Set<Class<? extends DvObject>> appliesTo;
    
    /**
     * Can this permission be applied only to {@link AuthenticatedUser}s, or to any user?
     */
    private final boolean requiresAuthenticatedUser;

    Permission(String aHumanName, boolean authenticatedUserRequired, Class<? extends DvObject>... appliesToList) {
        humanName = aHumanName;
        appliesTo = new HashSet<>(Arrays.asList(appliesToList));
        requiresAuthenticatedUser = authenticatedUserRequired;
    }

    public String getHumanName() {
        return humanName;
    }

    public boolean appliesTo(Class<? extends DvObject> aClass) {
        for (Class<? extends DvObject> c : appliesTo) {
            if (c.isAssignableFrom(aClass)) {
                return true;
            }
        }
        return false;
    }

    public boolean requiresAuthenticatedUser() {
        return requiresAuthenticatedUser;
    }
    
   
}
