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

@FacesConverter("datasetConverter")
public class DatasetConverter implements Converter {

    //@EJB
    DatasetServiceBean datasetService = CDI.current().select(DatasetServiceBean.class).get();

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String submittedValue) {
        return datasetService.find(new Long(submittedValue));
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        if (value == null || value.equals("")) {
            return "";
        } else {
            return ((Dataset) value).getId().toString();
        }
    }
}
