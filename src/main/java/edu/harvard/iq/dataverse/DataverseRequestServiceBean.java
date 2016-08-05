package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

/**
 * The service bean to go to when one needs the current {@link DataverseRequest}.
 * @author michael
 */
@Named
@RequestScoped
public class DataverseRequestServiceBean {
    
    @Inject
    DataverseSession dataverseSessionSvc;
    
    @Inject
    private HttpServletRequest request;
    
   private DataverseRequest dataverseRequest;
    
    @PostConstruct
    protected void setup() {
        dataverseRequest = new DataverseRequest(dataverseSessionSvc.getUser(), request);
    }
    
    public DataverseRequest getDataverseRequest() {
        return dataverseRequest;
    }
    
}
