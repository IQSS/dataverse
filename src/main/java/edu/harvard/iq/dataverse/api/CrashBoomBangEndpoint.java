package edu.harvard.iq.dataverse.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * An API endpoint that crashes. Used for testing the error handlers. Should 
 * be removed once #3423 is closed.
 * 
 * @author michael
 */
@Path("boom")
public class CrashBoomBangEndpoint extends AbstractApiBean {
    
    @GET
    @Path("aoob")
    public Response arrayError() {
        String boom = "abc".split("3")[9];
        return ok("Not gonna happen");
    }
    
    @GET
    @Path("npe")
    public Response nullPointer() {
        String boom = null;
        boom.length();
        return ok("Not gonna happen");
    }
}
