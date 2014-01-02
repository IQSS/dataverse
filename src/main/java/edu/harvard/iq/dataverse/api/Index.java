package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.IndexServiceBean;
import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("index")
public class Index {

    @EJB
    IndexServiceBean indexService;

    @GET
    public String index() {
        indexService.index();
        return "called index service bean\n";
    }
}
