package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ellenk
 */
public class DatasetVersionDTO {
    String deacessionLink;
    // FIXME: Change to versionNumberMajor and versionNumberMinor? Some partial renaming of "minor" was done.
    Long versionNumber;
    Long versionMinorNumber;
    long id;
    VersionState versionState;
    String releaseDate;
    String releaseTime;
    String lastUpdateTime;
    String createTime;
    String archiveTime;
    String UNF;
    String termsOfUse;
    String termsOfAccess;
    String confidentialityDeclaration;
    String specialPermissions;
    String restrictions;
    String citationRequirements;
    String depositorRequirements;
    String conditions;
    String disclaimer;
    String dataAccessPlace;
    String originalArchive;
    String availabilityStatus;
    String contactForAccess;
    String sizeOfCollection;
    String studyCompletion;
    boolean fileAccessRequest;
    String citation;
    LicenseDTO license;
    boolean inReview;
    
    Map<String,MetadataBlockDTO> metadataBlocks;
    List<FileMetadataDTO> fileMetadatas;
    List<FileDTO> files;

    String versionNote;
    
    public boolean isInReview() {
        return inReview;
    }

    public void setInReview(boolean inReview) {
        this.inReview = inReview;
    }

    public String getTermsOfUse() {
        return termsOfUse;
    }

    public void setTermsOfUse(String termsOfUse) {
        this.termsOfUse = termsOfUse;
    }

    public String getTermsOfAccess() {
        return termsOfAccess;
    }

    public void setTermsOfAccess(String termsOfAccess) {
        this.termsOfAccess = termsOfAccess;
    }

    public String getConfidentialityDeclaration() {
        return confidentialityDeclaration;
    }

    public void setConfidentialityDeclaration(String confidentialityDeclaration) {
        this.confidentialityDeclaration = confidentialityDeclaration;
    }

    public String getSpecialPermissions() {
        return specialPermissions;
    }

    public void setSpecialPermissions(String specialPermissions) {
        this.specialPermissions = specialPermissions;
    }

    public String getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(String restrictions) {
        this.restrictions = restrictions;
    }

    public String getCitationRequirements() {
        return citationRequirements;
    }

    public void setCitationRequirements(String citationRequirements) {
        this.citationRequirements = citationRequirements;
    }

    public String getDepositorRequirements() {
        return depositorRequirements;
    }

    public void setDepositorRequirements(String depositorRequirements) {
        this.depositorRequirements = depositorRequirements;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public String getDataAccessPlace() {
        return dataAccessPlace;
    }

    public void setDataAccessPlace(String dataAccessPlace) {
        this.dataAccessPlace = dataAccessPlace;
    }

    public String getOriginalArchive() {
        return originalArchive;
    }

    public void setOriginalArchive(String originalArchive) {
        this.originalArchive = originalArchive;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public String getContactForAccess() {
        return contactForAccess;
    }

    public void setContactForAccess(String contactForAccess) {
        this.contactForAccess = contactForAccess;
    }

    public String getSizeOfCollection() {
        return sizeOfCollection;
    }

    public void setSizeOfCollection(String sizeOfCollection) {
        this.sizeOfCollection = sizeOfCollection;
    }

    public String getStudyCompletion() {
        return studyCompletion;
    }

    public void setStudyCompletion(String studyCompletion) {
        this.studyCompletion = studyCompletion;
    }
    
    public boolean isFileAccessRequest() {
    	return fileAccessRequest;
    }
    
    public void setFileAccessRequest(boolean fileAccessRequest) {
    	this.fileAccessRequest = fileAccessRequest;
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

    public List<FileMetadataDTO> getFileMetadatas() {
        return fileMetadatas;
    }

    public void setFileMetadatas(List<FileMetadataDTO> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
    }

    public List<FileDTO> getFiles() {
        return files;
    }

    public void setFiles(List<FileDTO> files) {
        this.files = files;
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

    public Long getMinorVersionNumber() {
        return versionMinorNumber;
    }

    public void setMinorVersionNumber(Long minorVersionNumber) {
        this.versionMinorNumber = minorVersionNumber;
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

    public LicenseDTO getLicense() {
        return license;
    }

    public void setLicense(LicenseDTO license) {
        this.license = license;
    }

    public Map<String, MetadataBlockDTO> getMetadataBlocks() {
        return metadataBlocks;
    }

    public void setMetadataBlocks(Map<String, MetadataBlockDTO> metadataBlocks) {
        this.metadataBlocks = metadataBlocks;
    }
    
    public List<FieldDTO> getDatasetFields() {
        List<FieldDTO> fields = new ArrayList<>();
        //TODO: finish this
        return null;
    }

    public String getVersionNote() {
        return versionNote;
    }

    public void setVersionNote(String versionNote) {
        this.versionNote = versionNote;
    }

    @Override
    public String toString() {
        return "DatasetVersionDTO{deacessionLink=" + deacessionLink + ", versionNumber=" + versionNumber + ", minorVersionNumber=" + versionMinorNumber + ", id=" + id + ", versionState=" + versionState + ", releaseDate=" + releaseDate + ", lastUpdateTime=" + lastUpdateTime + ", createTime=" + createTime + ", archiveTime=" + archiveTime + ", UNF=" + UNF + ", metadataBlocks=" + metadataBlocks + ", fileMetadatas=" + fileMetadatas + '}';
    }

}
