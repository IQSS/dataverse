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
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.InputStream;
import java.text.MessageFormat;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;

@Stateless
@Path("dataverseFeaturedItems")
public class DataverseFeaturedItems extends AbstractApiBean {

    @Inject
    DataverseFeaturedItemServiceBean dataverseFeaturedItemServiceBean;

    @DELETE
    @AuthRequired
    @Path("{id}")
    public Response deleteFeaturedItem(@Context ContainerRequestContext crc, @PathParam("id") Long id) {
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
    public Response updateFeaturedItem(@Context ContainerRequestContext crc,
                                       @PathParam("id") Long id,
                                       @FormDataParam("content") String content,
                                       @FormDataParam("type") String type,
                                       @FormDataParam("dvObject") String dvObjectIdtf,
                                       @FormDataParam("displayOrder") int displayOrder,
                                       @FormDataParam("keepFile") boolean keepFile,
                                       @FormDataParam("file") InputStream imageFileInputStream,
                                       @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {
        try {
            DataverseFeaturedItem dataverseFeaturedItem = dataverseFeaturedItemServiceBean.findById(id);
            if (dataverseFeaturedItem == null) {
                throw new WrappedResponse(error(Response.Status.NOT_FOUND, MessageFormat.format(BundleUtil.getStringFromBundle("dataverseFeaturedItems.errors.notFound"), id)));
            }
            DvObject dvObject = (dvObjectIdtf != null) ? findDvoByIdAndTypeOrDie(dvObjectIdtf, type) : null;
            UpdatedDataverseFeaturedItemDTO updatedDataverseFeaturedItemDTO = UpdatedDataverseFeaturedItemDTO.fromFormData(content, displayOrder, keepFile, imageFileInputStream, contentDispositionHeader, type, dvObject);
            return ok(json(execCommand(new UpdateDataverseFeaturedItemCommand(createDataverseRequest(getRequestUser(crc)), dataverseFeaturedItem, updatedDataverseFeaturedItemDTO))));
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }
}
