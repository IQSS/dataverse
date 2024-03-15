/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import jakarta.ejb.EJB;
import jakarta.enterprise.inject.spi.CDI;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

/**
 *
 * @author gdurand
 */
@FacesConverter("roleAssigneeConverter")
public class RoleAssigneeConverter implements Converter {
    
    //@EJB
    RoleAssigneeServiceBean roleAssigneeService = CDI.current().select(RoleAssigneeServiceBean.class).get();

    public Object getAsObject(FacesContext facesContext, UIComponent component, String submittedValue) {
        RoleAssignee mdb = roleAssigneeService.getRoleAssignee(submittedValue);
        return mdb;
    }

    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        if (value == null || value.equals("")) {
            return "";
        } else {
            return ((RoleAssignee) value).getIdentifier();
        }
    }
}
