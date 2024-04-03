package edu.harvard.iq.dataverse;

import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.enterprise.inject.spi.CDI;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

@FacesConverter("dataFileConverter")
public class DataFileConverter implements Converter {

    private static final Logger logger = Logger.getLogger(DataFileConverter.class.getCanonicalName());

    //@EJB
    DataFileServiceBean dataFileService = CDI.current().select(DataFileServiceBean.class).get();

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String submittedValue) {
        if (submittedValue == null || submittedValue.equals("")) {
            return "";
        } else {
            return dataFileService.find(new Long(submittedValue));
        }
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        if (value == null || value.equals("")) {
            return "";
        } else {
            String stringToReturn = ((DataFile) value).getId().toString();
            logger.fine("stringToReturn: " + stringToReturn);
            return stringToReturn;
        }
    }
}
