package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.MetadataBlockDao;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.dataverse.MetadataBlockTsvCreator;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.util.stream.Collectors;

/**
 * Api bean for managing metadata blocks.
 *
 * @author michael
 */
@Path("metadatablocks")
public class MetadataBlocks extends AbstractApiBean {

    @Inject
    private MetadataBlockDao metadataBlockDao;

    // -------------------- LOGIC --------------------

    @GET
    @Produces("application/json")
    public Response list() {
        MetadataBlockDTO.Converter converter = new MetadataBlockDTO.Converter();
        return allowCors(ok(metadataBlockSvc.listMetadataBlocks().stream()
                .map(converter::convertMinimal)
                .collect(Collectors.toList())));
    }

    @GET
    @Path("{identifier}")
    @Produces("application/json")
    public Response getBlock(@PathParam("identifier") String identifier) {
        MetadataBlock b = metadataBlockDao.findByName(identifier);

        return allowCors(b != null
                ? ok(new MetadataBlockDTO.Converter().convert(b))
                : notFound(String.format("Can't find metadata block '%s'", identifier)));
    }

    @Path("/tsv/{identifier}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response createBlockTsv(@PathParam("identifier") String identifier) {
        MetadataBlock block = metadataBlockDao.findByName(identifier);
        if (block == null) {
            return notFound(String.format("Can't find metadata block '%s'", identifier));
        }

        StreamingOutput tsvStreamer = output -> new MetadataBlockTsvCreator().createTsv(block, output);
        String fileName = identifier + ".tsv";

        return Response.ok(tsvStreamer, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=" + fileName)
                .build();
    }
}
