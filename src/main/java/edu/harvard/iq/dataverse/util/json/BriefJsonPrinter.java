package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.MetadataBlock;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import edu.harvard.iq.dataverse.workflow.Workflow;
import jakarta.json.JsonObjectBuilder;

/**
 * A Json printer that prints minimal data on objects. Useful when embedding 
 * objects inside others.
 * 
 * @author michael
 */
public class BriefJsonPrinter {
	
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
    
    public JsonObjectBuilder json( Workflow wf ) {
        return jsonObjectBuilder().add("id", wf.getId())
                                  .add("name", wf.getName() );
    }
}
