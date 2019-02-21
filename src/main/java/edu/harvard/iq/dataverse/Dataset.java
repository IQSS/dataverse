package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureParameter;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import edu.harvard.iq.dataverse.util.BundleUtil;

/**
 *
 * @author skraffmiller
 */
@NamedQueries({
    @NamedQuery(name = "Dataset.findByIdentifier",
               query = "SELECT d FROM Dataset d WHERE d.identifier=:identifier"),
    @NamedQuery(name = "Dataset.findByIdentifierAuthorityProtocol",
               query = "SELECT d FROM Dataset d WHERE d.identifier=:identifier AND d.protocol=:protocol AND d.authority=:authority"),
    @NamedQuery(name = "Dataset.findIdByOwnerId", 
                query = "SELECT o.identifier FROM Dataset o WHERE o.owner.id=:ownerId"),
    @NamedQuery(name = "Dataset.findByOwnerId", 
                query = "SELECT o FROM Dataset o WHERE o.owner.id=:ownerId"),
})

/*
    Below is the stored procedure for getting a numeric value from a database 
    sequence. Used when the Dataverse is (optionally) configured to use 
    incremental numeric values for dataset ids, instead of the default 
    random strings. 

    Unfortunately, there's no standard EJB way of handling sequences. So in the 
    past we would simply use a NativeQuery to call a proprietary Postgres
    sequence query. A better way of handling this however is to define any 
    proprietary SQL functionality outside of the application, in the database, 
    and call it using the standard JPA @StoredProcedureQuery. 

    The identifier sequence and the stored procedure for accessing it are currently 
    implemented with PostgresQL "CREATE SEQUENCE ..." and "CREATE FUNCTION ..."; 
    (we explain how to create these in the installation documentation and supply 
    a script). If necessary, it can be implemented using other SQL flavors -
    without having to modify the application code. 
            -- L.A. 4.6.2
*/ 
@NamedStoredProcedureQuery(
        name = "Dataset.generateIdentifierAsSequentialNumber",
        procedureName = "generateIdentifierAsSequentialNumber",
        parameters = {
            @StoredProcedureParameter(mode = ParameterMode.OUT, type = Integer.class, name = "identifier")
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
    
    /**
     * By default, Dataverse will attempt to show unique thumbnails for datasets
     * based on images that have been uploaded to them. Setting this to true
     * will result in a generic dataset thumbnail appearing instead.
     */
    private boolean useGenericThumbnail;

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

    public Dataset() {
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(this);
        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
        datasetVersion.setFileMetadatas(new ArrayList<>());
        datasetVersion.setVersionNumber((long) 1);
        datasetVersion.setMinorVersionNumber((long) 0);
        versions.add(datasetVersion);
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
        return new GlobalId(this).toURL().toString();
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

    private DatasetVersion createNewDatasetVersion(Template template) {
        DatasetVersion dsv = new DatasetVersion();
        dsv.setVersionState(DatasetVersion.VersionState.DRAFT);
        dsv.setFileMetadatas(new ArrayList<>());
        DatasetVersion latestVersion;

        //if the latest version has values get them copied over
        if (template != null) {
            dsv.updateDefaultValuesFromTemplate(template);
            setVersions(new ArrayList());
        } else {
            latestVersion = getLatestVersionForCopy();
            
            if (latestVersion.getUNF() != null){
                dsv.setUNF(latestVersion.getUNF());
            }
            
            if (latestVersion.getDatasetFields() != null && !latestVersion.getDatasetFields().isEmpty()) {
                dsv.setDatasetFields(dsv.copyDatasetFields(latestVersion.getDatasetFields()));
            }
            
            if (latestVersion.getTermsOfUseAndAccess()!= null){
                dsv.setTermsOfUseAndAccess(latestVersion.getTermsOfUseAndAccess().copyTermsOfUseAndAccess());
            } else {
                TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
                terms.setDatasetVersion(dsv);
                terms.setLicense(TermsOfUseAndAccess.License.CC0);
                dsv.setTermsOfUseAndAccess(terms);
            }

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
                
                dsv.getFileMetadatas().add(newFm);
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
     * latest version of a dataset is published, a new draft will be created.
     * 
     * @return The edit version {@code this}.
     */
    public DatasetVersion getEditVersion() {
        return getEditVersion(null);
    }

    public DatasetVersion getEditVersion(Template template) {
        DatasetVersion latestVersion = this.getLatestVersion();
        if (!latestVersion.isWorkingCopy() || template != null) {
            // if the latest version is released or archived, create a new version for editing
            return createNewDatasetVersion(template);
        } else {
            // else, edit existing working copy
            return latestVersion;
        }
    }

    /*
     * @todo Investigate if this method should be deprecated in favor of
     * createNewDatasetVersion.
     */
    public DatasetVersion getCreateVersion() {
        DatasetVersion dsv = new DatasetVersion();
        dsv.setVersionState(DatasetVersion.VersionState.DRAFT);
        dsv.setDataset(this);
        dsv.initDefaultValues();
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

    public Collection<String> getCategoriesByName() {
        Collection<String> ret = getCategoryNames();

        // "Documentation", "Data" and "Code" are the 3 default categories that we 
        // present by default:
        if (!ret.contains(BundleUtil.getStringFromBundle("dataset.category.documentation"))) {
            ret.add(BundleUtil.getStringFromBundle("dataset.category.documentation"));
        }
        if (!ret.contains(BundleUtil.getStringFromBundle("dataset.category.data"))) {
            ret.add(BundleUtil.getStringFromBundle("dataset.category.data"));
        }
        if (!ret.contains(BundleUtil.getStringFromBundle("dataset.category.code"))) {
            ret.add(BundleUtil.getStringFromBundle("dataset.category.code"));
        }

        return ret;
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

    public Path getFileSystemDirectory() {
        Path studyDir = null;

        String filesRootDirectory = System.getProperty("dataverse.files.directory");
        if (filesRootDirectory == null || filesRootDirectory.equals("")) {
            filesRootDirectory = "/tmp/files";
        }
        
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
            for (AlternativePersistentIdentifier api : this.getAlternativePersistentIndentifiers()) {
                retVal = api.getProtocol();
            }
        }
        return retVal;         
    }
    
    public String getAuthorityForFileStorage(){
         String retVal = getAuthority();            
        if (this.getAlternativePersistentIndentifiers() != null && !this.getAlternativePersistentIndentifiers().isEmpty()) {
            for (AlternativePersistentIdentifier api : this.getAlternativePersistentIndentifiers()) {
                retVal = api.getAuthority();
            }
        }
        return retVal;         
    }
    
    public String getIdentifierForFileStorage(){
         String retVal = getIdentifier();            
        if (this.getAlternativePersistentIndentifiers() != null && !this.getAlternativePersistentIndentifiers().isEmpty()) {
            for (AlternativePersistentIdentifier api : this.getAlternativePersistentIndentifiers()) {
                retVal = api.getIdentifier();
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
        return version.getCitation(isOnlineVersion);
    }

    public String getPublicationDateFormattedYYYYMMDD() {
        if (getPublicationDate() != null){
                   return new SimpleDateFormat("yyyy-MM-dd").format(getPublicationDate()); 
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

    public String getRemoteArchiveURL() {
        if (isHarvested()) {
            if (HarvestingClient.HARVEST_STYLE_DATAVERSE.equals(this.getHarvestedFrom().getHarvestStyle())) {
                return this.getHarvestedFrom().getArchiveUrl() + "/dataset.xhtml?persistentId=" + getGlobalIdString();
            } else if (HarvestingClient.HARVEST_STYLE_VDC.equals(this.getHarvestedFrom().getHarvestStyle())) {
                String rootArchiveUrl = this.getHarvestedFrom().getHarvestingUrl();
                int c = rootArchiveUrl.indexOf("/OAIHandler");
                if (c > 0) {
                    rootArchiveUrl = rootArchiveUrl.substring(0, c);
                    return rootArchiveUrl + "/faces/study/StudyPage.xhtml?globalId=" + getGlobalIdString();
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
            } else {
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
        return dsv != null ? dsv.getTitle() : getLatestVersion().getTitle();
    }

    @Override
    protected boolean isPermissionRoot() {
        return false;
    }
    
    @Override
    public boolean isAncestorOf( DvObject other ) {
        return equals(other) || equals(other.getOwner());
    }

    public DatasetThumbnail getDatasetThumbnail() {
        return DatasetUtil.getThumbnail(this);
    }
    
    /** 
     * Handle the case where we also have the datasetVersionId.
     * This saves trying to find the latestDatasetVersion, and 
     * other costly queries, etc.
     * 
     * @param datasetVersion
     * @return A thumbnail of the dataset (may be {@code null}).
     */
    public DatasetThumbnail getDatasetThumbnail(DatasetVersion datasetVersion) {
        return DatasetUtil.getThumbnail(this, datasetVersion);
    }

}
