package edu.harvard.iq.dataverse.importers.ui;

import edu.harvard.iq.dataverse.importer.metadata.ImporterRegistry;
import edu.harvard.iq.dataverse.importer.metadata.MetadataImporter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

@FacesConverter("importerConverter")
public class ImporterConverter implements Converter {
    @Inject
    ImporterRegistry importers;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return importers.getImporterForId(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        return value == null ? "" : importers.getIdForImporter((MetadataImporter) value);
    }
}
