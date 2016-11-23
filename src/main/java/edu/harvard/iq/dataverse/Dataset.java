package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author skraffmiller
 */
@NamedQueries(
        @NamedQuery(name = "Dataset.findByIdentifier",
                query = "SELECT d FROM Dataset d WHERE d.identifier=:identifier")
)
@Entity
@Table(indexes = {
    @Index(columnList = "guestbook_id"),
    @Index(columnList = "thumbnailfile_id")},
        uniqueConstraints = @UniqueConstraint(columnNames = {"authority,protocol,identifier,doiseparator"}))
public class Dataset extends DvObjectContainer {
    private static final Logger logger = Logger.getLogger(Dataset.class.getCanonicalName());

//    public static final String REDIRECT_URL = "/dataset.xhtml?persistentId=";
    public static final String TARGET_URL = "/citation?persistentId=";
    private static final long serialVersionUID = 1L;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.MERGE)
    private List<DataFile> files = new ArrayList();

    private String protocol;
    private String authority;
    private String doiSeparator;

    @Temporal(value = TemporalType.TIMESTAMP)
    private Date globalIdCreateTime;
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastExportTime;

    @NotBlank(message = "Please enter an identifier for your dataset.")
    @Column(nullable = false)
    private String identifier;
    @OneToMany(mappedBy = "dataset", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("versionNumber DESC, minorVersionNumber DESC")
    private List<DatasetVersion> versions = new ArrayList();
    @OneToOne(mappedBy = "dataset", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private DatasetLock datasetLock;
    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "thumbnailfile_id")
    private DataFile thumbnailFile;

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

    public String getGlobalId() {       
        return new GlobalId(this).toString();
    }

    public List<DataFile> getFiles() {
        logger.info("getFiles() on dataset "+this.getId());
        return files;
    }

    public void setFiles(List<DataFile> files) {
        logger.info("setFiles() on dataset "+this.getId());
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
            //Also check for draft version
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
        dsv.setFileMetadatas(new ArrayList());
        DatasetVersion latestVersion;

        //if the latest version has values get them copied over
        if (template != null) {
            dsv.updateDefaultValuesFromTemplate(template);
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
                dsv.getFileMetadatas().add(newFm);
            }
        }

        // I'm adding the version to the list so it will be persisted when
        // the study object is persisted.
        if (template == null) {
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
        dsv.initDefaultValues();
        this.setVersions(new ArrayList());
        getVersions().add(0, dsv);
        return dsv;
    }

    public Date getMostRecentMajorVersionReleaseDate() {
        if (this.isHarvested()) {
            return getVersions().get(0).getReleaseTime();
        } else {
            for (DatasetVersion version : this.getVersions()) {
                if (version.isReleased() && version.getMinorVersionNumber().equals(new Long(0))) {
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
        // (TODO: ? - provide these as constants somewhere? -- L.A. beta15)
        if (!ret.contains("Documentation")) {
            ret.add("Documentation");
        }
        if (!ret.contains("Data")) {
            ret.add("Data");
        }
        if (!ret.contains("Code")) {
            ret.add("Code");
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
    /*
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
     }*/

    public DataFileCategory getCategoryByName(String categoryName) {
        if (categoryName != null && !categoryName.equals("")) {
            if (dataFileCategories != null) {
                for (int i = 0; i < dataFileCategories.size(); i++) {
                    if (categoryName.equals(dataFileCategories.get(i).getName())) {
                        return dataFileCategories.get(i);
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

        if (this.getAuthority() != null && this.getIdentifier() != null) {
            studyDir = Paths.get(filesRootDirectory, this.getAuthority(), this.getIdentifier());
        }

        return studyDir;
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
                return this.getHarvestedFrom().getArchiveUrl() + "/dataset.xhtml?persistentId=" + getGlobalId();
            } else if (HarvestingClient.HARVEST_STYLE_VDC.equals(this.getHarvestedFrom().getHarvestStyle())) {
                String rootArchiveUrl = this.getHarvestedFrom().getHarvestingUrl();
                int c = rootArchiveUrl.indexOf("/OAIHandler");
                if (c > 0) {
                    rootArchiveUrl = rootArchiveUrl.substring(0, c);
                    return rootArchiveUrl + "/faces/study/StudyPage.xhtml?globalId=" + getGlobalId();
                }
            } else if (HarvestingClient.HARVEST_STYLE_ICPSR.equals(this.getHarvestedFrom().getHarvestStyle())) {
                // For the ICPSR, it turns out that the best thing to do is to 
                // rely on the DOI to send the user to the right landing page for 
                // the study: 
                //String icpsrId = identifier;
                //return this.getOwner().getHarvestingClient().getArchiveUrl() + "/icpsrweb/ICPSR/studies/"+icpsrId+"?q="+icpsrId+"&amp;searchSource=icpsr-landing";
                return "http://doi.org/" + authority + "/" + identifier;
            } else if (HarvestingClient.HARVEST_STYLE_NESSTAR.equals(this.getHarvestedFrom().getHarvestStyle())) {
                String nServerURL = this.getHarvestedFrom().getArchiveUrl();
                // chop any trailing slashes in the server URL - or they will result
                // in multiple slashes in the final URL pointing to the study 
                // on server of origin; Nesstar doesn't like it, apparently. 
                nServerURL = nServerURL.replaceAll("/*$", "");

                String nServerURLencoded = nServerURL;

                nServerURLencoded.replace(":", "%3A");
                nServerURLencoded.replace("/", "%2F");

                String NesstarWebviewPage = nServerURL
                        + "/webview/?mode=documentation&submode=abstract&studydoc="
                        + nServerURLencoded + "%2Fobj%2FfStudy%2F"
                        + identifier
                        + "&top=yes";

                return NesstarWebviewPage;
            } else if (HarvestingClient.HARVEST_STYLE_ROPER.equals(this.getHarvestedFrom().getHarvestStyle())) {
                return this.getHarvestedFrom().getArchiveUrl() + "/CFIDE/cf/action/catalog/abstract.cfm?archno=" + identifier;
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
            }else {
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
}
