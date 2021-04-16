package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.sitemap.SiteMapServiceBean;
import edu.harvard.iq.dataverse.sitemap.SiteMapUtil;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
