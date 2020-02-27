/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import javax.ejb.EJB;
import javax.enterprise.inject.spi.CDI;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author xyang
 */
@FacesConverter("facetConverter")
public class FacetConverter implements Converter {

    //@EJB
    DatasetFieldServiceBean datasetFieldService = CDI.current().select(DatasetFieldServiceBean.class).get();

    public Object getAsObject(FacesContext facesContext, UIComponent component, String submittedValue) {
        return datasetFieldService.find(new Long(submittedValue));
    }

    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        if (value == null || value.equals("")) {
            return "";
        } else {
            return ((DatasetFieldType) value).getId().toString();
        }
    }
}

