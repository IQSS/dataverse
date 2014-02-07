package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRole;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.engine.Permission;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

/**
 * Convert objects to Json.
 * @author michael
 */
public class JsonPrinter {
	
	public static JsonObjectBuilder json( RoleAssignment ra ) {
		return Json.createObjectBuilder()
				.add("id", ra.getId())
				.add("userId", ra.getUser().getId() )
				.add("_username", nullFill(ra.getUser().getUserName()))
				.add("roleId", ra.getRole().getId() )
				.add("_roleAlias", nullFill(ra.getRole().getAlias()))
				.add("definitionPointId", ra.getDefinitionPoint().getId() );
	}
	
	public static JsonArrayBuilder json( Set<Permission> permissions ) {
		JsonArrayBuilder bld = Json.createArrayBuilder();
		for ( Permission p : permissions ) {
			bld.add( p.name() );
		}
		return bld;
	}
	
	public static JsonObjectBuilder json( DataverseRole role ) {
		JsonObjectBuilder bld = Json.createObjectBuilder()
				.add("alias", nullFill(role.getAlias()) )
				.add("name", nullFill(role.getName()))
				.add("permissions", json(role.permissions()))
				.add("description", nullFill(role.getDescription()));
		if ( role.getId() != null ) bld.add("id", role.getId() );
		if ( role.getOwner()!=null && role.getOwner().getId()!=null ) bld.add("ownerId", role.getOwner().getId());
		
		return bld;
	}
	
	public static JsonObjectBuilder json( Dataverse dv ) {
		JsonObjectBuilder bld = Json.createObjectBuilder()
						.add("id", dv.getId() )
						.add("alias", nullFill(dv.getAlias()) )
						.add("name", nullFill(dv.getName()))
						.add("affiliation", dv.getAffiliation())
						.add("contactEmail", dv.getContactEmail())
						.add("permissionRoot", dv.isPermissionRoot())
						.add("description", nullFill(dv.getDescription()));
		if ( dv.getOwner() != null ) {
			bld.add("ownerId", dv.getOwner().getId());
		}
		return bld;
	}
	
	public static JsonObjectBuilder json( DataverseUser user ) {
		return Json.createObjectBuilder()
				.add( "id", user.getId() )
				.add( "firstName", nullFill(user.getFirstName()))
				.add( "lastName",  nullFill(user.getLastName()))
				.add( "userName",  nullFill(user.getUserName()))
				.add( "affiliation", nullFill(user.getAffiliation()))
				.add( "position",  nullFill(user.getPosition()))
				.add( "email",     nullFill(user.getEmail()))
				.add( "phone",     nullFill(user.getPhone()));
	}
	
	public static String nullFill( String s ) {
		return s==null ? "" : s;
	}
}
