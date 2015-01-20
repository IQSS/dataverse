package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.*;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArrayBuilder;

/**
 * An API to test internal models without the need to deal with UI etc.
 * @author michael
 */
@Stateless
@Path("test")
public class TestApi extends AbstractApiBean {
    
    @Path("assignee/{idtf}")
    @GET
    public Response findRoleAssignee( @PathParam("idtf") String idtf ) {
        RoleAssignee ra = roleAssigneeSvc.getRoleAssignee(idtf);
        return (ra==null) ? notFound("Role Assignee '" + idtf + "' not found.")
                : okResponse(json(ra.getDisplayInfo()));
    }
    
}
