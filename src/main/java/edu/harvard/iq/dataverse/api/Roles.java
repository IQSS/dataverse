package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRole;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.DataverseUserServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.RoleAssignment;
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
import java.util.EnumSet;
import javax.ws.rs.QueryParam;

/**
 * Util API for managing roles. Might not make it to the production version.
 * @author michael
 */
@Path("roles")
public class Roles extends AbstractApiBean {
	
	private static final Logger logger = Logger.getLogger(Roles.class.getName());
	
	@EJB
	DataverseRoleServiceBean rolesSvc;
	
	@EJB
	DataverseUserServiceBean usersSvc;
	
	@EJB
	DataverseServiceBean dvSvc;
	
	@GET
	public String list() {
		JsonArrayBuilder rolesArrayBuilder = Json.createArrayBuilder();
		for ( DataverseRole role : rolesSvc.findAll() ) {
			rolesArrayBuilder.add(json(role));
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
	
	// TODO move top JSON
	@POST
	public String createNewRole( RoleDTO roleDto,
								 @QueryParam("dvo") String dvoIdtf,
								 @QueryParam("key") String key ) {
		DataverseUser u = usersSvc.findByUserName(key);
		if ( u == null ) return error("bad api key " + key );
		Dataverse d = findDataverse(dvoIdtf);
		if ( d == null ) return error("no dataverse with id " + dvoIdtf );
		
		DataverseRole role = roleDto.asRole();
		
		role.setOwner(d);
		
		role = rolesSvc.save( role );
		
		return ok( json(role).build() );
	}
	
	
	public static class RoleDTO {
		String alias;
		String name;
		String description;
		String ownerId;
		String[] permissions;

		public String getAlias() {
			return alias;
		}

		public void setAlias(String alias) {
			this.alias = alias;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String[] getPermissions() {
			return permissions;
		}

		public void setPermissions(String[] permissions) {
			this.permissions = permissions;
		}
		
		public DataverseRole asRole() {
			DataverseRole r = new DataverseRole();
			r.setAlias(alias);
			r.setDescription(description);
			r.setName(name);
			if ( permissions != null ) {
				if ( permissions.length > 0 ) {
					if ( permissions[0].trim().toLowerCase().equals("all") ) {
						r.addPermissions(EnumSet.allOf(Permission.class));
					} else {
						for ( String ps : permissions ) {
							r.addPermission( Permission.valueOf(ps) );
						}
					}
				}
			}
			return r;
		}
		
	}
}
