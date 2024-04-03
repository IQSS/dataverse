package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.MetadataBlock;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.brief;
import jakarta.ws.rs.PathParam;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.toJsonArray;

/**
 * Api bean for managing metadata blocks.
 * @author michael
 */
@Path("metadatablocks")
@Produces("application/json")
public class MetadataBlocks extends AbstractApiBean {
    
    @GET
    public Response list()  {
        return ok(metadataBlockSvc.listMetadataBlocks().stream().map(brief::json).collect(toJsonArray()));
    }
    
    @Path("{identifier}")
    @GET
    public Response getBlock( @PathParam("identifier") String idtf ) {
        MetadataBlock b = findMetadataBlock(idtf);
        
        return   (b != null ) ? ok(json(b)) : notFound("Can't find metadata block '" + idtf + "'");
    }
    
}
