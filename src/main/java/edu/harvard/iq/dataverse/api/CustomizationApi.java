package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

@Path("customization")
public class CustomizationApi extends AbstractApiBean {

    @EJB
    SettingsServiceBean settingsService;

    @GET
    @Path("analytics")
    @Produces({"text/html; charset=UTF-8"})
    public Response getAnalyticsHTML() {
        return getFromSettings(SettingsServiceBean.Key.WebAnalyticsCode, "text/html; charset=UTF-8");
    }

    private Response getFromSettings(SettingsServiceBean.Key key, String type) {
        String value = settingsService.getValueForKey(key);
        if (value != null) {
            return Response.ok(value).type(type).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
