/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author gdurand
 */
@FacesConverter("roleAssigneeConverter")
public class RoleAssigneeConverter implements Converter {
    
    @EJB
    RoleAssigneeServiceBean roleAssigneeService;

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
