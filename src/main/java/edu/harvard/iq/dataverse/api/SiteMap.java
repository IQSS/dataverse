package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.sitemap.SiteMapServiceBean;
import edu.harvard.iq.dataverse.sitemap.SiteMapUtil;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Stateless
@Path("admin/sitemap")
public class SiteMap extends AbstractApiBean {

    @EJB
    SiteMapServiceBean siteMapSvc;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSiteMap() {
        boolean stageFileExists = SiteMapUtil.stageFileExists();
        if (stageFileExists) {
            return error(Response.Status.BAD_REQUEST, "Sitemap cannot be updated because staged file exists.");
        }
        siteMapSvc.updateSiteMap(dataverseSvc.findAll(), datasetSvc.findAll());
        return ok("Sitemap update has begun. Check logs for status.");
    }

}
