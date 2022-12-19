package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.search.dataverselookup.DataverseLookupService;
import edu.harvard.iq.dataverse.search.dataverselookup.LookupData;
import org.apache.commons.lang3.StringUtils;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

@FacesConverter("dataverseLookupConverter")
public class DataverseLookupConverter implements Converter {

    private DataverseLookupService dataverseLookupService;

    // -------------------- CONSTRUCTORS --------------------

    public DataverseLookupConverter() { }

    @Inject
    public DataverseLookupConverter(DataverseLookupService dataverseLookupService) {
        this.dataverseLookupService = dataverseLookupService;
    }

    // -------------------- LOGIC --------------------

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return dataverseLookupService.findDataverseByName(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        return value == null ? StringUtils.EMPTY : ((LookupData) value).getName();
    }
}
