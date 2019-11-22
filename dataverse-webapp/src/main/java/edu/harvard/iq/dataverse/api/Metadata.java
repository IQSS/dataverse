/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.harvest.server.OAISetServiceBean;
import edu.harvard.iq.dataverse.persistence.harvest.OAISet;

import javax.ejb.EJB;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * @author Leonid Andreev
 */

@Path("admin/metadata")
public class Metadata extends AbstractApiBean {
    private static final Logger logger = Logger.getLogger(Metadata.class.getName());

    @EJB
    OAISetServiceBean oaiSetService;

    @EJB
    DatasetDao datasetDao;

    /**
     * initial attempt at triggering indexing/creation/population of a OAI set without going throught
     * the UI.
     */
    @PUT
    @Path("/exportOAI/{specname}")
    public Response exportOaiSet(@PathParam("specname") String spec) {
        // assuming this belongs here (because it's a metadata export), but open to moving it elsewhere
        OAISet set = null;
        try {
            set = oaiSetService.findBySpec(spec);
        } catch (Exception ex) {
            return error(Response.Status.BAD_REQUEST, "bad request / invalid OAI set");
        }
        if (null == set) {
            return error(Response.Status.NOT_FOUND, "unable to find specified OAI set");
        }
        try {
            oaiSetService.setUpdateInProgress(set.getId());
            oaiSetService.exportOaiSetAsync(set);
            return ok("export started");
        } catch (Exception ex) {
            return error(Response.Status.BAD_REQUEST, "problem exporting OAI set");
        }
    }
}
