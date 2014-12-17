/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named
public class PermissionsWrapper implements java.io.Serializable {
    
    @EJB
    PermissionServiceBean permissionService;

    @Inject
    DataverseSession session;
    
    public boolean canManagePermissions(DvObject dvo) {
        User u = session.getUser();
        return dvo instanceof Dataverse ? 
                canManageDataversePermissions(u,  (Dataverse) dvo) : 
                canManageDatasetPermissions(u,  (Dataset) dvo);
    }
    
    public boolean canManageDatasetPermissions(User u, Dataset ds) {
        return permissionService.userOn(u, ds).has(Permission.ManageDatasetPermissions);
    }
    
    public boolean canManageDataversePermissions(User u, Dataverse dv) {
        return permissionService.userOn(u, dv).has(Permission.ManageDataversePermissions);
    } 
}
