package edu.harvard.iq.dataverse.privateurl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import java.io.Serializable;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Backing bean for JSF page. Sets session to {@link PrivateUrlUser}. 
 */
@ViewScoped
@Named("PrivateUrlPage")
public class PrivateUrlPage implements Serializable {

    private static final Logger logger = Logger.getLogger(PrivateUrlPage.class.getCanonicalName());

    @EJB
    PrivateUrlServiceBean privateUrlService;
    @EJB
    DatasetServiceBean datasetServiceBean;
    @Inject
    DataverseSession session;
    @Inject
    PermissionsWrapper permissionsWrapper;
    @Inject
    DataverseRequestServiceBean dvRequestService;

    /**
     * The unique string used to look up a PrivateUrlUser and the associated
     * draft dataset version to redirect the user to.
     */
    String token;

    public String init() {
        try {
            PrivateUrlRedirectData privateUrlRedirectData = privateUrlService.getPrivateUrlRedirectDataFromToken(token);
            String draftDatasetPageToBeRedirectedTo = privateUrlRedirectData.getDraftDatasetPageToBeRedirectedTo() + "&faces-redirect=true";
            PrivateUrlUser privateUrlUser = privateUrlRedirectData.getPrivateUrlUser();
            boolean sessionUserCanViewUnpublishedDataset = false;
            if (session.getUser().isAuthenticated()){
                Long datasetId = privateUrlUser.getDatasetId();
                Dataset dataset = datasetServiceBean.find(datasetId);
                sessionUserCanViewUnpublishedDataset = permissionsWrapper.canViewUnpublishedDataset(dvRequestService.getDataverseRequest(), dataset);
            }
            if(!sessionUserCanViewUnpublishedDataset){
                //Only Reset if user cannot view this Draft Version
                session.setUser(privateUrlUser); 
            }
            logger.info("Redirecting PrivateUrlUser '" + privateUrlUser.getIdentifier() + "' to " + draftDatasetPageToBeRedirectedTo);
            return draftDatasetPageToBeRedirectedTo;
        } catch (Exception ex) {
            logger.info("Exception processing Private URL token '" + token + "':" + ex);
            return "/404.xhtml";
        }
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
