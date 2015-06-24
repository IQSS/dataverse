/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author skraffmi
 */
@Stateless
public class MyDataQueryHelperServiceBean {
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public Query getDirectQuery(String dtype, ArrayList<DataverseRole> roles, AuthenticatedUser user) {
        String roleString = getRolesClause(roles);
        System.out.print("SELECT id FROM dvobject WHERE "
                + " dtype = '" + dtype + "'"
                + " and id in (select definitionpoint_id from roleassignment where assigneeidentifier in ('" + user.getIdentifier() + "') "
                + roleString + ");");
        return em.createNativeQuery("SELECT id FROM dvobject WHERE "
                + " dtype = '" + dtype + "'"
                + " and id in (select definitionpoint_id from roleassignment where assigneeidentifier in ('" + user.getIdentifier() + "') "
                + roleString + ");");
    }
    
    public Query getIndirectQuery(String indirectParentIds) {
        return em.createNativeQuery("SELECT id FROM dvobject WHERE "
                + " owner_id in (" + indirectParentIds + ");");
    }
    
    private String getRolesClause(List<DataverseRole> roles) {
        String roleString = "";
        if (roles != null && !roles.isEmpty()) {
            roleString = " and role_id in (";
            boolean first = true;
            for (DataverseRole role : roles) {
                if (!first) {
                    roleString += ",";
                }
                roleString += role.getId();
                first = false;
            }
            roleString += ")";
        }
        return roleString;
    }
    
}
