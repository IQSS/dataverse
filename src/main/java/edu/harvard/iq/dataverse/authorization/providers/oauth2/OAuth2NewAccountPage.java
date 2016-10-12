package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.inject.Inject;

/**
 * Backing bean for {@code oauth/welcome.xhtml}, the page that greets new
 * users when they first login through OAuth2.
 * 
 * @author michael
 */
@Named
@SessionScoped
public class OAuth2NewAccountPage implements java.io.Serializable {

    
    @EJB
    AuthenticationServiceBean authenticationSvc;
    
    @Inject
    DataverseSession session;
    
    OAuth2UserRecord newUser;
    
    /**
     * @return A textual reference to the user.
     */
    public String getUserReference() {
        AuthenticatedUserDisplayInfo udi = newUser.getDisplayInfo();
        return (udi.getFirstName() + " " + udi.getLastName()).trim();
    }
    
    public OAuth2UserRecord getNewUser() {
        return newUser;
    }

    public void setNewUser(OAuth2UserRecord newUser) {
        this.newUser = newUser;
    }
    
    public void setupMockData() {
        newUser = new OAuth2UserRecord("github", "1928379173561510", "mich.barsinai", "qwe-addssd-iiiiie",
                                        new AuthenticatedUserDisplayInfo("Michael", "Bar-Sinai", "m@mbarsinai.com", "aff", "pos"));
    }
}
