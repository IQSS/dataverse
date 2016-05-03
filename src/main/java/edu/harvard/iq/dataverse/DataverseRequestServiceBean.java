package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

/**
 * The service bean to go to when one needs the current {@link DataverseRequest}.
 * @author michael
 */
@Named
@RequestScoped
public class DataverseRequestServiceBean {
    
    @Inject
    DataverseSession dataverseSessionSvc;
    
    @Context
    HttpServletRequest httpRequest;
    
    private DataverseRequest dataverseRequest;
    
    @PostConstruct
    protected void setup() {
        dataverseRequest = new DataverseRequest(dataverseSessionSvc.getUser(), getRequest());
    }
    
    private HttpServletRequest getRequest() {
        if ( httpRequest != null ) {
            return httpRequest;
        } else {
            final FacesContext jsfCtxt = FacesContext.getCurrentInstance();
            if ( jsfCtxt != null ) {
                return (HttpServletRequest) jsfCtxt.getExternalContext().getRequest();
            } else {
                Logger.getLogger(DataverseRequestServiceBean.class.getName()).log(Level.WARNING, "Cannot get the HTTP request object.");
                return null;
            }
        }
    }

    public DataverseRequest getDataverseRequest() {
        return dataverseRequest;
    }
    
}
