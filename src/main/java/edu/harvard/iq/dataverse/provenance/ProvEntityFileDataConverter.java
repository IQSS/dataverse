package edu.harvard.iq.dataverse.provenance;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;

/**
 *
 * @author madunlap
 * To sort our entity objects in the provenance bundle dropdown
 */
@FacesConverter("provEntityFileDataConverter")
public class ProvEntityFileDataConverter implements Converter{

    //@Inject
    ProvPopupFragmentBean provBean = CDI.current().select(ProvPopupFragmentBean.class).get();
    
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return provBean.getEntityByEntityName(value);
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
