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
 * @author skraffmi
 */
@FacesConverter("datasetVersionConverter")
public class DatasetVersionConverter implements Converter {
    
    //@EJB
    DatasetVersionServiceBean datasetVersionService = CDI.current().select(DatasetVersionServiceBean.class).get();
    
    
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
                if (value == null || value.equals("")) {
            return "";
        } else {                  
            return datasetVersionService.find(new Long(value));
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
                if (value == null || value.equals("")) {
            return "";
        } else {
            String stringToReturn = ((DatasetVersion) value).getId().toString();
            return stringToReturn;
        }
    }
    
}
