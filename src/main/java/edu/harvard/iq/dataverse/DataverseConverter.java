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

import java.util.logging.Logger;

/**
 *
 * @author skraffmiller
 */
@FacesConverter("dataverseConverter")
public class DataverseConverter implements Converter {
    private static final Logger logger = Logger.getLogger(DatasetPage.class.getCanonicalName());

    
    //@EJB
    DataverseServiceBean dataverseService = CDI.current().select(DataverseServiceBean.class).get();

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String submittedValue) {
        if (submittedValue == null || !submittedValue.matches("[0-9]+")) {
            logger.fine("Submitted value is not a host dataverse number but: " + submittedValue);
            return CDI.current().select(DatasetPage.class).get().getSelectedHostDataverse();
        }
        else {
            return dataverseService.find(Long.valueOf(submittedValue));
        }
        //return dataverseService.findByAlias(submittedValue);
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        if (value == null || value.equals("")) {
            return "";
        } else {
            return ((Dataverse) value).getId().toString();
            //return ((Dataverse) value).getAlias();
        }
    }
}
