package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import java.util.LinkedList;
import java.util.List;
import javax.ejb.EJB;
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
    
    public enum EditMode {LOGIN, SUCCESS, FAILED};
    
    @Inject DataverseSession session;    
    
    @EJB
    BuiltinUserServiceBean dataverseUserService;
    
    @EJB
    UserServiceBean userService;
    
    @EJB
    AuthenticationServiceBean authSvc;

    private String authProviderId;
    
    public void init() {
        
        setAuthProviderId(authSvc.getAuthenticationProviderIds().iterator().next());
        
    }
    
    public List<AuthenticationProviderDisplayInfo> listAuthenticationProviders() {
        List<AuthenticationProviderDisplayInfo> infos = new LinkedList<>();
        for ( String id : authSvc.getAuthenticationProviderIds() ) {
            infos.add( authSvc.getAuthenticationProvider(id).getInfo());
        }
        return infos;
    }
   
    public AuthenticationProvider selectedProvider() {
        return authSvc.getAuthenticationProvider(getAuthProviderId());
    }
    
    public boolean validatePassword(String username, String password) {
        return false;
    }

    public String login() {
       return "";
    }

    public String getAuthProviderId() {
        return authProviderId;
    }

    public void setAuthProviderId(String authProviderId) {
        this.authProviderId = authProviderId;
    }

}
