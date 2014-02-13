package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import static edu.harvard.iq.dataverse.api.JsonPrinter.json;
import edu.harvard.iq.dataverse.engine.Permission;
import java.util.Set;
import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

/**
 * Permission test bean.
 * @author michael
 */
@Path("permissions")
public class Permissions extends AbstractApiBean {
	
	@EJB
	PermissionServiceBean permissions;
	
	@GET
	public String listPermissions( @QueryParam("user") String userIdtf, @QueryParam("on") String dvoIdtf ) {
		DataverseUser u = findUser(userIdtf);
		if ( u==null ) return error("Can't find user with identifier '" + userIdtf + "'");
		
		Dataverse d = findDataverse(dvoIdtf);
		if ( d==null ) error( "Can't find dataverser with identifier '" + dvoIdtf );
		
		Set<Permission> granted = permissions.on(d).user(u).get();
		return ok( json(granted) ) ;
	}
}
