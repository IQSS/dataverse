package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.MetadataBlockDao;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * Api bean for managing metadata blocks.
 *
 * @author michael
 */
@Path("metadatablocks")
@Produces("application/json")
public class MetadataBlocks extends AbstractApiBean {

    @Inject
    private JsonPrinter jsonPrinter;

    @Inject
    private MetadataBlockDao metadataBlockDao;

    @GET
    public Response list() {
        return allowCors(ok(metadataBlockSvc.listMetadataBlocks().stream()
                .map(jsonPrinter.brief::json)
                .collect(jsonPrinter.toJsonArray())));
    }

    @GET
    @Path("{identifier}")
    public Response getBlock(@PathParam("identifier") String idtf) {
        MetadataBlock b = metadataBlockDao.findByName(idtf);

        return allowCors((b != null)
                ? ok(jsonPrinter.json(b))
                : notFound("Can't find metadata block '" + idtf + "'"));
    }

}
