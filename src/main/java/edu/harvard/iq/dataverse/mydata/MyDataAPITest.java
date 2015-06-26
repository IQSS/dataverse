/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.api.Access;
import edu.harvard.iq.dataverse.api.BundleDownloadInstance;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import static java.lang.Math.max;
import java.math.BigDecimal;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

/**
 *
 * @author rmp553
 */
@Path("mydata")
public class MyDataAPITest extends AbstractApiBean {

    @Inject
    DataverseSession session;

    private static final Logger logger = Logger.getLogger(Access.class.getCanonicalName());
    
    
    private int randInt(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
    }
    
    
    public Pager getRandomPagerPager(Integer selectedPage) throws JSONException{
        if (selectedPage == null){
            selectedPage = 1;
        }
        
        int itemsPerPage = 10;
        int numResults = 108;//randInt(1,200);
        int numPages =  numResults / itemsPerPage;
        if ((numResults % itemsPerPage) > 0){
            numPages++;
        }
        int chosenPage = 1;
        if ((selectedPage > numPages)||(selectedPage < 1)){
            chosenPage = 1;
        }else{
            chosenPage = selectedPage;
        }
        //int chosenPage = max(randInt(0, numPages), 1);
        return new Pager(numResults, itemsPerPage, chosenPage);
                
    }
    
    @Path("test-it2")
    @GET
    @Produces({"application/json"})
    public String retrieveTestPager(@QueryParam("selectedPage") int selectedPage) throws JSONException{
        
        return this.getRandomPagerPager(selectedPage).asJSONString();
        /*
        JSONObject jsonData = new JSONObject();
        jsonData.put("name", "foo");
        jsonData.put("num", new Integer(100));
        jsonData.put("balance", new Double(1000.21));
        jsonData.put("is_vip", new Boolean(true));

        if (session == null){
            jsonData.put("has-session", false);
        } else{
            jsonData.put("has-session", true);
            if (session.getUser()==null){
                jsonData.put("has-user", false);
            }else{
                jsonData.put("has-user", true);
                if (session.getUser().isAuthenticated()){
                    jsonData.put("auth-status", "AUTHENTICATED");
                    AuthenticatedUser authUser = (AuthenticatedUser)session.getUser();
                    jsonData.put("username", authUser.getIdentifier());
                }else{
                    jsonData.put("auth-status", "GET OUT - NOT AUTHENTICATED");
                }
            }
            
        }
        return jsonData.toString();*/
    }
    
    //@Produces({"application/zip"})
    @Path("test-it")
    @GET
    public Response retrieveMyData(@QueryParam("key") String keyValue) throws JSONException{ //String myDataParams) {
        
        final JsonObjectBuilder jsonData = Json.createObjectBuilder();
        jsonData.add("name", keyValue);
        
        if (session == null){
            jsonData.add("has-session", false);
        } else{
            jsonData.add("has-session", true);
            if (session.getUser()==null){
                jsonData.add("has-user", false);
            }else{
                jsonData.add("has-user", true);
                if (session.getUser().isAuthenticated()){
                    jsonData.add("auth-status", "AUTHENTICATED");
                    AuthenticatedUser authUser = (AuthenticatedUser)session.getUser();
                    jsonData.add("username", authUser.getIdentifier());
                }else{
                    jsonData.add("auth-status", "GET OUT - NOT AUTHENTICATED");
                }
            }
            
        }
        JSONObject obj = new JSONObject();
        obj.put("name", "foo");
        obj.put("num", new Integer(100));
        obj.put("balance", new Double(1000.21));
        obj.put("is_vip", new Boolean(true));
        
        return okResponse(jsonData);
        
        
        //return okResponse(obj);
    }
}        