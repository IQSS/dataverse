package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.util.BundleUtil;
import javax.ejb.Stateless;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * PIDs are Persistent IDentifiers such as DOIs or Handles.
 *
 * Currently PIDs can be minted at the dataset and file level but there is
 * demand for PIDs at the dataverse level too. That's why this dedicated "pids"
 * endpoint exists.
 */
@Stateless
@Path("pids")
public class Pids extends AbstractApiBean {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPid(@QueryParam("persistentId") String persistentId) {
        try {
            User user = findUserOrDie();
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, BundleUtil.getStringFromBundle("admin.api.auth.mustBeSuperUser"));
            }
            String baseUrl = System.getProperty("doi.baseurlstringnext") + "/dois/";
            String username = System.getProperty("doi.username");
            String password = System.getProperty("doi.password");
            JsonObjectBuilder result = PidUtil.queryDoi(persistentId, baseUrl, username, password);
            return ok(result);
        } catch (WrappedResponse ex) {
            return error(Response.Status.BAD_REQUEST, ex.getLocalizedMessage());
        }
    }

}
