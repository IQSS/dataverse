package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.MetadataBlock;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;

/**
 * Api bean for managing metadata blocks.
 *
 * @author michael
 */
@Path("metadatablocks")
@Produces("application/json")
@Tag(name = "Metadata", description = "Metadata block and metadata export operations.")
public class MetadataBlocks extends AbstractApiBean {

    @GET
    @Operation(summary = "Lists metadata blocks",
            description = "Returns metadata blocks as JSON, optionally limited to blocks displayed during dataset creation and optionally including dataset field types.")
    public Response listMetadataBlocks(
            @Parameter(description = "Limit results to metadata blocks displayed during dataset creation.")
            @QueryParam("onlyDisplayedOnCreate") boolean onlyDisplayedOnCreate,
            @Parameter(description = "Include dataset field type definitions in each metadata block.")
            @QueryParam("returnDatasetFieldTypes") boolean returnDatasetFieldTypes) {
        List<MetadataBlock> metadataBlocks = metadataBlockSvc.listMetadataBlocks(onlyDisplayedOnCreate);
        return ok(json(metadataBlocks, returnDatasetFieldTypes, onlyDisplayedOnCreate));
    }

    @Path("{identifier}")
    @GET
    @Operation(summary = "Returns a metadata block",
            description = "Returns the metadata block identified by id, name, or display name.")
    public Response getMetadataBlock(
            @Parameter(description = "Metadata block id, name, or display name.", required = true)
            @PathParam("identifier") String idtf) {
        MetadataBlock b = findMetadataBlock(idtf);
        return (b != null) ? ok(json(b)) : notFound("Can't find metadata block '" + idtf + "'");
    }
}
