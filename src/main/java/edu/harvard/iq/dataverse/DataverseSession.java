package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean.StaticPermissionQuery;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SessionUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 *
 * @author gdurand
 */
@Named
@SessionScoped
public class DataverseSession implements Serializable{
    
    /* Note that on logout, variables must be cleared manually in DataverseHeaderFragment*/
    private User user;

    @EJB
    PermissionServiceBean permissionsService;

    @EJB
    BuiltinUserServiceBean usersSvc;
	
    @EJB 
    ActionLogServiceBean logSvc;
    
    @Inject
    SettingsWrapper settingsWrapper;
    
    @Inject
    DataverseHeaderFragment headerFragment;
    
    @EJB
    SystemConfig systemConfig;
    
    @EJB
    BannerMessageServiceBean bannerMessageService;

    @EJB
    AuthenticationServiceBean authenticationService;

    private static final Logger logger = Logger.getLogger(DataverseSession.class.getCanonicalName());
    
    private boolean statusDismissed = false;
    
    private List<BannerMessage> dismissedMessages = new ArrayList<>();

    public List<BannerMessage> getDismissedMessages() {
        return dismissedMessages;
    }

    public void setDismissedMessages(List<BannerMessage> dismissedMessages) {
        this.dismissedMessages = dismissedMessages;
    }

    /**
     * If debug is set to true, some pages show extra debugging information to
     * superusers.
     *
     * The way to set the Boolean to true is to pass debug=true as a query
     * parameter. The Boolean will remain true (even if nothing is passed to it)
     * until debug=false is passed.
     *
     * Because a boolean is false by default when it comes from a viewParam we
     * use a Boolean instead. That way, if the debug viewParam is null, we can
     * leave the state alone (see setDebug()).
     */
    private Boolean debug;
    
    public User getUser() {
        return getUser(false);
    }

    /**
     * For performance reasons, we only lookup the authenticated user again (to
     * check if it has been deleted or deactivated, for example) when we have
     * to.
     *
     * @param lookupAuthenticatedUserAgain A boolean to indicate if we should go
     * to the database again to lookup the user to get the latest values that
     * may have been updated outside the session.
     */
    public User getUser(boolean lookupAuthenticatedUserAgain) {
        if ( user == null ) {
            user = GuestUser.get();
        }
        if (lookupAuthenticatedUserAgain && user instanceof AuthenticatedUser) {
            AuthenticatedUser auFromSession = (AuthenticatedUser) user;
            AuthenticatedUser auFreshLookup = authenticationService.findByID(auFromSession.getId());
            if (auFreshLookup == null) {
                logger.fine("getUser found user no longer exists (was deleted). Returning GuestUser.");
                user = GuestUser.get();
            } else {
                if (auFreshLookup.isDeactivated()) {
                    logger.fine("getUser found user is deactivated. Returning GuestUser.");
                    user = GuestUser.get();
                }
            }
        }
        return user;
    }

    /**
     * Sets the user and configures the session timeout.
     */
    public void setUser(User aUser) {
        // We check for deactivated status here in "setUser" to ensure a common user
        // experience across Builtin, Shib, OAuth, and OIDC users.
        // If we want a different user experience for Builtin users, we can
        // modify getUpdateAuthenticatedUser in AuthenticationServiceBean
        // (and probably other places).
        if (aUser instanceof AuthenticatedUser && aUser.isDeactivated()) {
            logger.info("Login attempt by deactivated user " + aUser.getIdentifier() + ".");
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("deactivated.error"));
            return;
        }
        FacesContext context = FacesContext.getCurrentInstance();
		// Log the login/logout and Change the session id if we're using the UI and have
		// a session, versus an API call with no session - (i.e. /admin/submitToArchive()
		// which sets the user in the session to pass it through to the underlying command)
        // TODO: reformat to remove tabs etc.
		if(context != null) {
          logSvc.log( 
                      new ActionLogRecord(ActionLogRecord.ActionType.SessionManagement,(aUser==null) ? "logout" : "login")
                          .setUserIdentifier((aUser!=null) ? aUser.getIdentifier() : (user!=null ? user.getIdentifier() : "") ));

          //#3254 - change session id when user changes
          SessionUtil.changeSessionId((HttpServletRequest) context.getExternalContext().getRequest());
            HttpSession httpSession = (HttpSession) context.getExternalContext().getSession(false);
            if (httpSession != null) {
                // Configure session timeout.
                logger.fine("jsession: " + httpSession.getId() + " setting the lifespan of the session to " + systemConfig.getLoginSessionTimeout() + " minutes");
                httpSession.setMaxInactiveInterval(systemConfig.getLoginSessionTimeout() * 60); // session timeout, in seconds
            }
        }
        this.user = aUser;
    }

    public boolean isStatusDismissed() {
        return statusDismissed;
    }
    
    public void setStatusDismissed(boolean status) {
        statusDismissed = status; //MAD: Set to true to enable code!
    }

    public Boolean getDebug() {
        // Only superusers get extra debugging information.
        if (!getUser().isSuperuser()) {
            return false;
        }
        return debug;
    }

    public void setDebug(Boolean debug) {
        // Leave the debug state alone if nothing is passed.
        if (debug != null) {
            this.debug = debug;
        }
    }

    public StaticPermissionQuery on( Dataverse d ) {
            return permissionsService.userOn(user, d);
    }
    
    // Language Locale methods: 
    
    private String localeCode;
    
    public String getLocaleCode() {
        if (localeCode == null) {
            initLocale();
        }
        return localeCode;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    public String getLocaleTitle() {
        if (localeCode == null) {
            initLocale();
        }
        return settingsWrapper.getConfiguredLocales().get(localeCode);
    }
    
    public void initLocale() {
        
        if(FacesContext.getCurrentInstance() == null) {
            localeCode = "en";
        }
        else if (FacesContext.getCurrentInstance().getViewRoot() == null ) {
            localeCode = FacesContext.getCurrentInstance().getExternalContext().getRequestLocale().getLanguage();
        }
        else if (FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage().equals("en_US")) {
            localeCode = "en";
        }
        else {
            localeCode = FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage();
        }
        
        logger.fine("init: locale set to "+localeCode);
    }

    public void updateLocaleInViewRootAndRedirect(String code) {

        localeCode = code;
        FacesContext.getCurrentInstance().getViewRoot().setLocale(new Locale(code));
        try {
            String url = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).getHeader("referer");
            FacesContext.getCurrentInstance().getExternalContext().redirect(url);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void updateLocaleInViewRoot() {
        if (localeCode != null 
                && FacesContext.getCurrentInstance() != null 
                && FacesContext.getCurrentInstance().getViewRoot() != null 
                && !localeCode.equals(FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage())) {
            FacesContext.getCurrentInstance().getViewRoot().setLocale(new Locale(localeCode));
        } 
    }
    
    public void dismissMessage(BannerMessage message){
               
        if (message.isDismissibleByUser()){
            if (user.isAuthenticated()){
                bannerMessageService.dismissMessageByUser(message, (AuthenticatedUser) user);
            }

        } else {
            dismissedMessages.add(message);
        }
        
    }

}
