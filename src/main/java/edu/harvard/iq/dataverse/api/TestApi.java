package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.providers.builtin.PasswordEncryption;
import edu.harvard.iq.dataverse.authorization.users.User;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.QueryParam;
import org.mindrot.jbcrypt.BCrypt;

/**
 * An API to test internal models without the need to deal with UI etc.
 *
 * @todo Can this entire class be removed and its methods be moved to Admin.java
 * if they are still needed? Once this is done we can remove this warning:
 * "There is a “test” API endpoint used for development and troubleshooting that
 * has some potentially dangerous methods."
 * http://guides.dataverse.org/en/4.2.4/installation/config.html#blocking-api-endpoints
 * 
 * @author michael
 */
@Stateless
@Path("test")
public class TestApi extends AbstractApiBean {
    private static final Logger logger = Logger.getLogger(TestApi.class.getName());
    
    @Path("echo/{whatever}")
    @GET
    public Response echo( @PathParam("whatever") String body ) {
        return okResponse(body);
    }
    
    @Path("permissions/{dvo}")
    @GET
    public Response findPermissonsOn(@PathParam("dvo") String dvo) {
        try {
            DvObject dvObj = findDvo(dvo);
            if (dvObj == null) {
                return notFound("DvObject " + dvo + " not found");
            }
            try {
                User aUser = findUserOrDie();
                JsonObjectBuilder bld = Json.createObjectBuilder();
                bld.add("user", aUser.getIdentifier() );
                bld.add("permissions", json(permissionSvc.permissionsFor(createDataverseRequest(aUser), dvObj)) );
                return okResponse(bld);

            } catch (WrappedResponse wr) {
                return wr.getResponse();
            }
        } catch (Exception e) {
            logger.log( Level.SEVERE, "Error while testing permissions", e );
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Path("assignee/{idtf}")
    @GET
    public Response findRoleAssignee(@PathParam("idtf") String idtf) {
        RoleAssignee ra = roleAssigneeSvc.getRoleAssignee(idtf);
        return (ra == null) ? notFound("Role Assignee '" + idtf + "' not found.")
                : okResponse(json(ra.getDisplayInfo()));
    }
    
    @Path("bcrypt/encrypt/{word}")
    @GET
    public String encrypt( @PathParam("word")String word, @QueryParam("len") String len ) {
        int saltLen = (len==null || len.trim().isEmpty() ) ? 10 : Integer.parseInt(len);
        return BCrypt.hashpw(word, BCrypt.gensalt(saltLen)) + "\n";
    }
    
    @Path("password/{w1}")
    @GET
    public String test( @PathParam("w1") String w1 ) {
        StringBuilder sb = new StringBuilder();
        sb.append("[0] ").append( PasswordEncryption.getVersion(0).encrypt(w1)).append("\n");
        sb.append("[1] ").append( PasswordEncryption.getVersion(1).encrypt(w1)).append("\n");
        
        return sb.toString();
    }

    @Path("apikey")
    @GET
    public Response testUserLookup() {
        try {
            return okResponse( json(findAuthenticatedUserOrDie()) );
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
}
