package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationRequest;
import edu.harvard.iq.dataverse.authorization.AuthenticationResponse;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.CredentialsAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthenticationFailedException;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author xyang
 * @author Michael Bar-Sinai
 */
@ViewScoped
@Named("LoginPage")
public class LoginPage implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(LoginPage.class.getName());
    public static class FilledCredential {
        CredentialsAuthenticationProvider.Credential credential;
        String value;

        public FilledCredential() {
        }

        public FilledCredential(CredentialsAuthenticationProvider.Credential credential, String value) {
            this.credential = credential;
            this.value = value;
        }
        
        public CredentialsAuthenticationProvider.Credential getCredential() {
            return credential;
        }

        public void setCredential(CredentialsAuthenticationProvider.Credential credential) {
            this.credential = credential;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
        
    }
    
    public enum EditMode {LOGIN, SUCCESS, FAILED};
    
    @Inject DataverseSession session;    
    
    @EJB
    DataverseServiceBean dataverseService;
    
    @EJB
    BuiltinUserServiceBean dataverseUserService;
    
    @EJB
    UserServiceBean userService;
    
    @EJB
    AuthenticationServiceBean authSvc;

    @EJB
    SettingsServiceBean settingsService;
    
    @Inject
    DataverseRequestServiceBean dvRequestService;
    
    private String credentialsAuthProviderId;
    
    private List<FilledCredential> filledCredentials;
    
    private String redirectPage = "dataverse.xhtml";
    
    public void init() {
        Iterator<String> credentialsIterator = authSvc.getAuthenticationProviderIdsOfType( CredentialsAuthenticationProvider.class ).iterator();
        if ( credentialsIterator.hasNext() ) {
            setCredentialsAuthProviderId(credentialsIterator.next());
        }
        resetFilledCredentials(null);
    }
    
    public boolean isAuthenticationProvidersAvailable() {
        return ! authSvc.getAuthenticationProviderIds().isEmpty();
    }
    
    public List<AuthenticationProviderDisplayInfo> listCredentialsAuthenticationProviders() {
        List<AuthenticationProviderDisplayInfo> infos = new LinkedList<>();
        for ( String id : authSvc.getAuthenticationProviderIdsOfType( CredentialsAuthenticationProvider.class ) ) {
            AuthenticationProvider authenticationProvider = authSvc.getAuthenticationProvider(id);
            infos.add( authenticationProvider.getInfo());
        }
        return infos;
    }
    
    public List<AuthenticationProviderDisplayInfo> listAuthenticationProviders() {
        List<AuthenticationProviderDisplayInfo> infos = new LinkedList<>();
        for ( String id : authSvc.getAuthenticationProviderIds() ) {
            AuthenticationProvider authenticationProvider = authSvc.getAuthenticationProvider(id);
            infos.add( authenticationProvider.getInfo());
        }
        return infos;
    }
   
    public CredentialsAuthenticationProvider selectedCredentialsProvider() {
        return (CredentialsAuthenticationProvider) authSvc.getAuthenticationProvider(getCredentialsAuthProviderId());
    }
    
    public boolean validatePassword(String username, String password) {
        return false;
    }

    public String login() {
        
        AuthenticationRequest authReq = new AuthenticationRequest();
        List<FilledCredential> filledCredentialsList = getFilledCredentials();
        if ( filledCredentialsList == null ) {
            logger.info("Credential list is null!");
            return null;
        }
        for ( FilledCredential fc : filledCredentialsList ) {
            if(fc.getValue()==null || fc.getValue().isEmpty()){
                JH.addMessage(FacesMessage.SEVERITY_ERROR, "Please enter a "+fc.getCredential().getTitle());
            }
            authReq.putCredential(fc.getCredential().getTitle(), fc.getValue());
        }
        authReq.setIpAddress( dvRequestService.getDataverseRequest().getSourceAddress() );
        try {
            AuthenticatedUser r = authSvc.authenticate(credentialsAuthProviderId, authReq);
            logger.log(Level.FINE, "User authenticated: {0}", r.getEmail());
            session.setUser(r);
            
            if ("dataverse.xhtml".equals(redirectPage)) {
                redirectPage = redirectPage + "&alias=" + dataverseService.findRootDataverse().getAlias();
            }
            
            try {            
                redirectPage = URLDecoder.decode(redirectPage, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(LoginPage.class.getName()).log(Level.SEVERE, null, ex);
                redirectPage = "dataverse.xhtml&alias=" + dataverseService.findRootDataverse().getAlias();
            }

            logger.log(Level.FINE, "Sending user to = {0}", redirectPage);

            return redirectPage + (!redirectPage.contains("?") ? "?" : "&") + "faces-redirect=true";

            
        } catch (AuthenticationFailedException ex) {
            AuthenticationResponse response = ex.getResponse();
            switch ( response.getStatus() ) {
                case FAIL:
                    JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("login.builtin.invalidUsernameEmailOrPassword"));
                    return null;
                case ERROR:
                    /**
                     * @todo How do we exercise this part of the code? Something
                     * with password upgrade? See
                     * https://github.com/IQSS/dataverse/pull/2922
                     */
                    JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("login.error"));
                    logger.log( Level.WARNING, "Error logging in: " + response.getMessage(), response.getError() );
                    return null;
                case BREAKOUT:
                    return response.getMessage();
                default:
                    JsfHelper.addErrorMessage("INTERNAL ERROR");
                    return null;
            }
        }
        
    }

    public String getCredentialsAuthProviderId() {
        return credentialsAuthProviderId;
    }
    
    public void resetFilledCredentials( AjaxBehaviorEvent event) {
        if ( selectedCredentialsProvider()==null ) return;
        
        filledCredentials = new LinkedList<>();
        for ( CredentialsAuthenticationProvider.Credential c : selectedCredentialsProvider().getRequiredCredentials() ) {
            filledCredentials.add( new FilledCredential(c, ""));
        }
    }
    
    public void setCredentialsAuthProviderId(String authProviderId) {
        this.credentialsAuthProviderId = authProviderId;
    }

    public List<FilledCredential> getFilledCredentials() {
        return filledCredentials;
    }

    public void setFilledCredentials(List<FilledCredential> filledCredentials) {
        this.filledCredentials = filledCredentials;
    }

    public boolean isMultipleProvidersAvailable() {
        return authSvc.getAuthenticationProviderIds().size()>1;
    }

    public String getRedirectPage() {
        return redirectPage;
    }

    public void setRedirectPage(String redirectPage) {
        this.redirectPage = redirectPage;
    }
    
}
