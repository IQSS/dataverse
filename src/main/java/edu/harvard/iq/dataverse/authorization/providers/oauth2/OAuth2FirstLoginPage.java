package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.event.ValueChangeEvent;
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
public class OAuth2FirstLoginPage implements java.io.Serializable {

    
    @EJB
    AuthenticationServiceBean authenticationSvc;
    
    @Inject
    DataverseSession session;
    
    OAuth2UserRecord newUser;
    
    String username;
    
    String selectedEmail;
    
    int selectedTabIndex=0;
    
    public String createNewAccount() {
        
        AuthenticatedUserDisplayInfo newAud = new AuthenticatedUserDisplayInfo(newUser.getDisplayInfo().getFirstName(),
                newUser.getDisplayInfo().getLastName(),
                getSelectedEmail(),
                newUser.getDisplayInfo().getAffiliation(),
                newUser.getDisplayInfo().getPosition());
        final AuthenticatedUser user = authenticationSvc.createAuthenticatedUser(newUser.getUserRecordIdentifier(), getUsername(), newAud, true);
        session.setUser(user);
        
        return "/dataverse.xhtml?faces-redirect=true";
    }
    
    public String testAction() {
        Logger.getLogger(OAuth2FirstLoginPage.class.getName()).log(Level.INFO, "testAction");
        return "dataverse.xhtml";
    }
    
    public boolean isEmailAvailable() {
        return authenticationSvc.isEmailAvailable(getSelectedEmail());
    }
    
    public boolean isUsernameAvailable() {
        return ! authenticationSvc.identifierExists(getUsername());
    }
    
    public void suggestedEmailChanged( ValueChangeEvent evt )  {
        setSelectedEmail(evt.getNewValue().toString());
    }
    
    public boolean isNewAccountCreationEnabled() {
        return isUsernameAvailable() && isEmailAvailable();
    }
    
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
        setUsername( newUser.getUsername() );
        setSelectedEmail( newUser.getDisplayInfo().getEmailAddress() );
    }
    
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public void setSelectedTabIndex(int selectedTabIndex) {
        this.selectedTabIndex = selectedTabIndex;
    }

    public void setSelectedEmail(String selectedEmail) {
        this.selectedEmail = selectedEmail;
    }

    public String getSelectedEmail() {
        return selectedEmail;
    }
    
    public boolean areExtraEmailsAvailable() {
        return newUser.getAvailableEmailAddresses().size() > 1;
    }
    
    public List<String> getExtraEmails() {
        return newUser.getAvailableEmailAddresses();
    }
    
    
}