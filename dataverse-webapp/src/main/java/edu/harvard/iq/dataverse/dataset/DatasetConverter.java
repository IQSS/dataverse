package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import org.apache.commons.lang.StringUtils;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

@FacesConverter("datasetConverter")
public class DatasetConverter implements Converter {

    @Inject
    DatasetDao datasetDao;

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String submittedValue) {
        return datasetDao.find(new Long(submittedValue));
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        return value != null && !"".equals(value)
                ? ((Dataset) value).getId().toString()
                : StringUtils.EMPTY;
    }
}
