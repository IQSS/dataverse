package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.Dataset;
import java.util.List;

/**
 * Data transfer object for {@link Dataset}.
 * @author michael
 */
public class DatasetDTO implements java.io.Serializable {
	private String id;
        private String identifier;
        private String protocol;
        private String authority;
        private String separator;
        private String globalIdCreateTime;
        private String publisher;
        private String publicationDate;
        private String metadataLanguage;
        private DatasetVersionDTO datasetVersion;
        private List<DataFileDTO> dataFiles;
    private Integer datasetFileCountLimit;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public String getGlobalIdCreateTime() {
        return globalIdCreateTime;
    }

    public void setGlobalIdCreateTime(String globalIdCreateTime) {
        this.globalIdCreateTime = globalIdCreateTime;
    }

    public DatasetVersionDTO getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersionDTO datasetVersion) {
        this.datasetVersion = datasetVersion;
    }


    public List<DataFileDTO> getDataFiles() {
        return dataFiles;
    }

    public void setDataFiles(List<DataFileDTO> dataFiles) {
        this.dataFiles = dataFiles;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    @Override
    public String toString() {
        return "DatasetDTO{" + "id=" + id + ", identifier=" + identifier + ", protocol=" + protocol + ", authority=" + authority + ", separator=" + separator + ", globalIdCreateTime=" + globalIdCreateTime + ", datasetVersion=" + datasetVersion + ", dataFiles=" + dataFiles + '}';
    }

    public void setMetadataLanguage(String metadataLanguage) {
        this.metadataLanguage = metadataLanguage;
    }
    
    public String getMetadataLanguage() {
        return metadataLanguage;
    }

    public Integer getDatasetFileCountLimit() { return datasetFileCountLimit; }

    public void setDatasetFileCountLimit(Integer datasetFileCountLimit) {
        this.datasetFileCountLimit = datasetFileCountLimit;
    }
}
