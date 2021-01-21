package edu.harvard.iq.dataverse.api.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author ellenk
 */
public class MetadataBlockDTO {
    String displayName;
    List<FieldDTO> fields = new ArrayList<FieldDTO>();

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public FieldDTO getField(String typeName) {

        for (FieldDTO field : fields) {
            if (field.getTypeName().equals(typeName)) {
                return field;
            }
        }
        return null;
    }

    public void addField(FieldDTO newField) {
        FieldDTO current = getField(newField.getTypeName());
        // If there is no Field in this Metadatablock with the typeName,
        // then add it to the list
        if (current == null) {
            fields.add(newField);
            // Else, add/replace the value in the new field to the current field
        } else {
            if (current.getMultiple()) {
                if ("compound".equals(newField.getTypeClass())) {
                    List<Set<FieldDTO>> currentValue = current.getMultipleCompound();
                    currentValue.addAll(newField.getMultipleCompound());
                    current.setMultipleCompound(currentValue);
                } else if ("controlledVocabulary".equals(newField.getTypeClass())) {
                    List<String> currentValue = current.getMultipleVocab();
                    currentValue.addAll(newField.getMultipleVocab());
                    current.setMultipleVocab(currentValue);
                } else {
                    List<String> currentValue = current.getMultiplePrimitive();
                    currentValue.addAll(newField.getMultiplePrimitive());
                    current.setMultiplePrimitive(currentValue);
                }
            } else {
                // If this Field doesn't allow multiples, just replace the value
                // with the new field value.
                current.setValue(newField.getValue());
            }
        }
    }

    public List<FieldDTO> getFields() {
        return fields;
    }

    public void setFields(List<FieldDTO> fields) {
        this.fields = fields;
    }

    @Override
    public String toString() {
        return "MetadataBlockDTO{" + "displayName=" + displayName + ", fields=" + fields + '}';
    }
}
