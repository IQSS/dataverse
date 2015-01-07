package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ellenk
 */
public class DatasetVersionDTO {
    String archiveNote;
    String deacessionLink;
    Long versionNumber;
    String minorVersionNumber;
    long id;
    VersionState versionState;
    String releaseDate;
    String lastUpdateTime;
    String createTime;
    String archiveTime;
    String UNF;
    
    Map<String,MetadataBlockDTO> metadataBlocks;
    List<FileMetadataDTO> fileMetadatas;

    public String getUNF() {
        return UNF;
    }

    public void setUNF(String UNF) {
        this.UNF = UNF;
    }

    public List<FileMetadataDTO> getFileMetadatas() {
        return fileMetadatas;
    }

    public void setFileMetadatas(List<FileMetadataDTO> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
    }

    
    
    public String getArchiveNote() {
        return archiveNote;
    }

    public void setArchiveNote(String archiveNote) {
        this.archiveNote = archiveNote;
    }

    public String getDeacessionLink() {
        return deacessionLink;
    }

    public void setDeacessionLink(String deacessionLink) {
        this.deacessionLink = deacessionLink;
    }

    public Long getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Long versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getMinorVersionNumber() {
        return minorVersionNumber;
    }

    public void setMinorVersionNumber(String minorVersionNumber) {
        this.minorVersionNumber = minorVersionNumber;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    

    public VersionState getVersionState() {
        return versionState;
    }

    public void setVersionState(VersionState versionState) {
        this.versionState = versionState;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(String lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getArchiveTime() {
        return archiveTime;
    }

    public void setArchiveTime(String archiveTime) {
        this.archiveTime = archiveTime;
    }

    public Map<String, MetadataBlockDTO> getMetadataBlocks() {
        return metadataBlocks;
    }

    public void setMetadataBlocks(Map<String, MetadataBlockDTO> metadataBlocks) {
        this.metadataBlocks = metadataBlocks;
    }

    @Override
    public String toString() {
        return "DatasetVersionDTO{" + "archiveNote=" + archiveNote + ", deacessionLink=" + deacessionLink + ", versionNumber=" + versionNumber + ", minorVersionNumber=" + minorVersionNumber + ", id=" + id + ", versionState=" + versionState + ", releaseDate=" + releaseDate + ", lastUpdateTime=" + lastUpdateTime + ", createTime=" + createTime + ", archiveTime=" + archiveTime + ", UNF=" + UNF + ", metadataBlocks=" + metadataBlocks + ", fileMetadatas=" + fileMetadatas + '}';
    }
    
    
    
     
     
    
     
    
 
}
