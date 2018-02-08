package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.harvard.iq.dataverse.provenance.ProvenanceRestServiceBean;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This may not be the best way to go about testing this functionality
 * We definitely need tests that use the RestService in our code, but as I go this is useful
 * 
 * @author madunlap
 */
public class CPLExternalIT {
    
    private static final Logger logger = Logger.getLogger(CPLExternalIT.class.getCanonicalName());
    
    ProvenanceRestServiceBean provRest = new ProvenanceRestServiceBean(); //MAD: I tried ejb / inject, didn't work, dunno why...
    
    @BeforeClass
    public static void setUpClass() {
       // provRest ;
    }
    
    @Test
    public void testVersion() throws UnirestException {
        logger.info("Version is " + provRest.getVersion());
    }
    
    //MAD: currently fails due to internal CPL error
    @Test
    public void testAnything() throws UnirestException {
//        logger.info(provRest.postBundle().toString()); //this command should return the bundleId right?
        logger.info(provRest.getBundle("3").get("name"));
//        provRest.deleteBundle("3");
    }
    
}
