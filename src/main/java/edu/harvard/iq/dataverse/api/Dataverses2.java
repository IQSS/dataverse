package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRole;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.RoleAssignment;
import static edu.harvard.iq.dataverse.api.JsonPrinter.json;
import edu.harvard.iq.dataverse.api.dto.RoleAssignmentDTO;
import edu.harvard.iq.dataverse.api.dto.RoleDTO;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListRoleAssignments;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 * A REST API for dataverses. To be unified with {@link Dataverses}.
 * TODO unify with Dataverses.java.
 * @author michael
 */
@Stateless
@Path("dvs")
public class Dataverses2 extends AbstractApiBean {
	private static final Logger logger = Logger.getLogger(Dataverses2.class.getName());
	
	@GET
	public String list() {
		JsonArrayBuilder bld = Json.createArrayBuilder();
		for ( Dataverse d : dataverseSvc.findAll() ) {
			bld.add(json(d));
		}
		return ok( bld.build() );
	}
	
	@POST
	public String addRoot( Dataverse d, @QueryParam("key") String apiKey ) {
		return addDataverse(d, "", apiKey);
	}
	
	@GET
	@Path("{identifier}")
	public String viewDataverse( @PathParam("identifier") String idtf ) {
		Dataverse d = findDataverse(idtf);
		return ( d==null) ? error("Can't find dataverse with identifier '" + idtf + "'")
						  : ok( json(d) );
	}
	
	@POST
	@Path("{identifier}")
	public String addDataverse( Dataverse d, @PathParam("identifier") String parentIdtf, @QueryParam("key") String apiKey) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return error( "Invalid apikey '" + apiKey + "'");
		
		if ( ! parentIdtf.isEmpty() ) {
			Dataverse owner = isNumeric(parentIdtf) ? dataverseSvc.find( Long.parseLong(parentIdtf))
													: dataverseSvc.findByAlias( parentIdtf );
			if ( owner == null ) {
				return error( "Can't find dataverse with identifier='" + parentIdtf + "'");
			}
			d.setOwner(owner);
		}
		
		try {
			d = engineSvc.submit( new CreateDataverseCommand(d, u) );
			return ok( json(d).build() );
		} catch (CommandException ex) {
			logger.log(Level.SEVERE, "Error creating dataverse", ex);
			return error("Error creating dataverse: " + ex.getMessage() );
		}
	}
	
	@GET
	@Path("{identifier}/roles")
	public String listRoles( @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return error( "Invalid apikey '" + apiKey + "'");

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) {
			return error( "Can't find dataverse with identifier='" + dvIdtf + "'");
		}
		
		JsonArrayBuilder jab = Json.createArrayBuilder();
		for ( DataverseRole r : dataverse.getRoles() ){
			jab.add( json(r) );
		}
		return ok(jab);
	}
	
	@POST
	@Path("{identifier}/roles")
	public String createRole( RoleDTO roleDto, @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return error( "Invalid apikey '" + apiKey + "'");

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) {
			return error( "Can't find dataverse with identifier='" + dvIdtf + "'");
		}
		try {
			return ok(json(engineSvc.submit( new CreateRoleCommand(roleDto.asRole(), u, dataverse) )));
		} catch ( CommandException ce ) {
			return error( ce.getMessage() );
		}
	}
	
	@GET
	@Path("{identifier}/assignments")
	public String listAssignments( @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return error( "Invalid apikey '" + apiKey + "'");

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) {
			return error( "Can't find dataverse with identifier='" + dvIdtf + "'");
		}
		
		try {
			JsonArrayBuilder jab = Json.createArrayBuilder();
			for ( RoleAssignment ra : engineSvc.submit(new ListRoleAssignments(u, dataverse)) ){
				jab.add( json(ra) );
			}
			return ok(jab);
			
		} catch (CommandException ex) {
			return error( "can't list assignments: " + ex.getMessage() );
		}
	}
	
	@POST
	@Path("{identifier}/assignments")
	public String createAssignment( RoleAssignmentDTO ra, @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		DataverseUser actingUser = userSvc.findByUserName(apiKey);
		if ( actingUser == null ) return error( "Invalid apikey '" + apiKey + "'"); 

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) {
			return error( "Can't find dataverse with identifier='" + dvIdtf + "'");
		}
		DataverseUser grantedUser = (ra.getUserName()!=null) ? findUser(ra.getUserName()) : userSvc.find(ra.getUserId());
		if ( grantedUser==null ) {
			return error("Can't find user using " + ra.getUserName() + "/" + ra.getUserId() );
		}
		
		DataverseRole theRole;
		if ( ra.getRoleId() != 0 ) {
			theRole = rolesSvc.find(ra.getRoleId());
			if ( theRole == null ) {
				return error("Can't find role with id " + ra.getRoleId() );
			}
			
		} else {
			Dataverse dv = dataverse;
			theRole = null;
			while ( (theRole==null) && (dv!=null) ) {
				for ( DataverseRole aRole : dv.getRoles() ) {
					if ( aRole.getAlias().equals(ra.getRoleAlias()) ) {
						theRole = aRole;
						break;
					}
				}
				dv = dv.getOwner();
			}
			if ( theRole == null ) {
				return error("Can't find role named '" + ra.getRoleAlias() + "' in dataverse " + dataverse);
			}
		}
		
		try {
			RoleAssignment roleAssignment = engineSvc.submit( new AssignRoleCommand(grantedUser, theRole, dataverse, actingUser));
			return ok(json(roleAssignment));
			
		} catch (CommandException ex) {
			logger.log(Level.WARNING, "Can't create assignment: " + ex.getMessage(), ex );
			return error(ex.getMessage());
		}
	}
	
	@DELETE
	@Path("{identifier}/assignments/{id}")
	public String deleteAssignment( @PathParam("id") long assignmentId, @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		DataverseUser actingUser = userSvc.findByUserName(apiKey);
		if ( actingUser == null ) return error( "Invalid apikey '" + apiKey + "'"); 

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) {
			return error( "Can't find dataverse with identifier='" + dvIdtf + "'");
		}
		
		RoleAssignment ra = em.find( RoleAssignment.class, assignmentId );
		if ( ra != null ) {
			em.remove( ra );
			em.flush();
			return "Role assignment " + assignmentId + " removed";
		} else {
			return "Role assignment " + assignmentId + " not found";
		}
	}
	
	
	@GET
	@Path(":gv")
	public String toGraphviz() {
		StringBuilder sb = new StringBuilder();
		StringBuilder edges = new StringBuilder();
		
		sb.append( "digraph dataverses {");
		for ( Dataverse dv : dataverseSvc.findAll() ) {
			sb.append("dv").append(dv.getId()).append(" [label=\"").append(dv.getAlias()).append( "\"]\n");
			if ( dv.getOwner() != null ) {
				edges.append("dv").append(dv.getOwner().getId())
						.append("->")
					.append("dv").append(dv.getId())
					.append("\n");
			}
		}
		
		sb.append("\n");
		sb.append( edges );
		
		sb.append( "}" );
		return sb.toString();
	}
	
}
