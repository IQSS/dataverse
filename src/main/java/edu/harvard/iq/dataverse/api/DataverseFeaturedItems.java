package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.engine.command.impl.*;
import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import java.text.MessageFormat;

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
}
