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
        private String peristentUrl;
        private String protocol;
        private String authority;
        private String globalIdCreateTime;
        private List<DatasetVersionDTO> datasetVersions;
        private List<DataFileDTO> dataFiles;

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

    public String getPeristentUrl() {
        return peristentUrl;
    }

    public void setPeristentUrl(String peristentUrl) {
        this.peristentUrl = peristentUrl;
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

    public String getGlobalIdCreateTime() {
        return globalIdCreateTime;
    }

    public void setGlobalIdCreateTime(String globalIdCreateTime) {
        this.globalIdCreateTime = globalIdCreateTime;
    }

    public List<DatasetVersionDTO> getDatasetVersions() {
        return datasetVersions;
    }
    public DatasetVersionDTO getFirstVersion() {
        return datasetVersions.get(0);
    }

    public void setDatasetVersions(List<DatasetVersionDTO> datasetVersions) {
        this.datasetVersions = datasetVersions;
    }

    public List<DataFileDTO> getDataFiles() {
        return dataFiles;
    }

    public void setDataFiles(List<DataFileDTO> dataFiles) {
        this.dataFiles = dataFiles;
    }

    @Override
    public String toString() {
        return "DatasetDTO{" + "id=" + id + ", identifier=" + identifier + ", peristentUrl=" + peristentUrl + ", protocol=" + protocol + ", authority=" + authority + ", globalIdCreateTime=" + globalIdCreateTime + ", datasetVersions=" + datasetVersions + ", dataFiles=" + dataFiles + '}';
    }
        
}
