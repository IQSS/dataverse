package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.MetadataBlock;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;

/**
 * Api bean for managing metadata blocks.
 *
 * @author michael
 */
@Path("metadatablocks")
@Produces("application/json")
public class MetadataBlocks extends AbstractApiBean {

    @GET
    public Response listMetadataBlocks(@QueryParam("onlyDisplayedOnCreate") boolean onlyDisplayedOnCreate,
                                       @QueryParam("returnDatasetFieldTypes") boolean returnDatasetFieldTypes) {
        List<MetadataBlock> metadataBlocks = metadataBlockSvc.listMetadataBlocks(onlyDisplayedOnCreate);
        return ok(json(metadataBlocks, returnDatasetFieldTypes, onlyDisplayedOnCreate));
    }

    @Path("{identifier}")
    @GET
    public Response getMetadataBlock(@PathParam("identifier") String idtf) {
        MetadataBlock b = findMetadataBlock(idtf);
        return (b != null) ? ok(json(b)) : notFound("Can't find metadata block '" + idtf + "'");
    }
}
