/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import jakarta.ejb.EJB;
import jakarta.enterprise.inject.spi.CDI;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

/**
 *
 * @author xyang
 */
@FacesConverter("metadataBlockConverter")
public class MetadataBlockConverter implements Converter {

    //@EJB
    DataverseServiceBean dataverseService = CDI.current().select(DataverseServiceBean.class).get();

    public Object getAsObject(FacesContext facesContext, UIComponent component, String submittedValue) {
        MetadataBlock mdb = dataverseService.findMDB(new Long(submittedValue));
        return mdb;
    }

    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        if (value == null || value.equals("")) {
            return "";
        } else {
            return ((MetadataBlock) value).getId().toString();
        }
    }
}
