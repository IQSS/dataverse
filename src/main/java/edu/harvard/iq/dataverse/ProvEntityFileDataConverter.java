/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

/**
 *
 * @author madunlap
 */
@FacesConverter("provEntityFileDataConverter")
public class ProvEntityFileDataConverter implements Converter{

    @Inject
    ProvenanceUploadFragmentBean provBean;
    
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return provBean.getEntityByEntityName(value);
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null || value.equals("")) {
            return "";
        } else {
            return ((ProvEntityFileData) value).getEntityName();
        }
        
    }
}
