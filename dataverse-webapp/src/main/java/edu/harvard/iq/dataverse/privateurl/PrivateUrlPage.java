package edu.harvard.iq.dataverse.privateurl;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import javax.faces.view.ViewScoped;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.inject.Named;

import java.io.Serializable;
import java.util.logging.Logger;

/**
 * Backing bean for JSF page. Sets session to {@link PrivateUrlUser}.
 */
@ViewScoped
@Named("PrivateUrlPage")
public class PrivateUrlPage implements Serializable {

    private static final Logger logger = Logger.getLogger(PrivateUrlPage.class.getCanonicalName());

    @EJB
    PrivateUrlServiceBean privateUrlService;
    @Inject
    DataverseSession session;

    /**
     * The unique string used to look up a PrivateUrlUser and the associated
     * draft dataset version to redirect the user to.
     */
    String token;

    public String init() {
        try {
            PrivateUrlRedirectData privateUrlRedirectData = privateUrlService.getPrivateUrlRedirectDataFromToken(token);
            String datasetPageToBeRedirectedTo = privateUrlRedirectData.getDatasetPageToBeRedirectedTo() + "&faces-redirect=true";
            PrivateUrlUser privateUrlUser = privateUrlRedirectData.getPrivateUrlUser();
            session.setUser(privateUrlUser);
            logger.info("Redirecting PrivateUrlUser '" + privateUrlUser.getIdentifier() + "' to " + datasetPageToBeRedirectedTo);
            return datasetPageToBeRedirectedTo;
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
