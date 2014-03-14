package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetAuthor;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRole;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.engine.Permission;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import static edu.harvard.iq.dataverse.api.NullSafeJsonBuilder.jsonObjectBuilder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Convert objects to Json.
 * @author michael
 */
public class JsonPrinter {
	
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss X");
	
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
	
	public static JsonObjectBuilder json( Dataset ds ) {
		int versionCount = ds.getVersions().size();
		return jsonObjectBuilder()
				.add( "id", ds.getId() )
				.add( "identifier", ds.getIdentifier() )
				.add( "persistentUrl", ds.getPersistentURL() )
				.add( "protocol", ds.getProtocol() )
				.add( "authority", ds.getAuthority() )
				.add( "versions", jsonObjectBuilder()
									.add("count", versionCount)
									.add("latest", jsonBrief(ds.getLatestVersion()))
									.add("edit", jsonBrief(ds.getEditVersion()))
				);
		
	}
	
	public static JsonObjectBuilder json( DatasetVersion dsv ) {
		JsonObjectBuilder bld = jsonObjectBuilder()
				.add("id", dsv.getId())
				.add("version", dsv.getVersion() )
				.add("versionState", dsv.getVersionState().name() )
				.add("versionNote", dsv.getVersionNote())
				.add("title", dsv.getTitle())
				.add("archiveNote", dsv.getArchiveNote())
				.add("deaccessionLink", dsv.getDeaccessionLink())
				.add("distributionDate", dsv.getDistributionDate())
				.add("distributorNames", dsv.getDistributorNames())
				.add("productionDate", dsv.getProductionDate())
				.add("UNF", dsv.getUNF())
				.add("archiveTime", format(dsv.getArchiveTime()) )
				;
				
		// Add authors
		List<DatasetAuthor> auth = dsv.getDatasetAuthors();
		if ( ! auth.isEmpty() ) {
			if ( auth.size() > 1 ) {
				Collections.sort(auth, new Comparator<DatasetAuthor>(){
					@Override
					public int compare(DatasetAuthor o1, DatasetAuthor o2) {
						return o1.getDisplayOrder()-o2.getDisplayOrder();}});
			}
			JsonArrayBuilder ab = Json.createArrayBuilder();
			for ( DatasetAuthor da : auth ) {
				ab.add( json(da) );
			}
			bld.add("authors", ab);
		}
		
		return bld;
	}
	
	public static JsonObjectBuilder json( DatasetAuthor da ) {
		return jsonObjectBuilder()
				.add( "idType", da.getIdType() )
				.add( "idValue", da.getIdValue() )
				.addStrValue( "name", da.getName() )
				.addStrValue( "affiliation", da.getAffiliation() )
				.add( "displayOrder", da.getDisplayOrder() )
				;
	}
	
	public static JsonObjectBuilder jsonBrief( DatasetVersion dsv ) {
		return ( dsv==null ) 
				? null
				: jsonObjectBuilder().add("id", dsv.getId())
					.add("version", dsv.getVersion() )
					.add("versionState", dsv.getVersionState().name() )
					.add("title", dsv.getTitle());
	}
	
	public static String nullFill( String s ) {
		return s==null ? "" : s;
	}
	
	public static String format( Date d ) {
		return (d==null) ? null : dateFormat.format(d);
	}
}
