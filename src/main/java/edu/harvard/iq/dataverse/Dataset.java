package edu.harvard.iq.dataverse;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author skraffmiller
 */
@NamedQueries(
        @NamedQuery( name="Dataset.findByIdentifier",
                     query="SELECT d FROM Dataset d WHERE d.identifier=:identifier")
)
@Entity
public class Dataset extends DvObjectContainer {

    private static final long serialVersionUID = 1L;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.MERGE)
    private List<DataFile> files = new ArrayList();

    private String protocol;
    private String authority;
    private String doiSeparator;
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date globalIdCreateTime;
    @NotBlank(message = "Please enter an identifier for your dataset.")
    private String identifier;
    @OneToMany(mappedBy = "dataset", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("id DESC")
    private List<DatasetVersion> versions = new ArrayList();
    @OneToOne(mappedBy = "dataset", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private DatasetLock datasetLock;
    @OneToOne(cascade={CascadeType.MERGE,CascadeType.PERSIST})
    @JoinColumn(name="thumbnailfile_id")
    private DataFile thumbnailFile;
    
    @OneToOne(cascade={CascadeType.MERGE,CascadeType.PERSIST})
    @JoinColumn(name="guestbook_id", unique= true, nullable=true, insertable=true, updatable=true)
    private Guestbook guestbook;
    
    private boolean fileAccessRequest;
    @OneToMany(mappedBy = "dataset", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DataFileCategory> dataFileCategories = null;

    public Dataset() {
        //this.versions = new ArrayList();
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(this);
        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
        datasetVersion.setFileMetadatas(new ArrayList());
        datasetVersion.setVersionNumber(new Long(1));
        datasetVersion.setMinorVersionNumber(new Long(0));
        versions.add(datasetVersion);
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

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    
    
    public String getDoiSeparator() {
        return doiSeparator;
    }

    public void setDoiSeparator(String doiSeparator) {
        this.doiSeparator = doiSeparator;
    }
    
    public Date getGlobalIdCreateTime() {
        return globalIdCreateTime;
    }

    public void setGlobalIdCreateTime(Date globalIdCreateTime) {
        this.globalIdCreateTime = globalIdCreateTime;
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
        switch (this.getProtocol()) {
            case "hdl":
                return getHandleURL();
            case "doi":
                return getEZIdURL();
            default:
                return "";
        }
    }

    private String getHandleURL() {
        return "http://hdl.handle.net/" + authority + "/" + getIdentifier();
    }

    private String getEZIdURL() {
       if(globalIdCreateTime != null){
            return "http://dx.doi.org/" + authority + doiSeparator + getIdentifier();
        } else {
            return "http://dx.doi.org/" + authority + doiSeparator + "Dataset-not-registered";
        } 
    }
    
    public String getGlobalId() {

            return protocol + ":" + authority + doiSeparator + getIdentifier();
                   
    }

    public List<DataFile> getFiles() {
        return files;
    }

    public void setFiles(List<DataFile> files) {
        this.files = files;
    }

    public DatasetLock getDatasetLock() {
        return datasetLock;
    }

    public void setDatasetLock(DatasetLock datasetLock) {
        this.datasetLock = datasetLock;
    }

    public boolean isLocked() {
        if (datasetLock != null) {
            return true;
        }
        return false;
    }
    
    public boolean isDeaccessioned() {
        // return true, if all published versions were deaccessioned
        boolean hasDeaccessionedVersions = false;
        for (DatasetVersion testDsv : getVersions()) {
            if (testDsv.isReleased()) {
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
        DatasetVersion latestVersion = null;

        //if the latest version has values get them copied over
        if (!(template == null)) {
            if (!template.getDatasetFields().isEmpty()) {
                dsv.setDatasetFields(dsv.copyDatasetFields(template.getDatasetFields()));
            }
        } else {
            latestVersion = getLatestVersionForCopy();
            if (latestVersion.getDatasetFields() != null && !latestVersion.getDatasetFields().isEmpty()) {
                dsv.setDatasetFields(dsv.copyDatasetFields(latestVersion.getDatasetFields()));
            }
            dsv.setTermsOfUse(latestVersion.getTermsOfUse());
            dsv.setTermsOfAccess(latestVersion.getTermsOfAccess());
            dsv.setConfidentialityDeclaration(latestVersion.getConfidentialityDeclaration());
            dsv.setSpecialPermissions(latestVersion.getSpecialPermissions());
            dsv.setRestrictions(latestVersion.getRestrictions());
            dsv.setCitationRequirements(latestVersion.getCitationRequirements());
            dsv.setDepositorRequirements(latestVersion.getDepositorRequirements());
            dsv.setConditions(latestVersion.getConditions());
            dsv.setDisclaimer(latestVersion.getDisclaimer());
            dsv.setDataAccessPlace(latestVersion.getDataAccessPlace());
            dsv.setOriginalArchive(latestVersion.getOriginalArchive());
            dsv.setAvailabilityStatus(latestVersion.getAvailabilityStatus());
            dsv.setContactForAccess(latestVersion.getContactForAccess());
            dsv.setSizeOfCollection(latestVersion.getSizeOfCollection());
            dsv.setStudyCompletion(latestVersion.getStudyCompletion());
        }

        dsv.setFileMetadatas(new ArrayList());
        if (latestVersion != null) {
            for (FileMetadata fm : latestVersion.getFileMetadatas()) {
                FileMetadata newFm = new FileMetadata();
                // TODO: 
                // the "category" will be removed, shortly. 
                // (replaced by multiple, tag-like categories of 
                // type DataFileCategory) -- L.A. beta 10
                newFm.setCategory(fm.getCategory());
                // yep, these are the new categories:
                newFm.setCategories(fm.getCategories());
                newFm.setDescription(fm.getDescription());
                newFm.setLabel(fm.getLabel());
                newFm.setDataFile(fm.getDataFile());
                newFm.setDatasetVersion(dsv);
                dsv.getFileMetadatas().add(newFm);                
            }
            dsv.setLicense(latestVersion.getLicense());
        } else {            
            dsv.setLicense(DatasetVersion.License.CC0); 
        }

        // I'm adding the version to the list so it will be persisted when
        // the study object is persisted.
        if(template == null){
            getVersions().add(0, dsv);
        } else {
            this.setVersions(new ArrayList());
            getVersions().add(0, dsv);
        }

        dsv.setDataset(this);
        return dsv;
    }

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
    
    public DatasetVersion getCreateVersion() {
        DatasetVersion dsv = new DatasetVersion();
        dsv.setVersionState(DatasetVersion.VersionState.DRAFT);
        dsv.setDataset(this);
        dsv.setDatasetFields(dsv.initDatasetFields());;
        dsv.setFileMetadatas(new ArrayList());

        this.setVersions(new ArrayList());
        getVersions().add(0, dsv);

        return dsv;
    }

    public Date getMostRecentMajorVersionReleaseDate() {
        for (DatasetVersion version : this.getVersions()) {
            if (version.isReleased() && version.getMinorVersionNumber().equals(new Long(0))) {
                return version.getReleaseTime();
            }
        }
        return null;
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
        
        // "Documentation" and "Data" are basic categories that we 
        // want to be present by default:
        if (!ret.contains("Documentation")) {
            ret.add("Documentation");
        }
        if (!ret.contains("Data")) {
            ret.add("Data");
        }
        
        return ret;
    }
    
    public void setCategoriesByName(List<String> newCategoryNames) {
        if (newCategoryNames != null) {
            Collection<String> oldCategoryNames = getCategoryNames();
            
            for (int i = 0; i < newCategoryNames.size(); i++) {
                if (!oldCategoryNames.contains(newCategoryNames.get(i))) {
                    DataFileCategory newCategory = new DataFileCategory();
                    newCategory.setName(newCategoryNames.get(i));
                    newCategory.setDataset(this);
                    this.addFileCategory(newCategory);
                }
            }
        }
    }
    
    public void addCategoryByName(String newCategoryName) {
        if (newCategoryName != null && !newCategoryName.equals("")) {
            Collection<String> oldCategoryNames = getCategoryNames();
            if (!oldCategoryNames.contains(newCategoryName)) {
                DataFileCategory newCategory = new DataFileCategory();
                newCategory.setName(newCategoryName);
                newCategory.setDataset(this);
                this.addFileCategory(newCategory);
            }
        }
    }
    
    public DataFileCategory getCategoryByName(String categoryName) {
        if (categoryName != null && !categoryName.equals("")) {
            for (int i = 0; i < dataFileCategories.size(); i++) {
                if (categoryName.equals(dataFileCategories.get(i).getName())) {
                    return dataFileCategories.get(i);
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
        ArrayList<String> ret = new ArrayList<>();
        if (dataFileCategories != null) {
            for (int i = 0; i < dataFileCategories.size(); i++) {
                ret.add(dataFileCategories.get(i).getName());
            }
        }
        return ret;
    }
    
    public Path getFileSystemDirectory() {
        Path studyDir = null;

        String filesRootDirectory = System.getProperty("dataverse.files.directory");
        if (filesRootDirectory == null || filesRootDirectory.equals("")) {
            filesRootDirectory = "/tmp/files";
        }

        if (this.getAuthority() != null && this.getIdentifier() != null) {
            studyDir = Paths.get(filesRootDirectory, this.getAuthority(), this.getIdentifier());
        }

        return studyDir;
    }

    public String getNextMajorVersionString() {
        for (DatasetVersion dv : this.getVersions()) {
            if (!dv.isWorkingCopy()) {
                return Integer.toString(dv.getVersionNumber().intValue() + 1) + ".0";
            }
        }
        return "1.0";
    }

    public String getNextMinorVersionString() {
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
    
    public DataFile getThumbnailFile() {
        return thumbnailFile;
    }
    
    public void setThumbnailFile(DataFile thumbnailFile) {
        this.thumbnailFile = thumbnailFile;
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
}
