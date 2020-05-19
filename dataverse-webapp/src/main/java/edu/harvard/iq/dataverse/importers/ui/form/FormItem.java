package edu.harvard.iq.dataverse.importers.ui.form;

import edu.harvard.iq.dataverse.importer.metadata.ImporterData;
import edu.harvard.iq.dataverse.importer.metadata.ImporterFieldType;
import edu.harvard.iq.dataverse.importer.metadata.SafeBundleWrapper;

public class FormItem {
    private final String viewId;
    private final ImporterData.ImporterField importerField;
    private final SafeBundleWrapper bundle;

    private Object value;

    // -------------------- CONSTRUCTORS --------------------

    public FormItem(String viewId, ImporterData.ImporterField importerField, SafeBundleWrapper bundle) {
        this.viewId = viewId;
        this.importerField = importerField;
        this.bundle = bundle;
    }

    // -------------------- GETTERS --------------------

    public String getViewId() {
        return viewId;
    }

    public ImporterFieldType getType() {
        return importerField.fieldType;
    }

    public String getLabel() {
        return bundle.getString(importerField.labelKey);
    }

    public String getDescription() {
        return bundle.getString(importerField.descriptionKey);
    }

    public ImporterData.ImporterField getImporterField() {
        return importerField;
    }

    public Boolean getRequired() {
        return importerField.required;
    }

    public Object getValue() {
        return value;
    }

    public boolean isRelevantForProcessing() {
        return importerField.fieldKey.isRelevant();
    }

    // -------------------- SETTERS --------------------

    public void setValue(Object value) {
        this.value = value;
    }
}
