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
    @Path("{itemId}")
    public Response deleteItem(@Context ContainerRequestContext crc, @PathParam("itemId") Long itemId) {
        try {
            DataverseFeaturedItem dataverseFeaturedItem = dataverseFeaturedItemServiceBean.findById(itemId);
            if (dataverseFeaturedItem == null) {
                throw new WrappedResponse(error(Response.Status.NOT_FOUND, MessageFormat.format(BundleUtil.getStringFromBundle("dataverseFeaturedItems.errors.notFound"), itemId)));
            }
            execCommand(new DeleteDataverseFeaturedItemCommand(createDataverseRequest(getRequestUser(crc)), dataverseFeaturedItem));
            return ok(MessageFormat.format(BundleUtil.getStringFromBundle("dataverseFeaturedItems.delete.successful"), itemId));
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }
}
