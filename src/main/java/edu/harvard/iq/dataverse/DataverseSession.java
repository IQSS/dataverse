package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.PermissionServiceBean.PermissionQuery;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.GuestUser;
import edu.harvard.iq.dataverse.authorization.User;
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
    
	private AuthenticatedUser user;
	
	@EJB
	PermissionServiceBean permissionsService;
	
	@EJB
	DataverseUserServiceBean usersSvc;
	
    public User getUser() {
        return ( user==null ) ? GuestUser.get(): user;
    }

    public void setUser(AuthenticatedUser user) {
        this.user = user;
    }

	public PermissionQuery on( Dataverse d ) {
		return permissionsService.userOn(user, d);
	}

}
