package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.makedatacount.DatasetExternalCitations;
import edu.harvard.iq.dataverse.makedatacount.DatasetMetrics;
import edu.harvard.iq.dataverse.settings.FeatureFlags;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.storageuse.StorageUse;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;

/**
 *
 * @author skraffmiller
 */
@NamedQueries({
    // Dataset.findById should only be used if you're going to iterate over files (otherwise, lazy loading in DatasetService.find() is better).
    // If you are going to iterate over files, preferably call the DatasetService.findDeep() method i.s.o. using this query directly.
    @NamedQuery(name = "Dataset.findById", 
                query = "SELECT o FROM Dataset o LEFT JOIN FETCH o.files WHERE o.id=:id"),
    @NamedQuery(name = "Dataset.findIdStale",
               query = "SELECT d.id FROM Dataset d WHERE d.indexTime is NULL OR d.indexTime < d.modificationTime"),
    @NamedQuery(name = "Dataset.findIdStalePermission",
               query = "SELECT d.id FROM Dataset d WHERE d.permissionIndexTime is NULL OR d.permissionIndexTime < d.permissionModificationTime"),
    @NamedQuery(name = "Dataset.findByIdentifier",
               query = "SELECT d FROM Dataset d WHERE d.identifier=:identifier"),
    @NamedQuery(name = "Dataset.findByIdentifierAuthorityProtocol",
               query = "SELECT d FROM Dataset d WHERE d.identifier=:identifier AND d.protocol=:protocol AND d.authority=:authority"),
    @NamedQuery(name = "Dataset.findIdentifierByOwnerId", 
                query = "SELECT o.identifier FROM Dataset o WHERE o.owner.id=:ownerId"),
    @NamedQuery(name = "Dataset.findIdByOwnerId", 
                query = "SELECT o.id FROM Dataset o WHERE o.owner.id=:ownerId"),
    @NamedQuery(name = "Dataset.findByOwnerId", 
                query = "SELECT o FROM Dataset o WHERE o.owner.id=:ownerId"),
    @NamedQuery(name = "Dataset.findByCreatorId",
                query = "SELECT o FROM Dataset o WHERE o.creator.id=:creatorId"),
    @NamedQuery(name = "Dataset.findByReleaseUserId",
                query = "SELECT o FROM Dataset o WHERE o.releaseUser.id=:releaseUserId"),
})

/*
    Below is the database stored procedure for getting a string dataset id.
    Used when the Dataverse is (optionally) configured to use
    procedurally generated values for dataset ids, instead of the default
    random strings. 

    The use of a stored procedure to create an identifier is explained in the
    installation documentation (where an example script is supplied).
    The stored procedure can be implemented using other SQL flavors -
    without having to modify the application code. 
            -- L.A. 4.6.2 (modified by C.S. for version 5.4.1+)
*/ 
@NamedStoredProcedureQuery(
        name = "Dataset.generateIdentifierFromStoredProcedure",
        procedureName = "generateIdentifierFromStoredProcedure",
        parameters = {
            @StoredProcedureParameter(mode = ParameterMode.OUT, type = String.class)
        }
)
@Entity
@Table(indexes = {
    @Index(columnList = "guestbook_id"),
    @Index(columnList = "thumbnailfile_id")})
public class Dataset extends DvObjectContainer {

