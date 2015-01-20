package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroupProvider;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
/**
 *
 * @author michael
 */
@Path("groups")
@Stateless
public class Groups extends AbstractApiBean {
    private static final Logger logger = Logger.getLogger(Groups.class.getName());
    
    private IpGroupProvider ipGroupPrv;
    
    @PostConstruct
    void postConstruct() {
        ipGroupPrv = groupSvc.getIpGroupProvider();
    }
    
    @POST
    @Path("ip")
    public Response createIpGroups( JsonObject dto ){
        try {
           IpGroup grp = new JsonParser(null,null,null).parseIpGroup(dto);
            
            if ( grp.getPersistedGroupAlias()== null ) {
                return errorResponse(Response.Status.BAD_REQUEST, "Must provide valid group alias");
            }
            grp.setProvider( groupSvc.getIpGroupProvider() );
            
            grp = ipGroupPrv.store(grp);
            return createdResponse("/groups/ip/" + grp.getPersistedGroupAlias(), json(grp) );
        
        } catch ( Exception e ) {
            logger.log( Level.WARNING, "Error while storing a new IP group: " + e.getMessage(), e);
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage() );
            
        }
    }
    
    
    @GET
    @Path("ip")
    public Response listIpGroups() {
         
        JsonArrayBuilder arrBld = Json.createArrayBuilder();
        for ( IpGroup g : ipGroupPrv.findAll() ) {
            arrBld.add( json(g) );
        }
        return okResponse( arrBld );
    }
    
    @GET
    @Path("ip/{groupIdtf}")
    public Response getIpGroup( @PathParam("groupIdtf") String groupIdtf ) {
        IpGroup grp;
        if ( isNumeric(groupIdtf) ) {
            grp = ipGroupPrv.get( Long.parseLong(groupIdtf) );
        } else {
            grp = ipGroupPrv.get(groupIdtf);
        }
        
        return (grp == null) ? notFound( "Group " + groupIdtf + " not found") : okResponse(json(grp));
    }
    
    @DELETE
    @Path("ip/{groupIdtf}")
    public Response deleteIpGroup( @PathParam("groupIdtf") String groupIdtf ) {
        IpGroup grp;
        if ( isNumeric(groupIdtf) ) {
            grp = ipGroupPrv.get( Long.parseLong(groupIdtf) );
        } else {
            grp = ipGroupPrv.get(groupIdtf);
        }
        
        if (grp == null) return notFound( "Group " + groupIdtf + " not found");
        
        try {
            ipGroupPrv.deleteGroup(grp);
            return okResponse("Group " + grp.getAlias() + " deleted.");
        } catch ( IllegalArgumentException ex ) {
            return errorResponse(Response.Status.BAD_REQUEST, ex.getMessage());
        }
    }
    
    
    
}
