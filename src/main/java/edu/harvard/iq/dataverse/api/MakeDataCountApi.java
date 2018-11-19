package edu.harvard.iq.dataverse.api;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Note that there are makeDataCount endpoints in Datasets.java as well.
 */
@Path("admin/makeDataCount")
public class MakeDataCountApi extends AbstractApiBean {

    @POST
    @Path("sendToHub")
    public Response sendDataToHub() {
        String msg = "Data has been sent to Make Data Count";
        return ok(msg);
    }

}
