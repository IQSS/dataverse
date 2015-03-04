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
 * KEEP THIS UNDER 64, or update the permission storage that currently use
 * bit-field
 *
 * @author michael
 */
public enum Permission implements java.io.Serializable {
    
    // Create
    AddDataverse("Add a dataverse within another dataverse", Dataverse.class),
    AddDataset("Add a dataset to a dataverse", Dataverse.class),     
    // Read
    ViewUnpublishedDataverse("View an unpublished dataverse", Dataverse.class),
    ViewUnpublishedDataset("View an unpublished dataset and its files", Dataset.class),    
    DownloadFile("Download a file", DataFile.class),
    // Update
    EditDataverse("Edit a dataverse's metadata, facets, customization, and templates ", Dataverse.class),
    EditDataset("Edit a dataset's metadata", Dataset.class),
    ManageDataversePermissions("Manage permissions for a dataverse", Dataverse.class),
    ManageDatasetPermissions("Manage permissions for a dataset", Dataset.class), 
    PublishDataverse("Publish a dataverse", Dataverse.class),
    PublishDataset("Publish a dataset", Dataset.class),     
    // Delete
    DeleteDataverse("Delete an unpublished dataverse", Dataverse.class),    
    DeleteDatasetDraft("Delete a dataset draft", Dataset.class);

    // FUTURE:
    //RestrictMetadata("Mark metadata as restricted", DvObject.class),
    //AccessRestrictedMetadata("Access metadata marked as\"restricted\"", DvObject.class),


    private final String humanName;

    private final Set<Class<? extends DvObject>> appliesTo;

    Permission(String aHumanName, Class<? extends DvObject>... appliesToList) {
        humanName = aHumanName;
        appliesTo = new HashSet<>(Arrays.asList(appliesToList));
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
}
