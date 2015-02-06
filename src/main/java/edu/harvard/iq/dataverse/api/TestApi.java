package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.*;
import javax.ws.rs.QueryParam;

/**
 * An API to test internal models without the need to deal with UI etc.
 * @author michael
 */
@Stateless
@Path("test")
public class TestApi extends AbstractApiBean {
    
    @Path("permissions/{dvo}")
    @GET
    public Response findPermissonsOn( @PathParam("dvo") String dvo,
                                      @QueryParam("key") String key ) {
        DvObject dvObj = findDvo(dvo);
        if ( dvObj == null ) {
            return notFound("DvObject " + dvo + " not found");
        }
        try {
            AuthenticatedUser au = findUserOrDie(key);
            return okResponse( json(permissionSvc.permissionsFor(au, dvObj)) );
            
        } catch ( WrappedResponse wr ) {
            return wr.getResponse();
        }
    }
    
    @Path("assignee/{idtf}")
    @GET
    public Response findRoleAssignee( @PathParam("idtf") String idtf ) {
        RoleAssignee ra = roleAssigneeSvc.getRoleAssignee(idtf);
        return (ra==null) ? notFound("Role Assignee '" + idtf + "' not found.")
                : okResponse(json(ra.getDisplayInfo()));
    }
    
}
