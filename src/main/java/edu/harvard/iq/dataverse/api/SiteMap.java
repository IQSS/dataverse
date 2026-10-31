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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Stateless
@Path("admin/sitemap")
@Tag(name = "Admin", description = "Administrative Dataverse operations.")
public class SiteMap extends AbstractApiBean {

    @EJB
    SiteMapServiceBean siteMapSvc;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Starts a sitemap update",
            description = "Starts regeneration of the site map for all dataverses and datasets when no staged sitemap file is present.")
    @APIResponse(responseCode = "200", description = "Sitemap update started.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = SchemaType.OBJECT)))
    public Response updateSiteMap() {
        boolean stageFileExists = SiteMapUtil.stageFileExists();
        if (stageFileExists) {
            return error(Response.Status.BAD_REQUEST, "Sitemap cannot be updated because staged file exists.");
        }
        siteMapSvc.updateSiteMap(dataverseSvc.findAll(), datasetSvc.findAll());
        return ok("Sitemap update has begun. Check logs for status.");
    }

}
