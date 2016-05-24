package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @todo Rename this to ApiTokenFragment? The separate page is being taken out
 * per https://github.com/IQSS/dataverse/issues/3086
 */
@ViewScoped
@Named("ApiTokenPage")
public class ApiTokenPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(ApiTokenPage.class.getCanonicalName());

    @Inject
    DataverseSession session;
    @EJB
    AuthenticationServiceBean authSvc;

    ApiToken apiToken;
    
    public boolean checkForApiToken() {
        if (session.getUser().isAuthenticated()){
            AuthenticatedUser au = (AuthenticatedUser) session.getUser();
            apiToken = authSvc.findApiTokenByUser(au);
            if (apiToken != null) {
                return true;
            }
        }
        return false;
    }

    public String getApiToken() {

        if (session.getUser().isAuthenticated()) {
            AuthenticatedUser au = (AuthenticatedUser) session.getUser();
            apiToken = authSvc.findApiTokenByUser(au);
            if (apiToken != null) {
                return apiToken.getTokenString();
            } else {
                List<String> arguments = new ArrayList<>();
                arguments.add(au.getName());
                return BundleUtil.getStringFromBundle("apitoken.notFound", arguments);
            }
        } else {
            // It should be impossible to get here from the UI.
            return "Only authenticated users can have API tokens.";
        }

    }

    public void generate() {
        if (session.getUser().isAuthenticated()) {
            AuthenticatedUser au = (AuthenticatedUser) session.getUser();

            apiToken = authSvc.findApiTokenByUser(au);
            if (apiToken != null) {
                authSvc.removeApiToken(au);
            }
            /**
             * @todo DRY! Stolen from BuiltinUsers API page
             */
            ApiToken newToken = new ApiToken();
            newToken.setTokenString(java.util.UUID.randomUUID().toString());
            newToken.setAuthenticatedUser(au);
            Calendar c = Calendar.getInstance();
            newToken.setCreateTime(new Timestamp(c.getTimeInMillis()));
            c.roll(Calendar.YEAR, 1);
            newToken.setExpireTime(new Timestamp(c.getTimeInMillis()));
            authSvc.save(newToken);
            
        }
    }
}