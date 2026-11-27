package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@ViewScoped
@Named("ControlledVocabularyAutocompleteBean")
public class ControlledVocabularyAutocompleteBean implements Serializable {

    private final Set<Long> slowModeFieldTypeIds = new HashSet<>();

    public boolean isSlowMode(Long fieldTypeId) {
        return fieldTypeId != null && slowModeFieldTypeIds.contains(fieldTypeId);
    }

    public void switchToSlowMode(Long fieldTypeId) {
        if (fieldTypeId != null) {
            slowModeFieldTypeIds.add(fieldTypeId);
        }
    }

    public List<ControlledVocabularyValue> complete(String query) {
        UIComponent component = UIComponent.getCurrentComponent(FacesContext.getCurrentInstance());
        DatasetField dsf = (DatasetField) component.getAttributes().get("dsf");
        DatasetFieldType dsft = (DatasetFieldType) component.getAttributes().get("dsft");

        if (dsf != null) {
            dsft = dsf.getDatasetFieldType();
        }

        if (dsft == null || dsft.getControlledVocabularyValues() == null || query == null) {
            return Collections.emptyList();
        }

        List<ControlledVocabularyValue> results = new ArrayList<>();
        String queryLower = query.toLowerCase();
        String mdLangCode = null;

        if (dsf != null && dsf.getDatasetVersion() != null && dsf.getDatasetVersion().getDataset() != null) {
            mdLangCode = dsf.getDatasetVersion().getDataset().getMetadataLanguage();
        }

        for (ControlledVocabularyValue cvv : dsft.getControlledVocabularyValues()) {
            String localeStrValue = cvv.getLocaleStrValue(mdLangCode);
            if (localeStrValue != null && localeStrValue.toLowerCase().contains(queryLower)) {
                results.add(cvv);
            }
            if (results.size() >= 101) {
                break;
            }
        }
        return results;
    }
}
