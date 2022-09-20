package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DatasetPage;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author rmp553
 */
@ViewScoped
@Named("RolePermissionHelperPage")
public class RolePermissionHelperPage implements java.io.Serializable {
    
    private static final Logger logger = Logger.getLogger(DatasetPage.class.getCanonicalName());

    @Inject DataverseSession session;    

    @EJB
    DataverseRoleServiceBean dataverseRoleService;
    @EJB
    RoleAssigneeServiceBean roleAssigneeService;
    
    
    private DataverseRolePermissionHelper rolePermissionHelper;// = new DataverseRolePermissionHelper();
    
    
    public String init() {
       // msgt("_YE_OLDE_QUERY_COUNTER_");  // for debug purposes

        List<DataverseRole> roleList = dataverseRoleService.findAll();
        rolePermissionHelper = new DataverseRolePermissionHelper(roleList);

        return null;
    }
    
        
    public DataverseRolePermissionHelper getRolePermissionHelper(){
        return this.rolePermissionHelper;
    }
    
    private void msg(String s){
        System.out.println(s);
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }
    
}
