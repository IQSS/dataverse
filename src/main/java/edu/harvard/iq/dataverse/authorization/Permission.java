package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * All the permissions in the system are implemented as enum constants in this
 * class.
 *
 * KEEP THIS UNDER 64, or update the permission storage that currently uses a bit-field.
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
        for (Class c : appliesTo) {
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
