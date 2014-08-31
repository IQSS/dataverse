package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.MetadataBlock;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import javax.json.JsonObjectBuilder;

/**
 * A Json printer that prints minimal data on objects. Useful when embedding 
 * objects inside others.
 * 
 * @author michael
 */
public class BriefJsonPrinter {
	
	public JsonObjectBuilder json( BuiltinUser usr ) {
		return ( usr==null ) 
				? null
				: jsonObjectBuilder().add("id", usr.getId())
					.add("firstName", usr.getFirstName())
					.add("lastName", usr.getLastName())
					.add("affiliation", usr.getAffiliation())
					;
	}
	
	public JsonObjectBuilder json( DatasetVersion dsv ) {
		return ( dsv==null ) 
				? null
				: jsonObjectBuilder().add("id", dsv.getId())
					.add("version", dsv.getVersion() )
					.add("versionState", dsv.getVersionState().name() )
					.add("title", dsv.getTitle());
	}
    
    public JsonObjectBuilder json( MetadataBlock blk ) {
		return ( blk==null ) 
				? null
				: jsonObjectBuilder().add("id", blk.getId())
					.add("displayName", blk.getDisplayName())
					.add("name", blk.getName())
					;
	}
}
