package edu.harvard.iq.dataverse.util;

import java.io.Serializable;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;
import javax.persistence.Converter;
import org.apache.commons.lang.StringUtils;

/**
 *This class just trims the trailing and leading white spaces from input fields. 
 * We just have to add converter="#{StringTrimConverter}" to use this
 */
@FacesConverter(forClass = String.class)
public class StringTrimConverter implements Serializable, javax.faces.convert.Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return value != null ? value.trim() : null;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        return (String) value;
    }

}
