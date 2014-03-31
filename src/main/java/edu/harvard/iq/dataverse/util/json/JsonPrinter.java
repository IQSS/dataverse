package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetAuthor;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRole;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.engine.Permission;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

/**
 * Convert objects to Json.
 * @author michael
 */
public class JsonPrinter {
	
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss X");
	
	public static final BriefJsonPrinter brief = new BriefJsonPrinter();
	
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
		JsonObjectBuilder bld = jsonObjectBuilder()
						.add("id", dv.getId() )
						.add("alias", nullFill(dv.getAlias()) )
						.add("name", nullFill(dv.getName()))
						.add("affiliation", dv.getAffiliation())
						.add("contactEmail", dv.getContactEmail())
						.add("permissionRoot", dv.isPermissionRoot())
						.add("creator",json(dv.getCreator()))
						.add("description", dv.getDescription());
		if ( dv.getOwner() != null ) {
			bld.add("ownerId", dv.getOwner().getId());
		}
		if ( dv.getCreateDate() != null ) {
			bld.add("creationDate", dateFormat.format(dv.getCreateDate()));
		}
		
		return bld;
	}
	
	public static JsonObjectBuilder json( DataverseUser user ) {
		return (user == null ) 
				? null 
				: jsonObjectBuilder()
					.add( "id", user.getId() )
					.add( "firstName", user.getFirstName())
					.add( "lastName",  user.getLastName())
					.add( "userName",  user.getUserName())
					.add( "affiliation", user.getAffiliation())
					.add( "position",  user.getPosition())
					.add( "email",     user.getEmail())
					.add( "phone",     user.getPhone());
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
									.add("latest", brief.json(ds.getLatestVersion()))
									.add("edit", brief.json(ds.getEditVersion()))
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
		
		// Arrange the dataset field values in metadata blocks.
		JsonObjectBuilder blocksBld = jsonObjectBuilder();
		List<MetadataBlock> metadataBlocks = dsv.getDataset().getOwner().getMetadataBlocks();
		List<DatasetField> fieldValues = dsv.getDatasetFields();
		
		for ( MetadataBlock block : metadataBlocks ) {
			JsonObjectBuilder blockBld = jsonObjectBuilder();
		
			blockBld.add("block", json(block) );
			
			Set<DatasetFieldType> blockFields = new TreeSet<>(block.getDatasetFieldTypes());
			
			JsonObjectBuilder valuesBld = jsonObjectBuilder();

			for ( DatasetField val : new TreeSet<>(fieldValues) ) {
				if ( blockFields.contains(val.getDatasetFieldType()) ) {
					valuesBld.add( val.getDatasetFieldType().getName(), json(val) );
				}
			}
			
			blockBld.add( "values", valuesBld );
			
			blocksBld.add(block.getName(), blockBld);
		}
		
		bld.add("metadataBlocks", blocksBld);

		
		return bld;
	}
	
	public static JsonObjectBuilder json( DatasetField dfv ) {
		JsonObjectBuilder bld = jsonObjectBuilder();
		bld.add( "id", dfv.getId() );
                /*
		bld.add( "displayOrder", dfv.getDisplayOrder() );
		if ( dfv.isEmpty() ) {
			bld.addNull("value");
		} else {
			if ( dfv.isChildEmpty() ) {
				bld.add( "value", dfv.getValue());
			} else {
				JsonObjectBuilder childBld = jsonObjectBuilder();
				for ( DatasetFieldValue childVal : dfv.getChildDatasetFieldValues() ) {
					childBld.add(childVal.getDatasetField().getName(), json(childVal) );
				}
				bld.add( "value", childBld );
			}
		}*/
		
		return bld;
	}
	
	public static JsonObjectBuilder json( MetadataBlock blk ) {
		JsonObjectBuilder bld = jsonObjectBuilder();
		bld.add("id", blk.getId());
		bld.add("name", blk.getName());
		bld.add("displayName", blk.getDisplayName());
		
		JsonObjectBuilder fieldsBld = jsonObjectBuilder();
		for ( DatasetFieldType df : new TreeSet<>(blk.getDatasetFieldTypes()) ) {
			fieldsBld.add( df.getName(), json(df) );
		}
		
		bld.add("fields", fieldsBld );
		
		return bld;
	}
	
	public static JsonObjectBuilder json( DatasetFieldType fld ) {
		JsonObjectBuilder fieldsBld = jsonObjectBuilder();
		fieldsBld.add( "name", fld.getName() );
		fieldsBld.add( "displayName", fld.getDisplayName());
		fieldsBld.add( "title", fld.getTitle());
		fieldsBld.add( "type", fld.getFieldType());
		fieldsBld.add( "watermark", fld.getWatermark());
		fieldsBld.add( "description", fld.getDescription());
		if ( ! fld.getChildDatasetFieldTypes().isEmpty() ) {
			JsonObjectBuilder subFieldsBld = jsonObjectBuilder();
			for ( DatasetFieldType subFld : fld.getChildDatasetFieldTypes() ) {
				subFieldsBld.add( subFld.getName(), json(subFld) );
			}
			fieldsBld.add("childFields", subFieldsBld);
		}
		
		return fieldsBld;
	}
	
	public static JsonObjectBuilder json( FileMetadata fmd ) {
		return jsonObjectBuilder()
				.add("category", fmd.getCategory())
				.add("description", fmd.getDescription())
				.add("label", fmd.getLabel())
				.add("version", fmd.getVersion())
				.add("datasetVersionId", fmd.getDatasetVersion().getId())
				.add("datafile", json(fmd.getDataFile()))
				;
	}
	
	public static JsonObjectBuilder json( DataFile df ) {
		return jsonObjectBuilder()
				.add("id", df.getId() )
				.add("name", df.getName())
				.add("contentType", df.getContentType())
				.add("filename", df.getFilename())
				.add("originalFileFormat", df.getOriginalFileFormat())
				.add("originalFormatLabel", df.getOriginalFormatLabel())
				.add("UNF", df.getUnf())
				.add("md5", df.getmd5())
				.add("description", df.getDescription())
				;
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
	
	
	public static String nullFill( String s ) {
		return s==null ? "" : s;
	}
	
	public static String format( Date d ) {
		return (d==null) ? null : dateFormat.format(d);
	}
}
