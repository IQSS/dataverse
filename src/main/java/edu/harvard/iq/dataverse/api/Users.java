package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
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
		
		for ( BuiltinUser u : builtinUserSvc.findAll() ) {
			bld.add( json(u) );
		}
		
		return okResponse( bld );
	}
	
	@GET
	@Path("{identifier}")
	public Response view( @PathParam("identifier") String identifier ) {
		User u = findUser(identifier);
		
		return ( u!=null ) 
				? okResponse( json(u) ) 
				: errorResponse( Status.NOT_FOUND, "Can't find user with identifier '" + identifier + "'");
	}
	
	@POST
	public Response save( BuiltinUser user, @QueryParam("password") String password ) {
		try { 
			if ( password != null ) {
				user.setEncryptedPassword(builtinUserSvc.encryptPassword(password));
			}
			user = builtinUserSvc.save(user);
            
            AuthenticatedUser au = authSvc.createAuthenticatedUser("builtin", user.getUserName(), user.createDisplayInfo());
            
            // CONT fix this
            ApiToken token = new ApiToken();
            token.setToken(user.getUserName());
            token.setAuthenticatedUser(au);
             
            engineSvc.getContext().em().merge( token );
            
			return okResponse( json(user) );
		} catch ( Exception e ) {
			logger.log( Level.WARNING, "Error saving user", e );
			return errorResponse( Status.INTERNAL_SERVER_ERROR, "Can't save user: " + e.getMessage() );
		}
	}

}
