package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationRequest;
import edu.harvard.iq.dataverse.authorization.AuthenticationResponse;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.CredentialsAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthenticationFailedException;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;

import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.validator.ValidatorException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

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

    @EJB
    SystemConfig systemConfig;
    
    @Inject
    DataverseRequestServiceBean dvRequestService;
    
    private String credentialsAuthProviderId;
    
    private List<FilledCredential> filledCredentials;
    
    private String redirectPage = "dataverse.xhtml";
    private AuthenticationProvider authProvider;
    private int numFailedLoginAttempts;
    Random random;
    long op1;
    long op2;
    Long userSum;

    public void init() {
        Iterator<String> credentialsIterator = authSvc.getAuthenticationProviderIdsOfType( CredentialsAuthenticationProvider.class ).iterator();
        if ( credentialsIterator.hasNext() ) {
            setCredentialsAuthProviderId(credentialsIterator.next());
        }
        resetFilledCredentials(null);
        authProvider = authSvc.getAuthenticationProvider(systemConfig.getDefaultAuthProvider());
        random = new Random();
    }

    public List<AuthenticationProviderDisplayInfo> listCredentialsAuthenticationProviders() {
        List<AuthenticationProviderDisplayInfo> infos = new LinkedList<>();
        for ( String id : authSvc.getAuthenticationProviderIdsOfType( CredentialsAuthenticationProvider.class ) ) {
            AuthenticationProvider authenticationProvider = authSvc.getAuthenticationProvider(id);
            infos.add( authenticationProvider.getInfo());
        }
        return infos;
    }
    
    /**
     * Retrieve information about all enabled identity providers in a sorted order to be displayed to the user.
     * @return list of display information for each provider
     */
    public List<AuthenticationProviderDisplayInfo> listAuthenticationProviders() {
        List<AuthenticationProviderDisplayInfo> infos = new LinkedList<>();
        List<AuthenticationProvider> idps = new ArrayList<>(authSvc.getAuthenticationProviders());
        
        // sort by order first. in case of same order values, be deterministic in UI and sort by id, too.
        Collections.sort(idps, Comparator.comparing(AuthenticationProvider::getOrder).thenComparing(AuthenticationProvider::getId));
        
        for (AuthenticationProvider idp : idps) {
            if (idp != null) {
                infos.add(idp.getInfo());
            }
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
            authReq.putCredential(fc.getCredential().getKey(), fc.getValue());
        }

        authReq.setIpAddress( dvRequestService.getDataverseRequest().getSourceAddress() );
        try {
            AuthenticatedUser r = authSvc.getUpdateAuthenticatedUser(credentialsAuthProviderId, authReq);
            logger.log(Level.FINE, "User authenticated: {0}", r.getEmail());
            session.setUser(r);
            if ("dataverse.xhtml".equals(redirectPage)) {
                redirectPage = redirectToRoot();
            }
            
            try {            
                redirectPage = URLDecoder.decode(redirectPage, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(LoginPage.class.getName()).log(Level.SEVERE, null, ex);
                redirectPage = redirectToRoot();
            }

            logger.log(Level.FINE, "Sending user to = {0}", redirectPage);
            return redirectPage + (!redirectPage.contains("?") ? "?" : "&") + "faces-redirect=true";

            
        } catch (AuthenticationFailedException ex) {
            numFailedLoginAttempts++;
            op1 = new Long(random.nextInt(10));
            op2 = new Long(random.nextInt(10));
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
                    FacesContext.getCurrentInstance().getExternalContext().getFlash().put("silentUpgradePasswd",authReq.getCredential(BuiltinAuthenticationProvider.KEY_PASSWORD));
                    return response.getMessage();
                default:
                    JsfHelper.addErrorMessage("INTERNAL ERROR");
                    return null;
            }
        }
        
    }
    
    private String redirectToRoot(){
        return "dataverse.xhtml?alias=" + dataverseService.findRootDataverse().getAlias();
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

    public AuthenticationProvider getAuthProvider() {
        return authProvider;
    }

    public void setAuthProviderById(String authProviderId) {
        logger.fine("Setting auth provider to " + authProviderId);
        this.authProvider = authSvc.getAuthenticationProvider(authProviderId);
    }

    public String getLoginButtonText() {
        if (authProvider != null) {
            // Note that for ORCID we do not want the normal "Log In with..." text. There is special logic in the xhtml.
            return BundleUtil.getStringFromBundle("login.button", Arrays.asList(authProvider.getInfo().getTitle()));
        } else {
            return BundleUtil.getStringFromBundle("login.button", Arrays.asList("???"));
        }
    }

    public int getNumFailedLoginAttempts() {
        return numFailedLoginAttempts;
    }

    public boolean isRequireExtraValidation() {
        if (numFailedLoginAttempts > 2) {
            return true;
        } else {
            return false;
        }
    }

    public long getOp1() {
        return op1;
    }

    public long getOp2() {
        return op2;
    }

    public Long getUserSum() {
        return userSum;
    }

    public void setUserSum(Long userSum) {
        this.userSum = userSum;
    }

    // TODO: Consolidate with SendFeedbackDialog.validateUserSum?
    public void validateUserSum(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        // The FacesMessage text is on the xhtml side.
        FacesMessage msg = new FacesMessage("");
        ValidatorException validatorException = new ValidatorException(msg);
        if (value == null) {
            throw validatorException;
        }
        if (op1 + op2 != (Long) value) {
            throw validatorException;
        }
    }

}
