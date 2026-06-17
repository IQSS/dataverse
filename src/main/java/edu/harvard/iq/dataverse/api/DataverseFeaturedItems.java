package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.api.dto.UpdatedDataverseFeaturedItemDTO;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItemServiceBean;
import edu.harvard.iq.dataverse.engine.command.impl.*;
import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.InputStream;
import java.text.MessageFormat;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;

@Stateless
@Path("dataverseFeaturedItems")
@Tag(name = "Dataverse Featured Items", description = "Manage featured items displayed on dataverse pages.")
@SecurityRequirement(name = "DataverseApiKey")
public class DataverseFeaturedItems extends AbstractApiBean {

    @Inject
    DataverseFeaturedItemServiceBean dataverseFeaturedItemServiceBean;

    @DELETE
    @AuthRequired
    @Path("{id}")
    @Operation(summary = "Remove a dataverse featured item",
            description = "Deletes the featured item with the supplied database id.")
    public Response deleteFeaturedItem(@Context ContainerRequestContext crc,
                                       @Parameter(description = "Database id of the featured item to delete.", required = true)
                                       @PathParam("id") Long id) {
        try {
            DataverseFeaturedItem dataverseFeaturedItem = dataverseFeaturedItemServiceBean.findById(id);
            if (dataverseFeaturedItem == null) {
                throw new WrappedResponse(error(Response.Status.NOT_FOUND, MessageFormat.format(BundleUtil.getStringFromBundle("dataverseFeaturedItems.errors.notFound"), id)));
            }
            execCommand(new DeleteDataverseFeaturedItemCommand(createDataverseRequest(getRequestUser(crc)), dataverseFeaturedItem));
            return ok(MessageFormat.format(BundleUtil.getStringFromBundle("dataverseFeaturedItems.delete.successful"), id));
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }

    @PUT
    @AuthRequired
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("{id}")
    @Operation(summary = "Revise a dataverse featured item",
            description = "Updates text, linked object, display order, and optional image content for a featured item.")
    @RequestBody(description = "Multipart featured item update with text, ordering, optional linked object, and optional replacement image.")
    public Response updateFeaturedItem(@Context ContainerRequestContext crc,
                                       @Parameter(description = "Database id of the featured item to update.", required = true)
                                       @PathParam("id") Long id,
                                       @Parameter(description = "Featured item text or link content.")
                                       @FormDataParam("content") String content,
                                       @Parameter(description = "Featured item type used to resolve an optional linked dataverse object.")
                                       @FormDataParam("type") String type,
                                       @Parameter(description = "Identifier of the optional dataverse object linked by this featured item.")
                                       @FormDataParam("dvObjectIdentifier") String dvObjectIdtf,
                                       @Parameter(description = "Display order for the featured item.")
                                       @FormDataParam("displayOrder") int displayOrder,
                                       @Parameter(description = "Whether to keep the existing featured item image.")
                                       @FormDataParam("keepFile") boolean keepFile,
                                       @Parameter(description = "Replacement featured item image file.")
                                       @FormDataParam("file") InputStream imageFileInputStream,
                                       @Parameter(description = "Uploaded image file metadata.")
                                       @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {
        try {
            DataverseFeaturedItem dataverseFeaturedItem = dataverseFeaturedItemServiceBean.findById(id);
            if (dataverseFeaturedItem == null) {
                throw new WrappedResponse(error(Response.Status.NOT_FOUND, MessageFormat.format(BundleUtil.getStringFromBundle("dataverseFeaturedItems.errors.notFound"), id)));
            }
            DvObject dvObject = (dvObjectIdtf != null) ? findDvoByIdAndTypeOrDie(dvObjectIdtf, type, true) : null;
            UpdatedDataverseFeaturedItemDTO updatedDataverseFeaturedItemDTO = UpdatedDataverseFeaturedItemDTO.fromFormData(content, displayOrder, keepFile, imageFileInputStream, contentDispositionHeader, type, dvObject);
            return ok(json(execCommand(new UpdateDataverseFeaturedItemCommand(createDataverseRequest(getRequestUser(crc)), dataverseFeaturedItem, updatedDataverseFeaturedItemDTO))));
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }
}
