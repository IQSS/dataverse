package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.User;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Permission test bean.
 * @author michael
 */
@Path("permissions")
public class Permissions extends AbstractApiBean {
	@EJB
	PermissionServiceBean permissions;
	
	@GET
	public Response listPermissions( @QueryParam("user") String userIdtf, @QueryParam("on") String dvoIdtf ) {
		User u = findUserById(userIdtf);
		if ( u==null ) return errorResponse( Status.FORBIDDEN, "Can't find user with identifier '" + userIdtf + "'");
		
		Dataverse d = findDataverse(dvoIdtf);
		if ( d==null ) notFound("Can't find dataverser with identifier '" + dvoIdtf );
		
		return okResponse( json(permissions.on(d).user(u).get()) ) ;
	}
}
