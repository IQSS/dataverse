package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState;

import java.util.List;
import java.util.Map;

/**
 * @author ellenk
 */
public class DatasetVersionDTO {
    String archiveNote;
    String deacessionLink;
    Long versionNumber;
    Long versionMinorNumber;
    Long id;
    VersionState versionState;
    String releaseDate;
    String releaseTime;
    String lastUpdateTime;
    String createTime;
    String archiveTime;
    String UNF;
    String citation;
    boolean inReview;

    Map<String, MetadataBlockDTO> metadataBlocks;
    List<FileDTO> files;

    public boolean isInReview() {
        return inReview;
    }

    public void setInReview(boolean inReview) {
        this.inReview = inReview;
    }

    public String getCitation() {
        return citation;
    }

    public void setCitation(String citation) {
        this.citation = citation;
    }

    public String getUNF() {
        return UNF;
    }

    public void setUNF(String UNF) {
        this.UNF = UNF;
    }

    public List<FileDTO> getFiles() {
        return files;
    }

    public void setFiles(List<FileDTO> files) {
        this.files = files;
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

    public Long getVersionMinorNumber() {
        return versionMinorNumber;
    }

    public void setVersionMinorNumber(Long versionMinorNumber) {
        this.versionMinorNumber = versionMinorNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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


    public String getReleaseTime() {
        return releaseTime;
    }

    public void setReleaseTime(String releaseTime) {
        this.releaseTime = releaseTime;
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
        return "DatasetVersionDTO{" + "archiveNote=" + archiveNote
                + ", deacessionLink=" + deacessionLink
                + ", versionNumber=" + versionNumber
                + ", versionMinorNumber=" + versionMinorNumber
                + ", id=" + id
                + ", versionState=" + versionState
                + ", releaseDate=" + releaseDate
                + ", lastUpdateTime=" + lastUpdateTime
                + ", createTime=" + createTime
                + ", archiveTime=" + archiveTime
                + ", UNF=" + UNF
                + ", metadataBlocks=" + metadataBlocks
                + ", files=" + files + '}';
    }


}
