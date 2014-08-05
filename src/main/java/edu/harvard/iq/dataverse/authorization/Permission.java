package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectContainer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * All the permissions in the system are implemented as enum constants in this
 * class. 
 * @author michael
 */
public enum Permission implements java.io.Serializable {
	Access("See and search content", DvObject.class),
    AccessUnpublishedContent("Access unpublished content", DvObject.class),
	AccessRestrictedMetadata("Access metadata marked as\"restricted\"", DvObject.class),
	UndoableEdit("Edits that do not cause data loss", DvObject.class),
	DestructiveEdit("Edits that cannot be reversed, such as deleting data", DvObject.class),
	EditMetadata("Edit the metadata of objects", DvObject.class),
    AddDataverse("Add a dataverse within another dataverse", Dataverse.class),
    AddDataset("Add a dataset to a dataverse", Dataverse.class),
    AddDatasetVersion("Add a version to a dataset", Dataset.class),
	ChooseTemplate("Choose metadata template for dataverses and datasets", DvObjectContainer.class),
	ChooseFacets("Choose facets", Dataverse.class),
	Publish("Release a dataverse or a dataset", Dataverse.class, Dataset.class),
	Style("Customize the appearance of objects", Dataverse.class, Dataset.class),
	GrantPermissions("Manage permissions of other users", Dataset.class, Dataverse.class),
	Tracking("Manage guestbook, download statistics, etc.", Dataverse.class),
    CreateTemplates("Create new templates", DvObjectContainer.class ),
    RestrictFile("Restrict File Access", DataFile.class),
    RestrictMetadata("Mark metadata as restricted", DvObject.class)
	;
	
	private final String humanName;
    
    private final Set<Class<? extends DvObject>> appliesTo;
    
	Permission( String aHumanName, Class<? extends DvObject>... appliesToList  ) {
		humanName = aHumanName;
        appliesTo = new HashSet<>(Arrays.asList(appliesToList));
	}
	
	public String getHumanName() {
		return humanName;
	}
    
    public boolean appliesTo( Class<? extends DvObject> aClass ) {
        for ( Class c : appliesTo ) {
            if ( c.isAssignableFrom(aClass) ) return true;
        }
        return false;
    }
}
