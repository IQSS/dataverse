package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean.StaticPermissionQuery;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.io.Serializable;
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
    
    @EJB
    SystemConfig systemConfig;
    
    private static final Logger logger = Logger.getLogger(DataverseSession.class.getCanonicalName());
    
    private boolean statusDismissed = false;
    
    public User getUser() {
        if ( user == null ) {
            user = GuestUser.get();
        }
 
        return user;
    }

    public void setUser(User aUser) {
        logSvc.log( 
                new ActionLogRecord(ActionLogRecord.ActionType.SessionManagement,(aUser==null) ? "logout" : "login")
                    .setUserIdentifier((aUser!=null) ? aUser.getIdentifier() : (user!=null ? user.getIdentifier() : "") ));
        
        this.user = aUser;
    }

    public boolean isStatusDismissed() {
        return statusDismissed;
    }
    
    public void setStatusDismissed(boolean status) {
        statusDismissed = status; //MAD: Set to true to enable code!
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
    
    public void configureSessionTimeout() {
        HttpSession httpSession = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
        
        if (httpSession != null) {
            logger.info("jsession: "+httpSession.getId()+" setting the lifespan of the session to " + systemConfig.getLoginSessionTimeout() + " minutes");
            httpSession.setMaxInactiveInterval(systemConfig.getLoginSessionTimeout() * 60); // session timeout, in seconds
        }
        
    }

}
