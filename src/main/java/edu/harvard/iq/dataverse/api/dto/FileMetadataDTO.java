package edu.harvard.iq.dataverse.api.dto;

/**
 *
 * @author ellenk
 */
public class FileMetadataDTO {
    String label;
    String description;
    String category;
    DataFileDTO dataFile;

    public DataFileDTO getDataFile() {
        return dataFile;
    }

    public void setDataFile(DataFileDTO dataFile) {
        this.dataFile = dataFile;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
   
}
