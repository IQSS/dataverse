package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

@ViewScoped
@Named
public class DashboardUsersPage implements java.io.Serializable {

    @EJB
    AuthenticationServiceBean authenticationService;

    public List<AuthenticatedUser> getAuthenticatedUsers() {
        return authenticationService.findAllAuthenticatedUsers();
    }
}
