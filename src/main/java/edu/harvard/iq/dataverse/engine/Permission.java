package edu.harvard.iq.dataverse.engine;

/**
 * All the permissions in the system are implemented as enum constants in this
 * class. 
 * @author michael
 */
public enum Permission implements java.io.Serializable {
	DatasetCreate( "Create Dataset" ),
	DatasetList( "List all Datasets in a Dataverse" ),
	DatasetView( "View Datasets" ),
	DatasetEdit( "Edit a Dataset" ),
	DatasetDelete( "Delete a Dataset" ),
	DataverseEdit( "Edit a Dataverse" ),
	DataverseCreate( "Create a Dataverse" ),
	DataverseDelete( "Delete a Dataverse" )
	;
	
	private final String humanName;
	Permission( String aHumanName ) {
		humanName = aHumanName;
	}
	
	public String getHumanName() {
		return humanName;
	}
}
