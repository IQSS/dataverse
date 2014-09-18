package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataverseUser;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 *
 * @author michael
 */
@Path("users")
public class Users extends AbstractApiBean {
	private static final Logger logger = Logger.getLogger(Users.class.getName());
	
	@GET
	public Response list() {
		JsonArrayBuilder bld = Json.createArrayBuilder();
		
		for ( DataverseUser u : userSvc.findAll() ) {
			bld.add( json(u) );
		}
		
		return okResponse( bld );
	}
	
	@GET
	@Path("{identifier}")
	public Response view( @PathParam("identifier") String identifier ) {
		DataverseUser u = findUser(identifier);
		
		return ( u!=null ) 
				? okResponse( json(u) ) 
				: errorResponse( Status.NOT_FOUND, "Can't find user with identifier '" + identifier + "'");
	}
	
	@POST
	public Response save( DataverseUser user, @QueryParam("password") String password ) {
		try { 
			if ( password != null ) {
				user.setEncryptedPassword(userSvc.encryptPassword(password));
			}
			user = userSvc.save(user);
			return okResponse( json(user) );
		} catch ( Exception e ) {
			logger.log( Level.WARNING, "Error saving user", e );
			return errorResponse( Status.INTERNAL_SERVER_ERROR, "Can't save user: " + e.getMessage() );
		}
	}
	
	@GET
	@Path(":guest")
	public Response genarateGuest() {
		return okResponse( json(userSvc.createGuestUser()) );
		
	}
}
