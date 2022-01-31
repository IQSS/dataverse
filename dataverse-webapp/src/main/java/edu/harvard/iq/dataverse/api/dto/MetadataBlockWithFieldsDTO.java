package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;

import java.util.List;


@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MetadataBlockWithFieldsDTO {
    private String displayName;

    private List<DatasetFieldDTO> fields;

    // -------------------- GETTERS --------------------

    public String getDisplayName() {
        return displayName;
    }

    public List<DatasetFieldDTO> getFields() {
        return fields;
    }

    // -------------------- LOGIC --------------------

    public void clearEmailFields() {
        fields.removeIf(DatasetFieldDTO::isEmailType);
        fields.removeIf(DatasetFieldDTO::clearEmailSubfields);
    }

    // -------------------- SETTERS --------------------

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setFields(List<DatasetFieldDTO> fields) {
        this.fields = fields;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Creator {

        // -------------------- LOGIC --------------------

        public MetadataBlockWithFieldsDTO create(MetadataBlock metadataBlock, List<DatasetField> datasetFields) {
            MetadataBlockWithFieldsDTO created = new MetadataBlockWithFieldsDTO();
            created.setDisplayName(metadataBlock.getDisplayName());
            created.setFields(new DatasetFieldDTO.Creator().create(datasetFields));
            return created;
        }
    }
}
