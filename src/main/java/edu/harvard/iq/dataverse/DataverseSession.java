/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.PermissionServiceBean.PermissionQuery;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
//import javax.faces.bean.SessionScoped;
import javax.inject.Named;

/**
 *
 * @author gdurand
 */
@Named
@SessionScoped
public class DataverseSession implements Serializable{
    
	private DataverseUser user;
	
	@EJB
	PermissionServiceBean permissionsService;

    public DataverseUser getUser() {
        return user;
    }

    public void setUser(DataverseUser user) {
        this.user = user;
    }

	public PermissionQuery on( Dataverse d ) {
		return permissionsService.userOn(user, d);
	}

}
