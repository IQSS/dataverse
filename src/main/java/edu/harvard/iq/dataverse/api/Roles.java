package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRole;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.DataverseUserServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.RoleAssignment;
import static edu.harvard.iq.dataverse.api.Util.error;
import static edu.harvard.iq.dataverse.api.Util.ok;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import static edu.harvard.iq.dataverse.api.JsonPrinter.*;
import javax.ws.rs.QueryParam;

/**
 * Util API for managing roles. Might not make it to the production version.
 * @author michael
 */
@Path("roles")
public class Roles {
	
	private static final Logger logger = Logger.getLogger(Roles.class.getName());
	
	@EJB
	DataverseRoleServiceBean rolesSvc;
	
	@EJB
	DataverseUserServiceBean usersSvc;
	
	@EJB
	DataverseServiceBean dvSvc;
	
	@EJB
	EjbDataverseEngine engineSvc;
	
	@GET
	public String list() {
		JsonArrayBuilder rolesArrayBuilder = Json.createArrayBuilder();
		for ( DataverseRole role : rolesSvc.findAll() ) {
            JsonObjectBuilder roleJson = Json.createObjectBuilder();
			roleJson.add("id", role.getId() );
			roleJson.add("alias", role.getAlias() );
			roleJson.add("name", role.getName());
			roleJson.add("ownerId", role.getOwner().getId());
			roleJson.add("permissions", json(role.permissions()));
			rolesArrayBuilder.add(roleJson);
		}
        
        return Util.jsonArray2prettyString(rolesArrayBuilder.build());
	}
	
	@GET
	@Path("{id}")
	public String viewRole( @PathParam("id") Long id ) {
		DataverseRole role = rolesSvc.find(id);
		if ( role == null ) {
			return error( "role with id " + id + " not found");
		} else  {
			return ok( json(role).build() );
		}
	}
	
	@POST
	@Path("assignments")
	public String assignRole( @FormParam("username") String username, 
			@FormParam("roleId") long roleId, 
			@FormParam("definitionPointId") long dvObjectId,
			@QueryParam("key") String key ) {
		DataverseUser u = usersSvc.findByUserName(username);
		if ( u == null ) return error("no user with username " + username );
		DataverseUser issuer = usersSvc.findByUserName(key);
		if ( issuer == null ) return error("invalid api key '" + key +"'" );
		Dataverse d = dvSvc.find( dvObjectId );
		if ( d == null ) return error("no DvObject with id " + dvObjectId );
		DataverseRole r = rolesSvc.find(roleId);
		if ( r == null ) return error("no role with id " + roleId );
		
		try {
			RoleAssignment ra = engineSvc.submit( new AssignRoleCommand(u,r,d, issuer) );
			return ok( json(ra).build() );
			
		} catch (CommandException ex) {
			logger.log( Level.WARNING, "Error Assigning role", ex );
			return error("Assignment Faild: " + ex.getMessage() );
		}
	}
	
	@POST
	public String createNewRole( @FormParam("alias")   String alias,
								 @FormParam("permissions") String permissionNames,
								 @FormParam("dataverseId") long dataverseId,
								 @FormParam("key") String key ) {
		DataverseUser u = usersSvc.findByUserName(key);
		if ( u == null ) return error("bad api key " + key );
		Dataverse d = dvSvc.find( dataverseId );
		if ( d == null ) return error("no dataverse with id " + dataverseId );
		
		DataverseRole role = new DataverseRole();
		if ( permissionNames.toUpperCase().equals("ALL") ) {
			for ( Permission p : Permission.values() ) {
				role.addPermission(p);
			}
		} else {
			for ( String prm : permissionNames.split(",") ) {
				try {
					role.addPermission(Permission.valueOf(prm.trim()));
				} catch ( IllegalArgumentException iae ) {
					return error("Unknown permission '" + prm.trim() + "'");
				}
			}
		}
		
		role.setName(alias);
		role.setAlias(alias);
		role.setOwner(d);
		
		rolesSvc.save( role );
		
		return ok( json(role).build() );
		
	}
	
}
