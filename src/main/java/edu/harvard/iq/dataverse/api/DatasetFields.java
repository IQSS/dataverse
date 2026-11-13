package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.jsonDatasetFieldTypes;

/**
 * Api bean for managing dataset fields.
 */
@Path("datasetfields")
@Produces("application/json")
@Tag(name = "Dataset Fields", description = "Dataset field type, controlled vocabulary, and metadata block loading operations.")
public class DatasetFields extends AbstractApiBean {

    @EJB
    DatasetFieldServiceBean datasetFieldService;

    @GET
    @Path("facetables")
    @Operation(summary = "Lists facetable dataset fields",
            description = "Lists all facetable dataset fields defined in the installation.")
    @APIResponse(responseCode = "200", description = "Facetable dataset fields.",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(type = SchemaType.OBJECT)))
    public Response listAllFacetableDatasetFields() {
        List<DatasetFieldType> datasetFieldTypes = datasetFieldService.findAllFacetableFieldTypes();
        return ok(jsonDatasetFieldTypes(datasetFieldTypes));
    }
}
