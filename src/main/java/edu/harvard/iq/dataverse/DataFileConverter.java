package edu.harvard.iq.dataverse;

import java.util.logging.Logger;
import javax.ejb.EJB;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

@FacesConverter("dataFileConverter")
public class DataFileConverter implements Converter {

    private static final Logger logger = Logger.getLogger(DataFileConverter.class.getCanonicalName());

    @EJB
    DataFileServiceBean dataFileService;

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
