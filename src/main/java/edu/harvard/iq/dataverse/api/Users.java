package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
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
            
            ApiToken token = new ApiToken();
            
            token.setTokenString(java.util.UUID.randomUUID().toString());
            token.setAuthenticatedUser(au);

            Calendar c = Calendar.getInstance();
            token.setCreateTime( new Timestamp(c.getTimeInMillis()) );
            c.roll(Calendar.YEAR, 1);
            token.setExpireTime( new Timestamp(c.getTimeInMillis()) );
            authSvc.save(token);
            
            JsonObjectBuilder resp = Json.createObjectBuilder();
            resp.add("user", json(user) );
            resp.add("apiToken", token.getTokenString());
			return okResponse( resp );
            
		} catch ( Exception e ) {
			logger.log( Level.WARNING, "Error saving user", e );
			return errorResponse( Status.INTERNAL_SERVER_ERROR, "Can't save user: " + e.getMessage() );
		}
	}

}
