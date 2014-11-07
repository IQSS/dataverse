package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

@ViewScoped
@Named("ApiTokenPage")
public class ApiTokenPage {

    @Inject
    DataverseSession session;
    @EJB
    AuthenticationServiceBean authSvc;

    public String getApiToken() {

        if (session.getUser().isAuthenticated()) {
            AuthenticatedUser au = (AuthenticatedUser) session.getUser();
            ApiToken apiToken = authSvc.findApiTokenStringByUser(au);
            if (apiToken != null) {
                return apiToken.getTokenString();
            } else {
                return "API token for " + au.getName() + " not found";
            }
        } else {
            return "Only authenticated users can have API tokens.";
        }

    }
}
