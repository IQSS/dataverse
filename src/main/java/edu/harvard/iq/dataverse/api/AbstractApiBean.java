package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.DataverseUserServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseMetadataBlocksCommand;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Base class for API beans
 * @author michael
 */
public abstract class AbstractApiBean {
	
	@EJB
	protected EjbDataverseEngine engineSvc;
	
	@EJB
	protected DataverseUserServiceBean userSvc;
	
	@EJB 
	protected DataverseServiceBean dataverseSvc;
    
    @EJB
    protected DatasetFieldServiceBean datasetFieldSvc;
	
    @EJB
    protected MetadataBlockServiceBean metadataBlockSvc;
    
	@PersistenceContext(unitName = "VDCNet-ejbPU")
	EntityManager em;
	
	@EJB
	DataverseRoleServiceBean rolesSvc;
	
	
	protected DataverseUser findUser( String userIdtf ) {
        
		return isNumeric(userIdtf) ? engineSvc.getContext().users().find(Long.parseLong(userIdtf))
	 							  : engineSvc.getContext().users().findByUserName(userIdtf);
	}
	
	protected Dataverse findDataverse( String idtf ) {
		return isNumeric(idtf) ? dataverseSvc.find(Long.parseLong(idtf))
	 							  : dataverseSvc.findByAlias(idtf);
	}
	
	protected DvObject findDvo( Long id ) {
		return em.createNamedQuery("DvObject.findById", DvObject.class)
				.setParameter("id", id)
				.getSingleResult();
	}
	
    protected MetadataBlock findMetadataBlock(String idtf) throws NumberFormatException {
        return isNumeric(idtf) ? metadataBlockSvc.findById(Long.parseLong(idtf))
                : metadataBlockSvc.findByName(idtf);
    }
    
    protected Response okResponse( JsonArrayBuilder bld ) {
        return Response.ok( Json.createObjectBuilder()
            .add("status", "OK")
            .add("data", bld).build() ).build();
    }
    
    protected Response okResponse( JsonObjectBuilder bld ) {
        return Response.ok( Json.createObjectBuilder()
            .add("status", "OK")
            .add("data", bld).build() ).build();
    }
    
    protected Response okResponse( String msg ) {
        return Response.ok().entity(Json.createObjectBuilder()
            .add("status", "OK")
            .add("data", Json.createObjectBuilder().add("message",msg)).build() ).build();
    }
    
    /**
     * Returns an OK response (HTTP 200, status:OK) with the passed value
     * in the data field.
     * @param value the value for the data field
     * @return a HTTP OK response with the passed value as data.
     */
    protected Response okResponseWithValue( String value ) {
        return Response.ok().entity(Json.createObjectBuilder()
            .add("status", "OK")
            .add("data", value).build() ).build();
    }

    protected Response okResponseWithValue( boolean value ) {
        return Response.ok().entity(Json.createObjectBuilder()
            .add("status", "OK")
            .add("data", value).build() ).build();
    }
    
    protected Response accepted() {
        return Response.accepted()
                .entity(Json.createObjectBuilder()
                        .add("status", "OK").build()
                ).build();
    }
    
    protected Response notFound( String msg ) {
        return errorResponse(Status.NOT_FOUND, msg);
    }
    
    protected Response badApiKey( String apiKey ) {
        return errorResponse(Status.UNAUTHORIZED, "Bad api key '" + apiKey +"'");
    }
    
    protected Response permissionError( PermissionException pe ) {
        return errorResponse( Status.UNAUTHORIZED, pe.getMessage() );
    }
    
    protected Response errorResponse( Status sts, String msg ) {
        return Response.status(sts)
                .entity( Json.createObjectBuilder().add("status", "ERROR")
                        .add( "message", msg ).build())
                .build();
    }
    
    protected Response execute( Command c ) {
         try { 
            engineSvc.submit( c );
            return accepted();
            
        } catch ( PermissionException pex ) {
            return permissionError( pex );
            
        } catch ( CommandException ce ) {
            return errorResponse(Status.INTERNAL_SERVER_ERROR, ce.getLocalizedMessage());
        }
    }
    
	protected boolean isNumeric( String str ) { return Util.isNumeric(str); };
	protected String error( String msg ) { return Util.error(msg); }
	protected String ok( String msg ) { return Util.ok(msg); }
	protected String ok( JsonObject jo ) { return Util.ok(jo); }
	protected String ok( JsonArray jo ) { return Util.ok(jo); }
	protected String ok( JsonObjectBuilder jo ) { return ok(jo.build()); }
	protected String ok( JsonArrayBuilder jo ) { return ok(jo.build()); }
}
