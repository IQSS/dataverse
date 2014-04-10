package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.MetadataBlock;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.brief;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import javax.ws.rs.PathParam;

/**
 * Api bean for managing metadata blocks.
 * @author michael
 */
@Path("metadatablocks")
@Produces("application/json")
public class MetadataBlocks extends AbstractApiBean {
    
    @GET
    public Response list()  {
        JsonArrayBuilder bld = Json.createArrayBuilder();
        for ( MetadataBlock block : metadataBlockSvc.listMetadataBlocks() )  {
            bld.add( brief.json(block) );
        }
        
        return okResponse(bld);
    }
    
    @Path("{identifier}")
    @GET
    public Response getBlock( @PathParam("identifier") String idtf ) {
        MetadataBlock b = findMetadataBlock(idtf);
        
        return  (b != null ) ? okResponse(json(b)) : notFound("Can't find metadata block '" + idtf + "'");
    }
    
}
