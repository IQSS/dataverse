package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean.PermissionQuery;
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
	
    public User getUser() {
        if ( user == null ) {
            user = new GuestUser();
        }
        user.setRequestMetadata( new UserRequestMetadata((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()) );
        return user;
    }

    public void setUser(AuthenticatedUser user) {
        this.user = user;
    }

	public PermissionQuery on( Dataverse d ) {
		return permissionsService.userOn(user, d);
	}

}
