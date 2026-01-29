package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.jsonDatasetFieldTypes;

/**
 * Api bean for managing dataset fields.
 */
@Path("datasetfields")
@Produces("application/json")
public class DatasetFields extends AbstractApiBean {

    @EJB
    DatasetFieldServiceBean datasetFieldService;

    @GET
    @Path("facetables")
    public Response listAllFacetableDatasetFields() {
        List<DatasetFieldType> datasetFieldTypes = datasetFieldService.findAllFacetableFieldTypes();
        return ok(jsonDatasetFieldTypes(datasetFieldTypes));
    }
}
