package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;

import java.util.Map;
import java.util.stream.Collectors;


@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MetadataBlockDTO {
    private Long id;
    private String name;
    private String displayName;
    private Map<String, DatasetFieldTypeDTO> fields;

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Map<String, DatasetFieldTypeDTO> getFields() {
        return fields;
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setFields(Map<String, DatasetFieldTypeDTO> fields) {
        this.fields = fields;
    }

    // -------------------- INNER CLASSES --------------------

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class DatasetFieldTypeDTO {
        private String name;
        private String displayName;
        private String title;
        private String type;
        private String watermark;
        private String description;
        private Map<String, DatasetFieldTypeDTO> childFields;

        // -------------------- GETTERS --------------------

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getTitle() {
            return title;
        }

        public String getType() {
            return type;
        }

        public String getWatermark() {
            return watermark;
        }

        public String getDescription() {
            return description;
        }

        public Map<String, DatasetFieldTypeDTO> getChildFields() {
            return childFields;
        }

        // -------------------- SETTERS --------------------

        public void setName(String name) {
            this.name = name;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setWatermark(String watermark) {
            this.watermark = watermark;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setChildFields(Map<String, DatasetFieldTypeDTO> childFields) {
            this.childFields = childFields;
        }
    }

    public static class Converter {

        // -------------------- LOGIC --------------------

        public MetadataBlockDTO convert(MetadataBlock metadataBlock) {
            MetadataBlockDTO converted = convertMinimal(metadataBlock);
            converted.setFields(metadataBlock.getDatasetFieldTypes().stream()
                    .map(this::convertDatasetFieldType)
                    .collect(Collectors.toMap(DatasetFieldTypeDTO::getName, t -> t)));
            return converted;
        }

        public MetadataBlockDTO convertMinimal(MetadataBlock metadataBlock) {
            MetadataBlockDTO converted = new MetadataBlockDTO();
            converted.setId(metadataBlock.getId());
            converted.setName(metadataBlock.getName());
            converted.setDisplayName(metadataBlock.getDisplayName());
            return converted;
        }

        // -------------------- PRIVATE --------------------

        private DatasetFieldTypeDTO convertDatasetFieldType(DatasetFieldType datasetFieldType) {
            DatasetFieldTypeDTO converted = new DatasetFieldTypeDTO();
            converted.setName(datasetFieldType.getName());
            converted.setDisplayName(datasetFieldType.getDisplayName());
            converted.setTitle(datasetFieldType.getTitle());
            converted.setType(datasetFieldType.getFieldType().toString());
            converted.setWatermark(datasetFieldType.getWatermark());
            converted.setDescription(datasetFieldType.getDescription());
            if (datasetFieldType.getChildDatasetFieldTypes().isEmpty()) {
                return converted;
            }
            converted.setChildFields(datasetFieldType.getChildDatasetFieldTypes().stream()
                    .map(this::convertDatasetFieldType)
                    .collect(Collectors.toMap(DatasetFieldTypeDTO::getName, t -> t)));
            return converted;
        }
    }
}
