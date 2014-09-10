package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.Setting;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * Where the secure, setup API calls live.
 * @author michael
 */
@Path("s")
public class Admin extends AbstractApiBean {
    
    @Path("settings")
    @GET
    public Response listAllSettings() {
        JsonObjectBuilder bld = jsonObjectBuilder();
        for ( Setting s : settingsSvc.listAll() ) {
            bld.add(s.getName(), s.getContent());
        }
        return okResponse(bld);
    }
    
    @Path("settings/{name}/{content}")
    @PUT
    public Response addSetting( @PathParam("name") String name, @PathParam("content") String content ) {
        try {
            Setting s = settingsSvc.set(name, URLDecoder.decode(content, "UTF-8"));
            return okResponse( jsonObjectBuilder().add(s.getName(), s.getContent()) );
        } catch (UnsupportedEncodingException ex) {
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }
    
    @Path("settings/{name}")
    @GET
    public Response getSetting( @PathParam("name") String name ) {
        String s = settingsSvc.get(name);
        
        return ( s != null ) 
                ? okResponse( s ) 
                : notFound("Setting " + name + " not found");
    }
    
    @Path("settings/{name}")
    @DELETE
    public Response deleteSetting( @PathParam("name") String name ) {
        settingsSvc.delete(name);
        
        return okResponse("Setting " + name +  " deleted.");
    }
    
    @Path("setup")
    @POST
    public Response setup() {
        settingsSvc.set( SettingsServiceBean.Key.AllowSignUp, "1" );
        settingsSvc.set( SettingsServiceBean.Key.SignUpUrl, "builtin/signup.xhtml" );
        return okResponse("setup done");
    }
    
    @Path("test")
    @GET
    public Response test() {
        JsonObjectBuilder bld = jsonObjectBuilder();
        for ( Setting s : settingsSvc.listAll() ) {
            bld.add(s.getName(), settingsSvc.get(s.getName()));
        }
        return okResponse(bld);
    }
}
