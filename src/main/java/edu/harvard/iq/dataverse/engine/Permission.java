package edu.harvard.iq.dataverse.engine;

/**
 * All the permissions in the system are implemented as enum constants in this
 * class. 
 * @author michael
 */
public enum Permission implements java.io.Serializable {
	Access("See and search content"),
	AccessRestrictedMetadata("Access metadata marked as\"restricted\""),
	UndoableEdit("Edits that do not cause data loss"),
	DestructiveEdit("Edits that cannot be reversed, such as deleting data"),
	EditMetadata("Edit the metadata of objects"),
    AddDataverse("Add a dataverse within another dataverse"),
    AddDataset("Add a dataset to a dataverse"),
    AddDatasetVersion("Add a version to a dataset"),
	ChooseTemplate("Choose metadata template for dataverses and datasets"),
	Release("Release a dataverse or a dataset"),
	Style("Customize the appearance of objects"),
	GrantPermissions("Manage permissions of other users"),
	Tracking("Manage guestbook, download statistics, etc.")
	;
	
	private final String humanName;
	Permission( String aHumanName ) {
		humanName = aHumanName;
	}
	
	public String getHumanName() {
		return humanName;
	}
}
