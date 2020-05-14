package edu.harvard.iq.dataverse;

import org.apache.commons.lang.StringUtils;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

@FacesConverter(value = "strToLongConverter")
public class StringToLongParamConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        return value == null ? StringUtils.EMPTY : String.valueOf(value);
    }
}
