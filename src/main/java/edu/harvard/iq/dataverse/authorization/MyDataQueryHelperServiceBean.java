/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
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

    @EJB
    DataverseRoleServiceBean roleService;

    public Query getDirectQuery(String dtype, ArrayList<DataverseRole> roles, AuthenticatedUser user) {
        String roleString = getRolesClause(roles);
        return em.createNativeQuery("SELECT id FROM dvobject WHERE "
                + " dtype = '" + dtype + "'"
                + " and id in (select definitionpoint_id from roleassignment where assigneeidentifier in ('" + user.getIdentifier() + "') "
                + roleString + ");");
    }

    public List<Long> getParentIds(String dtypeParent, String dtypeChild, ArrayList<DataverseRole> roles, AuthenticatedUser user) {
        String roleString = getRolesClause(roles);
        List<Long> retVal = new ArrayList();
        List<Object[]> results = em.createNativeQuery("Select role.role_id, dvo.id FROM dvobject dvo, roleassignment role WHERE  "
                + " dtype = '" + dtypeParent + "'"
                + " and role.id in (select id from roleassignment where assigneeidentifier in ('" + user.getIdentifier() + "') "
                + roleString + ");").getResultList();

        for (Object[] result : results) {
            Long role_id = (Long) result[0];
            DataverseRole role = roleService.find(role_id);

            if (dtypeParent.equals("Dataverse") && dtypeChild.equals("Dataset") && doesRoleApply(role, "Dataset")) {
                Integer r1 = (Integer) result[1];
                retVal.add(r1.longValue());
            }

            if (dtypeParent.equals("Dataverse") && dtypeChild.equals("DataFile") && doesRoleApply(role, "DataFile")) {
                List<Object> dsIds = em.createNativeQuery("Select id from dvobject where owner_id = " + (Integer) result[1] + ";").getResultList();
                for (Object dsId : dsIds) {
                    Integer r1 = (Integer) dsId;
                    retVal.add(r1.longValue());
                }
            }

            if (dtypeParent.equals("Dataset")) {
                if (doesRoleApply(role, "DataFile")) {
                    Integer r1 = (Integer) result[1];
                    retVal.add(r1.longValue());
                }
            }
        }
        return retVal;
    }

    private Boolean doesRoleApply(DataverseRole role, String dvObjectType) {
        Boolean retVal = false;
        if (dvObjectType != null) {
            switch (dvObjectType) {
                case "Dataset":
                    for (Permission permission : role.permissions()) {
                        if (permission.appliesTo(Dataset.class)) {
                            retVal = true;
                            break;
                        }
                    }
                    break;
                case "DataFile":
                    if (role.permissions().contains(Permission.DownloadFile)) {
                        retVal = true;
                    }
                    break;
            }
        }
        return retVal;
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
