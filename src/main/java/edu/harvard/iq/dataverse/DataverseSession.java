package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean.PermissionQuery;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.authorization.users.UserRequestMetadata;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author gdurand
 */
@Named
@SessionScoped
public class DataverseSession implements Serializable{
    
	private User user;
	
	@EJB
	PermissionServiceBean permissionsService;
	
	@EJB
	BuiltinUserServiceBean usersSvc;
	
    @EJB 
    ActionLogServiceBean logSvc;
    
    public User getUser() {
        if ( user == null ) {
            user = new GuestUser();
        }
        
        if (FacesContext.getCurrentInstance() != null) {
            user.setRequestMetadata( new UserRequestMetadata((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()) );
        }
        return user;
    }

    public void setUser(AuthenticatedUser aUser) {
        logSvc.log( 
                new ActionLogRecord(ActionLogRecord.ActionType.SessionManagement,(aUser==null) ? "logout" : "login")
                    .setUserIdentifier((aUser!=null) ? aUser.getIdentifier() : (user!=null ? user.getIdentifier() : "") ));
        
        this.user = aUser;
    }

	public PermissionQuery on( Dataverse d ) {
		return permissionsService.userOn(user, d);
	}

}
