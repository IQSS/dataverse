package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

@ViewScoped
@Named("ApiTokenPage")
public class ApiTokenPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(ApiTokenPage.class.getCanonicalName());

    @Inject
    DataverseSession session;
    @EJB
    AuthenticationServiceBean authSvc;

    ApiToken apiToken;

    public String getApiToken() {

        if (session.getUser().isAuthenticated()) {
            AuthenticatedUser au = (AuthenticatedUser) session.getUser();
            apiToken = authSvc.findApiTokenByUser(au);
            if (apiToken != null) {
                return apiToken.getTokenString();
            } else {
                return "API token for " + au.getName() + " not found";
            }
        } else {
            return "Only authenticated users can have API tokens.";
        }

    }

    public void generate() {
        if (session.getUser().isAuthenticated()) {
            AuthenticatedUser au = (AuthenticatedUser) session.getUser();

            apiToken = authSvc.findApiTokenByUser(au);
            if (apiToken != null) {
                logger.info("An API token has already been generated for " + au.getIdentifier());
            } else {
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
}