    public static final String TARGET_URL = "/citation?persistentId=";
    private static final long serialVersionUID = 1L;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.MERGE)
    @OrderBy("id")
    private List<DataFile> files = new ArrayList<>();

    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastExportTime;

    
    @OneToMany(mappedBy = "dataset", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("versionNumber DESC, minorVersionNumber DESC")
    private List<DatasetVersion> versions = new ArrayList<>();

    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DatasetLock> datasetLocks;
    
    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "thumbnailfile_id")
    private DataFile thumbnailFile;
    
    @OneToMany(mappedBy = "dataset", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetMetrics> datasetMetrics = new ArrayList<>(); 
    
    @OneToMany(mappedBy = "dataset", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetExternalCitations> datasetExternalCitations = new ArrayList<>(); 
    
    /**
     * By default, Dataverse will attempt to show unique thumbnails for datasets
     * based on images that have been uploaded to them. Setting this to true
     * will result in a generic dataset thumbnail appearing instead.
     */
    private boolean useGenericThumbnail;

    @ManyToOne
    @JoinColumn(name="datasettype_id", nullable = false)
    private DatasetType datasetType;

    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "guestbook_id", unique = false, nullable = true, insertable = true, updatable = true)
    private Guestbook guestbook;
    
    @OneToMany(mappedBy="dataset", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetLinkingDataverse> datasetLinkingDataverses;

    public List<DatasetLinkingDataverse> getDatasetLinkingDataverses() {
        return datasetLinkingDataverses;
    }

    public void setDatasetLinkingDataverses(List<DatasetLinkingDataverse> datasetLinkingDataverses) {
        this.datasetLinkingDataverses = datasetLinkingDataverses;
    }

    private boolean fileAccessRequest;
    @OneToMany(mappedBy = "dataset", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DataFileCategory> dataFileCategories = null;
    
    @ManyToOne
    @JoinColumn(name = "citationDateDatasetFieldType_id")
    private DatasetFieldType citationDateDatasetFieldType;
    
    public DatasetFieldType getCitationDateDatasetFieldType() {
        return citationDateDatasetFieldType;
    }

    public void setCitationDateDatasetFieldType(DatasetFieldType citationDateDatasetFieldType) {
        this.citationDateDatasetFieldType = citationDateDatasetFieldType;
    }    

    // Per DataCite best practices, the citation date of a dataset may need 
    // to be adjusted to reflect the latest embargo availability date of any 
    // file within the first published version. 
    // If any files are embargoed in the first version, this date will be
    // calculated and cached here upon its publication, in the 
    // FinalizeDatasetPublicationCommand. 
    private Timestamp embargoCitationDate;
    
    public Timestamp getEmbargoCitationDate() {
        return embargoCitationDate;
    }

    public void setEmbargoCitationDate(Timestamp embargoCitationDate) {
        this.embargoCitationDate = embargoCitationDate;
    }
    
    
    
    @ManyToOne
    @JoinColumn(name="template_id",nullable = true)
    private Template template;
    
    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public Dataset() {
        this(false);
    }
    
    public Dataset(boolean isHarvested) {
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(this);
        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
        datasetVersion.setFileMetadatas(new ArrayList<>());
        datasetVersion.setVersionNumber((long) 1);
        datasetVersion.setMinorVersionNumber((long) 0);
        versions.add(datasetVersion);
        
        if (!isHarvested) {
            StorageUse storageUse = new StorageUse(this); 
            this.setStorageUse(storageUse);
        }
        
        if (FeatureFlags.DISABLE_DATASET_THUMBNAIL_AUTOSELECT.enabled()) {
            this.setUseGenericThumbnail(true);
        }
    }
    
    /**
     * Checks whether {@code this} dataset is locked for a given reason.
     * @param reason the reason we test for.
     * @return {@code true} iff the data set is locked for {@code reason}.
     */
    public boolean isLockedFor( DatasetLock.Reason reason ) {
        for ( DatasetLock l : getLocks() ) {
            if ( l.getReason() == reason ) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Retrieves the dataset lock for the passed reason. 
     * @param reason
     * @return the dataset lock, or {@code null}.
     */
    public DatasetLock getLockFor( DatasetLock.Reason reason ) {
        for ( DatasetLock l : getLocks() ) {
            if ( l.getReason() == reason ) {
                return l;
            }
        }
        return null;
    }
    
    public Set<DatasetLock> getLocks() {
        // lazy set creation
        if ( datasetLocks == null ) {
            datasetLocks = new HashSet<>();
        }
        return datasetLocks;
    }

    /**
     * JPA use only!
     * @param datasetLocks 
     */
    void setLocks(Set<DatasetLock> datasetLocks) {
        this.datasetLocks = datasetLocks;
    }
    
    public void addLock(DatasetLock datasetLock) {
        getLocks().add(datasetLock);
    }
    
    public void removeLock( DatasetLock aDatasetLock ) {
        getLocks().remove( aDatasetLock );
    }

    public boolean isLocked() {
        return !getLocks().isEmpty();
    }
    
    public Date getLastExportTime() {
        return lastExportTime;
    }

    public void setLastExportTime(Date lastExportTime) {
        this.lastExportTime = lastExportTime;
    }
    
    public Guestbook getGuestbook() {
        return guestbook;
    }

    public void setGuestbook(Guestbook guestbook) {
        this.guestbook = guestbook;
    }

    public boolean isFileAccessRequest() {
        return fileAccessRequest;
    }

    public void setFileAccessRequest(boolean fileAccessRequest) {
        this.fileAccessRequest = fileAccessRequest;
    }

    public String getPersistentURL() {
        return this.getGlobalId().asURL();
    }
    
    public List<DataFile> getFiles() {
        return files;
    }

    public void setFiles(List<DataFile> files) {
        this.files = files;
    }

    public boolean isDeaccessioned() {
        // return true, if all published versions were deaccessioned
        boolean hasDeaccessionedVersions = false;
        for (DatasetVersion testDsv : getVersions()) {
            if (testDsv.isReleased()) {
                return false;
            }
            // Also check for draft version
            if (testDsv.isDraft()) {
                return false;
            }
            if (testDsv.isDeaccessioned()) {
                hasDeaccessionedVersions = true;
            }
        }
        return hasDeaccessionedVersions; // since any published version would have already returned
    }
    

    public DatasetVersion getLatestVersion() {
        return getVersions().get(0);
    }

    public DatasetVersion getLatestVersionForCopy() {
        for (DatasetVersion testDsv : getVersions()) {
            if (testDsv.isReleased() || testDsv.isArchived()) {
                return testDsv;
            }
        }
        return getVersions().get(0);
    }

    public List<DatasetVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<DatasetVersion> versions) {
        this.versions = versions;
    }

    private DatasetVersion createNewDatasetVersion(Template template, FileMetadata fmVarMet) {
        
        DatasetVersion dsv = new DatasetVersion();
        dsv.setVersionState(DatasetVersion.VersionState.DRAFT);
        dsv.setFileMetadatas(new ArrayList<>());
        DatasetVersion latestVersion;

        //if the latest version has values get them copied over
        if (template != null) {
            dsv.updateDefaultValuesFromTemplate(template);
            setVersions(new ArrayList<>());
        } else {
            latestVersion = getLatestVersionForCopy();
            
            if (latestVersion.getUNF() != null){
                dsv.setUNF(latestVersion.getUNF());
            }
            
            if (latestVersion.getDatasetFields() != null && !latestVersion.getDatasetFields().isEmpty()) {
                dsv.setDatasetFields(dsv.copyDatasetFields(latestVersion.getDatasetFields()));
            }
            /*
            adding file metadatas here and updating terms
            because the terms need to know about the files
            in a pre-save validation SEK 12/6/2021
            */
            for (FileMetadata fm : latestVersion.getFileMetadatas()) {
                FileMetadata newFm = new FileMetadata();
                // TODO: 
                // the "category" will be removed, shortly. 
                // (replaced by multiple, tag-like categories of 
                // type DataFileCategory) -- L.A. beta 10
                //newFm.setCategory(fm.getCategory());
                // yep, these are the new categories:
                newFm.setCategories(fm.getCategories());
                newFm.setDescription(fm.getDescription());
                newFm.setLabel(fm.getLabel());
                newFm.setDirectoryLabel(fm.getDirectoryLabel());
                newFm.setRestricted(fm.isRestricted());
                newFm.setDataFile(fm.getDataFile());
                newFm.setDatasetVersion(dsv);
                newFm.setProvFreeForm(fm.getProvFreeForm());
                newFm.setInPriorVersion(true);

                //fmVarMet would be updated in DCT
                if ((fmVarMet != null && !fmVarMet.getId().equals(fm.getId())) || (fmVarMet == null))  {
                    if (fm.getVariableMetadatas() != null) {
                        newFm.copyVariableMetadata(fm.getVariableMetadatas());
                    }
                    if (fm.getVarGroups() != null) {
                        newFm.copyVarGroups(fm.getVarGroups());
                    }
                }
                
                dsv.getFileMetadatas().add(newFm);
            }
            
            if (latestVersion.getTermsOfUseAndAccess()!= null){
                TermsOfUseAndAccess terms = latestVersion.getTermsOfUseAndAccess().copyTermsOfUseAndAccess();
                terms.setDatasetVersion(dsv);
                dsv.setTermsOfUseAndAccess(terms);
            } else {
                TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
                terms.setDatasetVersion(dsv);
                terms.setLicense(null);
                terms.setFileAccessRequest(true);
                dsv.setTermsOfUseAndAccess(terms);
            }
        }

        // I'm adding the version to the list so it will be persisted when
        // the study object is persisted.
        if (template == null) {
            getVersions().add(0, dsv);
        } else {
            this.setVersions(new ArrayList<>());
            getVersions().add(0, dsv);
        }

        dsv.setDataset(this);
        return dsv;
    }

    /**
     * The "edit version" is the most recent *draft* of a dataset, and if the
     * latest version of a dataset is published, a new draft will be created. If
     * you don't want to create a new version, you should be using
     * getLatestVersion.
     *
     * @return The edit version {@code this}.
     */
    public DatasetVersion getOrCreateEditVersion() {
        return getOrCreateEditVersion(null, null);
    }

    public DatasetVersion getOrCreateEditVersion(FileMetadata fm) {
        return getOrCreateEditVersion(null, fm);
    }

    public DatasetVersion getOrCreateEditVersion(Template template, FileMetadata fm) {
        DatasetVersion latestVersion = this.getLatestVersion();
        if (!latestVersion.isWorkingCopy() || template != null) {
            // if the latest version is released or archived, create a new version for editing
            return createNewDatasetVersion(template, fm);
        } else {
            // else, edit existing working copy
            return latestVersion;
        }
    }

    /*
     * @todo Investigate if this method should be deprecated in favor of
     * createNewDatasetVersion.
     */
    public DatasetVersion getCreateVersion(License license) {
        DatasetVersion dsv = new DatasetVersion();
        dsv.setVersionState(DatasetVersion.VersionState.DRAFT);
        dsv.setDataset(this);
        dsv.initDefaultValues(license);
        this.setVersions(new ArrayList<>());
        getVersions().add(0, dsv);
        return dsv;
    }

    public Date getMostRecentMajorVersionReleaseDate() {
        if (this.isHarvested()) {
            return getVersions().get(0).getReleaseTime();
        } else {
            for (DatasetVersion version : this.getVersions()) {
                if (version.isReleased() && version.getMinorVersionNumber().equals((long) 0)) {
                    return version.getReleaseTime();
                }
            }
            return null;
        }
    }

    public DatasetVersion getReleasedVersion() {
        for (DatasetVersion version : this.getVersions()) {
            if (version.isReleased()) {
                return version;
            }
        }
        return null;
    }
    
    public DatasetVersion getVersionFromId(Long datasetVersionId) {
        for (DatasetVersion version : this.getVersions()) {
            if (datasetVersionId == version.getId().longValue()) {
                return version;
            }
        }
        return null;
    }

    public List<DataFileCategory> getCategories() {
        return dataFileCategories;
    }

    public void setFileCategories(List<DataFileCategory> categories) {
        this.dataFileCategories = categories;
    }

    public void addFileCategory(DataFileCategory category) {
        if (dataFileCategories == null) {
            dataFileCategories = new ArrayList<>();
        }
        dataFileCategories.add(category);
    }

    public void setCategoriesByName(List<String> newCategoryNames) {
        if (newCategoryNames != null) {
            Collection<String> oldCategoryNames = getCategoryNames();

            for (String newCategoryName : newCategoryNames) {
                if (!oldCategoryNames.contains(newCategoryName)) {
                    DataFileCategory newCategory = new DataFileCategory();
                    newCategory.setName(newCategoryName);
                    newCategory.setDataset(this);
                    this.addFileCategory(newCategory);
                }
            }
        }
    }

    public DataFileCategory getCategoryByName(String categoryName) {
        if (categoryName != null && !categoryName.isEmpty()) {
            if (dataFileCategories != null) {
                for (DataFileCategory dataFileCategory : dataFileCategories) {
                    if (categoryName.equals(dataFileCategory.getName())) {
                        return dataFileCategory;
                    }
                }
            }

            DataFileCategory newCategory = new DataFileCategory();
            newCategory.setName(categoryName);
            newCategory.setDataset(this);
            this.addFileCategory(newCategory);

            return newCategory;
        }
        return null;
    }

    private Collection<String> getCategoryNames() {
        if (dataFileCategories != null) {
            ArrayList<String> ret = new ArrayList<>(dataFileCategories.size());
            for ( DataFileCategory dfc : dataFileCategories ) {
                ret.add( dfc.getName() );
            }
            return ret;
        } else {
            return new ArrayList<>();
        }
    }

    /* Only used with packageFiles after the implementation of multi-store in #6488
     * DO NOT USE THIS METHOD FOR ANY OTHER PURPOSES - it's @Deprecated for a reason.
     * 
     */
    @Deprecated 
    public Path getFileSystemDirectory() {
        Path studyDir = null;
        
        String filesRootDirectory = JvmSettings.FILES_DIRECTORY.lookup();
        
        if (this.getAlternativePersistentIndentifiers() != null && !this.getAlternativePersistentIndentifiers().isEmpty()) {
            for (AlternativePersistentIdentifier api : this.getAlternativePersistentIndentifiers()) {
                if (api.isStorageLocationDesignator()) {
                    studyDir = Paths.get(filesRootDirectory, api.getAuthority(), api.getIdentifier());
                    return studyDir;
                }
            }
        }

        if (this.getAuthority() != null && this.getIdentifier() != null) {
            studyDir = Paths.get(filesRootDirectory, this.getAuthority(), this.getIdentifier());
        }

        return studyDir;
    }
    
    public String getAlternativePersistentIdentifier(){
        String retVal = null;            
        if (this.getAlternativePersistentIndentifiers() != null && !this.getAlternativePersistentIndentifiers().isEmpty()) {
            for (AlternativePersistentIdentifier api : this.getAlternativePersistentIndentifiers()) {
                retVal = retVal != null ? retVal + "; " : "";
                retVal += api.getProtocol() + ":";
                retVal += api.getAuthority() + "/";
                retVal +=  api.getIdentifier();
            }
        }
        return retVal;       
    }
    
    public String getProtocolForFileStorage(){
        String retVal = getProtocol();            
        if (this.getAlternativePersistentIndentifiers() != null && !this.getAlternativePersistentIndentifiers().isEmpty()) {
            for (AlternativePersistentIdentifier altpid : this.getAlternativePersistentIndentifiers()) {
                if (altpid.isStorageLocationDesignator()) {
                    retVal = altpid.getProtocol();
                }
            }
        }
        return retVal;         
    }
    
    public String getAuthorityForFileStorage(){
        String retVal = getAuthority();            
        if (this.getAlternativePersistentIndentifiers() != null && !this.getAlternativePersistentIndentifiers().isEmpty()) {
            for (AlternativePersistentIdentifier altpid : this.getAlternativePersistentIndentifiers()) {
                if (altpid.isStorageLocationDesignator()) {
                    retVal = altpid.getAuthority();
                }
            }
        }
        return retVal;         
    }
    
    public String getIdentifierForFileStorage(){
        String retVal = getIdentifier();            
        if (this.getAlternativePersistentIndentifiers() != null && !this.getAlternativePersistentIndentifiers().isEmpty()) {
            for (AlternativePersistentIdentifier altpid : this.getAlternativePersistentIndentifiers()) {
                if (altpid.isStorageLocationDesignator()) {
                    retVal = altpid.getIdentifier();
                }
            }
        }
        return retVal;         
    }

    public String getNextMajorVersionString() {
        // Never need to get the next major version for harvested studies.
        if (isHarvested()) {
            throw new IllegalStateException();
        }
        for (DatasetVersion dv : this.getVersions()) {
            if (!dv.isWorkingCopy()) {
                return Integer.toString(dv.getVersionNumber().intValue() + 1) + ".0";
            }
        }
        return "1.0";
    }

    public String getNextMinorVersionString() {
        // Never need to get the next minor version for harvested studies.
        if (isHarvested()) {
            throw new IllegalStateException();
        }
        for (DatasetVersion dv : this.getVersions()) {
            if (!dv.isWorkingCopy()) {
                return Integer.toString(dv.getVersionNumber().intValue()) + "."
                        + Integer.toString(dv.getMinorVersionNumber().intValue() + 1);
            }
        }
        return "1.0";
    }

    public Integer getVersionNumber() {
        for (DatasetVersion dv : this.getVersions()) {
            if (!dv.isWorkingCopy()) {
                return dv.getVersionNumber().intValue();
            }
        }
        return 1;
    }

    public Integer getMinorVersionNumber() {
        for (DatasetVersion dv : this.getVersions()) {
            if (!dv.isWorkingCopy()) {
                return dv.getMinorVersionNumber().intValue();
            }
        }
        return 0;
    }

    public String getCitation() {
        return getCitation(false, getLatestVersion());
    }

    public String getCitation(DatasetVersion version) {
        return version.getCitation();
    }

    public String getCitation(boolean isOnlineVersion, DatasetVersion version) {
        return getCitation(isOnlineVersion, version, false);
    }
    
    public String getCitation(boolean isOnlineVersion, DatasetVersion version, boolean anonymized) {
        return version.getCitation(isOnlineVersion, anonymized);
    }

    public String getPublicationDateFormattedYYYYMMDD() {
        if (getPublicationDate() != null){
                   return new SimpleDateFormat("yyyy-MM-dd").format(getPublicationDate()); 
        }
        return null;
    }
    
    public Timestamp getCitationDate() {
        Timestamp citationDate = null;
        //Only calculate if this dataset doesn't use an alternate date field for publication date
        if (citationDateDatasetFieldType == null) {
            citationDate = super.getPublicationDate();
            if (embargoCitationDate != null) {
                if (citationDate.compareTo(embargoCitationDate) < 0) {
                    return embargoCitationDate;
                }
            }
        }
        return citationDate;
    }
    
    public String getCitationDateFormattedYYYYMMDD() {
        if (getCitationDate() != null){
                   return new SimpleDateFormat("yyyy-MM-dd").format(getCitationDate()); 
        }
        return null;
    }

    public DataFile getThumbnailFile() {
        return thumbnailFile;
    }

    public void setThumbnailFile(DataFile thumbnailFile) {
        this.thumbnailFile = thumbnailFile;
    }

    public boolean isUseGenericThumbnail() {
        return useGenericThumbnail;
    }

    public void setUseGenericThumbnail(boolean useGenericThumbnail) {
        this.useGenericThumbnail = useGenericThumbnail;
    }

    public DatasetType getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(DatasetType datasetType) {
        this.datasetType = datasetType;
    }

    public List<DatasetMetrics> getDatasetMetrics() {
        return datasetMetrics;
    }

    public void setDatasetMetrics(List<DatasetMetrics> datasetMetrics) {
        this.datasetMetrics = datasetMetrics;
    }
    
    public List<DatasetExternalCitations> getDatasetExternalCitations() {
        return datasetExternalCitations;
    }

    public void setDatasetExternalCitations(List<DatasetExternalCitations> datasetExternalCitations) {
        this.datasetExternalCitations = datasetExternalCitations;
    }

    @ManyToOne
    @JoinColumn(name="harvestingClient_id")
    private  HarvestingClient harvestedFrom;

    public HarvestingClient getHarvestedFrom() {
        return this.harvestedFrom;
    }

    public void setHarvestedFrom(HarvestingClient harvestingClientConfig) {
        this.harvestedFrom = harvestingClientConfig;
    }
    
    public boolean isHarvested() {
        return this.harvestedFrom != null;
    }

    private String harvestIdentifier;
     
    public String getHarvestIdentifier() {
        return harvestIdentifier;
    }

    public void setHarvestIdentifier(String harvestIdentifier) {
        this.harvestIdentifier = harvestIdentifier;
    }

    public String getLocalURL() {
        //Assumes GlobalId != null
        return  SystemConfig.getDataverseSiteUrlStatic() + "/dataset.xhtml?persistentId=" + this.getGlobalId().asString();
    }
    
    public String getRemoteArchiveURL() {
        if (isHarvested()) {
            if (HarvestingClient.HARVEST_STYLE_DATAVERSE.equals(this.getHarvestedFrom().getHarvestStyle())) {
                return this.getHarvestedFrom().getArchiveUrl() + "/dataset.xhtml?persistentId=" + getGlobalId().asString();
            } else if (HarvestingClient.HARVEST_STYLE_VDC.equals(this.getHarvestedFrom().getHarvestStyle())) {
                String rootArchiveUrl = this.getHarvestedFrom().getHarvestingUrl();
                int c = rootArchiveUrl.indexOf("/OAIHandler");
                if (c > 0) {
                    rootArchiveUrl = rootArchiveUrl.substring(0, c);
                    return rootArchiveUrl + "/faces/study/StudyPage.xhtml?globalId=" + getGlobalId().asString();
                }
            } else if (HarvestingClient.HARVEST_STYLE_ICPSR.equals(this.getHarvestedFrom().getHarvestStyle())) {
                // For the ICPSR, it turns out that the best thing to do is to 
                // rely on the DOI to send the user to the right landing page for 
                // the study: 
                //String icpsrId = identifier;
                //return this.getOwner().getHarvestingClient().getArchiveUrl() + "/icpsrweb/ICPSR/studies/"+icpsrId+"?q="+icpsrId+"&amp;searchSource=icpsr-landing";
                return "http://doi.org/" + this.getAuthority() + "/" + this.getIdentifier();
            } else if (HarvestingClient.HARVEST_STYLE_NESSTAR.equals(this.getHarvestedFrom().getHarvestStyle())) {
                String nServerURL = this.getHarvestedFrom().getArchiveUrl();
                // chop any trailing slashes in the server URL - or they will result
                // in multiple slashes in the final URL pointing to the study 
                // on server of origin; Nesstar doesn't like it, apparently. 
                nServerURL = nServerURL.replaceAll("/*$", "");

                String nServerURLencoded = nServerURL;

                nServerURLencoded = nServerURLencoded.replace(":", "%3A").replace("/", "%2F");
                //SEK 09/13/18
                String NesstarWebviewPage = nServerURL
                        + "/webview/?mode=documentation&submode=abstract&studydoc="
                        + nServerURLencoded + "%2Fobj%2FfStudy%2F"
                        + this.getIdentifier()
                        + "&top=yes";

                return NesstarWebviewPage;
            } else if (HarvestingClient.HARVEST_STYLE_ROPER.equals(this.getHarvestedFrom().getHarvestStyle())) {
                return this.getHarvestedFrom().getArchiveUrl() + "/CFIDE/cf/action/catalog/abstract.cfm?archno=" + this.getIdentifier();
            } else if (HarvestingClient.HARVEST_STYLE_HGL.equals(this.getHarvestedFrom().getHarvestStyle())) {
                // a bit of a hack, true. 
                // HGL documents, when turned into Dataverse studies/datasets
                // all 1 datafile; the location ("storage identifier") of the file
                // is the URL pointing back to the HGL GUI viewer. This is what 
                // we will display for the dataset URL.  -- L.A. 
                // TODO: create a 4.+ ticket for a cleaner solution. 
                List<DataFile> dataFiles = this.getFiles();
                if (dataFiles != null && dataFiles.size() == 1) {
                    if (dataFiles.get(0) != null) {
                        String hglUrl = dataFiles.get(0).getStorageIdentifier();
                        if (hglUrl != null && hglUrl.matches("^http.*")) {
                            return hglUrl;
                        }
                    }
                }
                return this.getHarvestedFrom().getArchiveUrl();
            } else if (HarvestingClient.HARVEST_STYLE_DEFAULT.equals(this.getHarvestedFrom().getHarvestStyle())) {
                // This is a generic OAI archive. 
                // The metadata we harvested for this dataset is most likely a 
                // simple DC record that does not contain a URL pointing back at 
                // the specific location on the source archive. But, it probably
                // has a global identifier, a DOI or a Handle - so we should be 
                // able to redirect to the proper global resolver. 
                // But since this is a harvested dataset, we will assume that 
                // there is a possibility tha this object does NOT have all the 
                // valid persistent identifier components.
                
                if (StringUtil.nonEmpty(this.getProtocol()) 
                        && StringUtil.nonEmpty(this.getAuthority())
                        && StringUtil.nonEmpty(this.getIdentifier())) {
                    
                    // If there is a custom archival url for this Harvesting 
                    // Source, we'll use that
                    String harvestingUrl = this.getHarvestedFrom().getHarvestingUrl();
                    String archivalUrl = this.getHarvestedFrom().getArchiveUrl();
                    if (!harvestingUrl.contains(archivalUrl)) {
                        // When a Harvesting Client is created, the “archive url” is set to 
                        // just the host part of the OAI url automatically. 
                        // For example, if the OAI url was "https://remote.edu/oai", 
                        // the archive url will default to "https://remote.edu/". 
                        // If this is no longer true, we know it means the admin 
                        // went to the trouble of setting it to something else - 
                        // so we should use this url for the redirects back to source, 
                        // instead of the global id resolver.
                        return archivalUrl + this.getAuthority() + "/" + this.getIdentifier();
                    }
                    // ... if not, we'll redirect to the resolver for the global id: 
                    return this.getPersistentURL();    
                }
                
                // All we can do is redirect them to the top-level URL we have 
                // on file for this remote archive:
                return this.getHarvestedFrom().getArchiveUrl();
            } else {
                // This is really not supposed to happen - this is a harvested
                // dataset for which we don't have ANY information on the nature
                // of the archive we got it from. So all we can do is redirect 
                // the user to the top-level URL we have on file for this remote 
                // archive:
                return this.getHarvestedFrom().getArchiveUrl();
            }
        }

        return null;
    }

    public String getHarvestingDescription() {
        if (isHarvested()) {
            return this.getHarvestedFrom().getArchiveDescription();
        }

        return null;
    }

    public boolean hasEnabledGuestbook(){
        Guestbook gb = this.getGuestbook();

        return ( gb != null && gb.isEnabled());
    }
    
    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Dataset)) {
            return false;
        }
        Dataset other = (Dataset) object;
        return Objects.equals(getId(), other.getId());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
    
    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visit(this);
    }

    @Override
    public String getDisplayName() {
        DatasetVersion dsv = getReleasedVersion();
        String result = dsv != null ? dsv.getTitle() : getLatestVersion().getTitle();
        boolean resultIsEmpty = result == null || "".equals(result);
        if (resultIsEmpty && getGlobalId() != null) {
            return getGlobalId().asString();
        }
        return result;
    }
    
    @Override
    public String getCurrentName(){
        return getLatestVersion().getTitle();
    }

    @Override
    protected boolean isPermissionRoot() {
        return false;
    }
    
    @Override
    public boolean isAncestorOf( DvObject other ) {
        return equals(other) || equals(other.getOwner());
    }

    public DatasetThumbnail getDatasetThumbnail(int size) {
        return DatasetUtil.getThumbnail(this, size);
    }
    
    /** 
     * Handle the case where we also have the datasetVersionId.
     * This saves trying to find the latestDatasetVersion, and 
     * other costly queries, etc.
     * 
     * @param datasetVersion
     * @return A thumbnail of the dataset (may be {@code null}).
     */
    public DatasetThumbnail getDatasetThumbnail(DatasetVersion datasetVersion, int size) {
        return DatasetUtil.getThumbnail(this, datasetVersion, size);
    }

    @Override
    public String getTargetUrl() {
        return Dataset.TARGET_URL;
    }
}
