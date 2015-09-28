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
    AddDataverse("Add a dataverse within another dataverse", true, Dataverse.class),
    AddDataset("Add a dataset to a dataverse", true, Dataverse.class),     
    // Read
    ViewUnpublishedDataverse("View an unpublished dataverse", false, Dataverse.class),
    ViewUnpublishedDataset("View an unpublished dataset and its files", false, Dataset.class),    
    DownloadFile("Download a file", false, DataFile.class),
    // Update
    EditDataverse("Edit a dataverse's metadata, facets, customization, and templates ", true, Dataverse.class),
    EditDataset("Edit a dataset's metadata", true, Dataset.class),
    ManageDataversePermissions("Manage permissions for a dataverse", true, Dataverse.class),
    ManageDatasetPermissions("Manage permissions for a dataset", true, Dataset.class), 
    PublishDataverse("Publish a dataverse", true, Dataverse.class),
    PublishDataset("Publish a dataset", true, Dataset.class),     
    // Delete
    DeleteDataverse("Delete an unpublished dataverse", true, Dataverse.class),    
    DeleteDatasetDraft("Delete a dataset draft", true, Dataset.class);

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
