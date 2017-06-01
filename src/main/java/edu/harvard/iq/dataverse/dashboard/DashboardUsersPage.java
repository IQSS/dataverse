package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.api.Admin;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mydata.MyDataFilterParams;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

@ViewScoped
@Named("DashboardUsersPage")
public class DashboardUsersPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DashboardUsersPage.class.getCanonicalName());
    private AuthenticatedUser authUser = null;

    @EJB
    AuthenticationServiceBean authenticationService;

    @Inject 
    DataverseSession session;    
    @Inject
    PermissionsWrapper permissionsWrapper;        
        
    public String init() {
        
        if ((session.getUser() != null) && (session.getUser().isAuthenticated())&& (session.getUser().isSuperuser())) {
            authUser = (AuthenticatedUser) session.getUser();
        } else {
            return permissionsWrapper.notAuthorized();
	// redirect to login OR give some type ‘you must be logged in message'
        }

        return null;
    }
    public String getListUsersAPIPath(){
        //return "ok";
        return Admin.listUsersFullAPIPath;
    }
}
