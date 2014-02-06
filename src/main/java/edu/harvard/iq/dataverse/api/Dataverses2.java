package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.DataverseUserServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import static edu.harvard.iq.dataverse.api.JsonPrinter.json;
import static edu.harvard.iq.dataverse.api.Util.error;
import static edu.harvard.iq.dataverse.api.Util.isNumeric;
import static edu.harvard.iq.dataverse.api.Util.ok;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 * A REST API for dataverses. To be unified with {@link Dataverses}.
 * TODO unify with Dataverses.java.
 * @author michael
 */
@Path("dvs")
public class Dataverses2 {
	private static final Logger logger = Logger.getLogger(Dataverses2.class.getName());
	@EJB
	DataverseServiceBean dataverseSvc;
	
	@EJB
	DataverseUserServiceBean usersSvc;
	
	@EJB
	EjbDataverseEngine engineSvc;
	
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
	
	@POST
	@Path("{identifier}")
	public String addDataverse( Dataverse d, @PathParam("identifier") String parentIdtf, @QueryParam("key") String apiKey) {
		DataverseUser u = usersSvc.findByUserName(apiKey);
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
}
