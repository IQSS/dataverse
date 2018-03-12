package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean.StaticPermissionQuery;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

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

	public StaticPermissionQuery on( Dataverse d ) {
		return permissionsService.userOn(user, d);
	}

}
