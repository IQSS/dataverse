package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.sitemap.SiteMapUtil;
import javax.ejb.Stateless;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("admin/sitemap")
public class SiteMap extends AbstractApiBean {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSiteMap() {
        try {
            SiteMapUtil.updateSiteMap();
            return ok("Sitemap updated.");
        } catch (Exception ex) {
            return error(Response.Status.BAD_REQUEST, "Sitemap could not be updated. Exception: " + ex.getLocalizedMessage());
        }
    }

}
