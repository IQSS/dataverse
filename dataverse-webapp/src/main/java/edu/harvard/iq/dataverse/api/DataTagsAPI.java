package edu.harvard.iq.dataverse.api;


import edu.harvard.iq.dataverse.DataTagsContainer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author Naomi
 */
@Stateless
@Path("datatags")
public class DataTagsAPI extends AbstractApiBean { 
    
    private static final String TAGGING_SERVER_ENDPOINT = "http://datatags.org/api/1/interviewLink";
    private static final String CALLBACK_URL = "http://dvn-build.hmdc.harvard.edu/api/datatags/receiveTags/" + java.util.UUID.randomUUID().toString();
    private static final String USER_REDIRECT_URL = "http://dvn-build.hmdc.harvard.edu/datatags-api-test-deposit-complete.xhtml";
    
    private static final Map<String, DataTagsContainer> CACHE = new ConcurrentHashMap<>();
    
        
    private static final Logger logger = Logger.getLogger(DataTagsAPI.class.getName());
    
    
    @EJB
    DataTagsContainer container;
    
    
    /**
     * send GET request to tagging server API for URL for user
     * @return the URL at which the interview waits.
     */
    public String requestInterview() {
        Client client = ClientBuilder.newClient();
        WebTarget endpoint = client.target(TAGGING_SERVER_ENDPOINT);
                //.path("{repositoryName}").path("{callbackURL}");
        JsonObject jsonInfo = endpoint.queryParam("repositoryName", "Dataverse")
                .queryParam("callbackURL", CALLBACK_URL).request(MediaType.TEXT_PLAIN)
                .get(JsonObject.class);
        String url = jsonInfo.getJsonString("data").toString();
        logger.info(url);
        return url;
    }
    
    public void setCache(String key, DataTagsContainer value) {
        CACHE.put(key, value);
    }
    
    public Map<String, DataTagsContainer> getCache() {
        return CACHE;
    }
    
    public DataTagsContainer getContainer() {
        return container;
    }
    
    public String getCallbackURL() {
        return CALLBACK_URL;
    }
    

    
    @POST
    @Path("receiveTags/{uniqueCacheId}")
    public Response receiveTags(JsonObject tags, @PathParam("uniqueCacheId") String uniqueCacheId) {
        
        // store json tags in the DataTagsContainer holding the dataset name
        container.setTag(tags);
        CACHE.put(uniqueCacheId, container);
        
        // return an OK message with the redirect URL for the user to return to Dataverse through postBackTo or unacceptableDataset in DataTags
        return ok( USER_REDIRECT_URL );
    }
    
}