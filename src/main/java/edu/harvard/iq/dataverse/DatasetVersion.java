package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.MarkupChecker;
import edu.harvard.iq.dataverse.util.PersonOrOrgUtil;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.DataFileComparator;
import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.DateUtil;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import edu.harvard.iq.dataverse.workflows.WorkflowComment;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author skraffmiller
 */

@NamedQueries({
    @NamedQuery(name = "DatasetVersion.findUnarchivedReleasedVersion",
               query = "SELECT OBJECT(o) FROM DatasetVersion AS o WHERE o.dataset.harvestedFrom IS NULL and o.releaseTime IS NOT NULL and o.archivalCopyLocation IS NULL"
    ), 
    @NamedQuery(name = "DatasetVersion.findById", 
                query = "SELECT o FROM DatasetVersion o LEFT JOIN FETCH o.fileMetadatas WHERE o.id=:id"), 
    @NamedQuery(name = "DatasetVersion.findByDataset",
                query = "SELECT o FROM DatasetVersion o WHERE o.dataset.id=:datasetId ORDER BY o.versionNumber DESC, o.minorVersionNumber DESC"), 
    @NamedQuery(name = "DatasetVersion.findReleasedByDataset",
                query = "SELECT o FROM DatasetVersion o WHERE o.dataset.id=:datasetId AND o.versionState=edu.harvard.iq.dataverse.DatasetVersion.VersionState.RELEASED ORDER BY o.versionNumber DESC, o.minorVersionNumber DESC")/*,
    @NamedQuery(name = "DatasetVersion.findVersionElements",
                query = "SELECT o.id, o.versionState, o.versionNumber, o.minorVersionNumber FROM DatasetVersion o WHERE o.dataset.id=:datasetId ORDER BY o.versionNumber DESC, o.minorVersionNumber DESC")*/})
    
    
@Entity
@Table(indexes = {@Index(columnList="dataset_id")},
        uniqueConstraints = @UniqueConstraint(columnNames = {"dataset_id,versionnumber,minorversionnumber"}))
@ValidateDeaccessionNote(deaccessionNote = "deaccessionNote", versionState = "versionState")
public class DatasetVersion implements Serializable {

    private static final Logger logger = Logger.getLogger(DatasetVersion.class.getCanonicalName());
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * Convenience comparator to compare dataset versions by their version number.
     * The draft version is considered the latest.
     */
    public static final Comparator<DatasetVersion> compareByVersion = new Comparator<DatasetVersion>() {
        @Override
        public int compare(DatasetVersion o1, DatasetVersion o2) {
            if ( o1.isDraft() ) {
                return o2.isDraft() ? 0 : 1;
            } else {
               return (int)Math.signum( (o1.getVersionNumber().equals(o2.getVersionNumber())) ?
                        o1.getMinorVersionNumber() - o2.getMinorVersionNumber()
                       : o1.getVersionNumber() - o2.getVersionNumber() );
            }
        }
    };
    public static final JsonObjectBuilder compareVersions(DatasetVersion originalVersion, DatasetVersion newVersion) {
        DatasetVersionDifference diff = new DatasetVersionDifference(newVersion, originalVersion);
        return diff.compareVersionsAsJson();
    }

    // TODO: Determine the UI implications of various version states
    //IMPORTANT: If you add a new value to this enum, you will also have to modify the
    // StudyVersionsFragment.xhtml in order to display the correct value from a Resource Bundle
    public enum VersionState {
        DRAFT, RELEASED, ARCHIVED, DEACCESSIONED
    }

    public static final int DEACCESSION_NOTE_MAX_LENGTH = 1000;
    public static final int DEACCESSION_LINK_MAX_LENGTH = 1260; //Long enough to cover the case where a legacy deaccessionLink(256 char) and archiveNote (1000) are combined (with a space)
    public static final int VERSION_NOTE_MAX_LENGTH = 1000;
    
    //Archival copies: Status message required components
    public static final String ARCHIVAL_STATUS = "status";
    public static final String ARCHIVAL_STATUS_MESSAGE = "message";
    //Archival Copies: Allowed Statuses
    public static final String ARCHIVAL_STATUS_PENDING = "pending";
    public static final String ARCHIVAL_STATUS_SUCCESS = "success";
    public static final String ARCHIVAL_STATUS_FAILURE = "failure";
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String UNF;

    @Version
    private Long version;

    private Long versionNumber;
    private Long minorVersionNumber;
    
    //This is used for the deaccession reason
    @Size(min=0, max=DEACCESSION_NOTE_MAX_LENGTH)
    @Column(length = DEACCESSION_NOTE_MAX_LENGTH)
    private String deaccessionNote;
    
    //This is a plain text, optional reason for the version's creation
    @Size(min=0, max=VERSION_NOTE_MAX_LENGTH)
    @Column(length = VERSION_NOTE_MAX_LENGTH)
    private String versionNote;

    /*
     * @todo versionState should never be null so when we are ready, uncomment
     * the `nullable = false` below.
     */
//    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VersionState versionState;

    @ManyToOne
    private Dataset dataset;

    @OneToMany(mappedBy = "datasetVersion", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("label") // this is not our preferred ordering, which is with the AlphaNumericComparator, but does allow the files to be grouped by category
    private List<FileMetadata> fileMetadatas = new ArrayList();
    
    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval=true)
    @JoinColumn(name = "termsOfUseAndAccess_id")
    private TermsOfUseAndAccess termsOfUseAndAccess;
    
    @OneToMany(mappedBy = "datasetVersion", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetField> datasetFields = new ArrayList();
    
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column( nullable=false )
    private Date createTime;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column( nullable=false )
    private Date lastUpdateTime;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date releaseTime;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date archiveTime;
    
    // Originally a simple string indicating the location of the archival copy. As
    // of v5.12, repurposed to provide a more general json archival status (failure,
    // pending, success) and message (serialized as a string). The archival copy
    // location is now expected as the contents of the message for the status
    // 'success'. See the /api/datasets/{id}/{version}/archivalStatus API calls for more details
    @Column(nullable=true, columnDefinition = "TEXT")
    private String archivalCopyLocation;
    
    //This is used for the deaccession reason
    @Size(min=0, max=DEACCESSION_LINK_MAX_LENGTH)
    @Column(length = DEACCESSION_LINK_MAX_LENGTH)
    private String deaccessionLink;

    @Transient
    private String contributorNames;

    @Transient
    private final String dataverseSiteUrl = SystemConfig.getDataverseSiteUrlStatic();

    @Transient 
    private String jsonLd;

    @OneToMany(mappedBy="datasetVersion", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetVersionUser> datasetVersionUsers;
    
    // Is this the right mapping and cascading for when the workflowcomments table is being used for objects other than DatasetVersion?
    @OneToMany(mappedBy = "datasetVersion", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<WorkflowComment> workflowComments;

    @Column(nullable=true)
    private String externalStatusLabel;
    
    @Transient
    private DatasetVersionDifference dvd;
    
    @Transient 
    private JsonObject archivalStatus;
    
    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUNF() {
        return UNF;
    }

    public void setUNF(String UNF) {
        this.UNF = UNF;
    }

    /**
     * This is JPA's optimistic locking mechanism, and has no semantic meaning in the DV object model.
     * @return the object db version
     */
    public Long getVersion() {
        return this.version;
    }

    public void setVersion(Long version) {
    }

    public String getDataverseSiteUrl() {
        return dataverseSiteUrl;
    }

    public List<FileMetadata> getFileMetadatas() {
        return fileMetadatas;
    }
    
    public List<FileMetadata> getFileMetadatasSorted() {
 
        /*
         * fileMetadatas can sometimes be an
         * org.eclipse.persistence.indirection.IndirectList When that happens, the
         * comparator in the Collections.sort below is not called, possibly due to
         * https://bugs.eclipse.org/bugs/show_bug.cgi?id=446236 which is Java 1.8+
         * specific Converting to an ArrayList solves the problem, but the longer term
         * solution may be in avoiding the IndirectList or moving to a new version of
         * the jar it is in.
         */
        if(!(fileMetadatas instanceof ArrayList)) {
            List<FileMetadata> newFMDs = new ArrayList<FileMetadata>();
            for(FileMetadata fmd: fileMetadatas) {
                newFMDs.add(fmd);
            }
            setFileMetadatas(newFMDs);
        }
        
        DataFileComparator dfc = new DataFileComparator();
        Collections.sort(fileMetadatas, dfc.compareBy(true, null!=FileMetadata.getCategorySortOrder(), "name", true));
        return fileMetadatas;
    }
    
    public List<FileMetadata> getFileMetadatasSortedByLabelAndFolder() {
        ArrayList<FileMetadata> fileMetadatasCopy = new ArrayList<>();
        fileMetadatasCopy.addAll(fileMetadatas);
        DataFileComparator dfc = new DataFileComparator();
        Collections.sort(fileMetadatasCopy, dfc.compareBy(true, null!=FileMetadata.getCategorySortOrder(), "name", true));
        return fileMetadatasCopy;
    }
    
    public List<FileMetadata> getFileMetadatasFolderListing(String folderName) {
        ArrayList<FileMetadata> fileMetadatasCopy = new ArrayList<>();
        HashSet<String> subFolders = new HashSet<>();

        for (FileMetadata fileMetadata : fileMetadatas) {
            String thisFolder = fileMetadata.getDirectoryLabel() == null ? "" : fileMetadata.getDirectoryLabel(); 
            
            if (folderName.equals(thisFolder)) {
                fileMetadatasCopy.add(fileMetadata);
            } else if (thisFolder.startsWith(folderName)) {
                String subFolder = "".equals(folderName) ? thisFolder : thisFolder.substring(folderName.length() + 1);
                if (subFolder.indexOf('/') > 0) {
                    subFolder = subFolder.substring(0, subFolder.indexOf('/'));
                }
                
                if (!subFolders.contains(subFolder)) {
                    fileMetadatasCopy.add(fileMetadata);
                    subFolders.add(subFolder);
                }
                
            }
        }
        Collections.sort(fileMetadatasCopy, FileMetadata.compareByFullPath);
                
        return fileMetadatasCopy; 
    }

    public void setFileMetadatas(List<FileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
    }
    
    public TermsOfUseAndAccess getTermsOfUseAndAccess() {
        return termsOfUseAndAccess;
    }

    public void setTermsOfUseAndAccess(TermsOfUseAndAccess termsOfUseAndAccess) {
        this.termsOfUseAndAccess = termsOfUseAndAccess;
    }

    public List<DatasetField> getDatasetFields() {
        return datasetFields;
    }

    /**
     * Sets the dataset fields for this version. Also updates the fields to 
     * have @{code this} as their dataset version.
     * @param datasetFields
     */
    public void setDatasetFields(List<DatasetField> datasetFields) {
        for ( DatasetField dsf : datasetFields ) {
            dsf.setDatasetVersion(this);
        }
        this.datasetFields = datasetFields;
    }
    
    /**
     * The only time a dataset can be in review is when it is in draft.
     * @return if the dataset is being reviewed
     */
    public boolean isInReview() {
        if (versionState != null && versionState.equals(VersionState.DRAFT)) {
            return getDataset().isLockedFor(DatasetLock.Reason.InReview);
        } else {
            return false;
        }
    }

    public Date getArchiveTime() {
        return archiveTime;
    }

    public void setArchiveTime(Date archiveTime) {
        this.archiveTime = archiveTime;
    }

    public String getArchivalCopyLocation() {
        return archivalCopyLocation;
    }
    
    public String getArchivalCopyLocationStatus() {
        populateArchivalStatus(false);
        
        if(archivalStatus!=null) {
            return archivalStatus.getString(ARCHIVAL_STATUS);
        } 
        return null;
    }
    public String getArchivalCopyLocationMessage() {
        populateArchivalStatus(false);
        if(archivalStatus!=null) {
            return archivalStatus.getString(ARCHIVAL_STATUS_MESSAGE);
        } 
        return null;
    }
    
    private void populateArchivalStatus(boolean force) {
        if(archivalStatus ==null || force) {
            if(archivalCopyLocation!=null) {
                try {
            archivalStatus = JsonUtil.getJsonObject(archivalCopyLocation);
                } catch(Exception e) {
                    logger.warning("DatasetVersion id: " + id + "has a non-JsonObject value, parsing error: " + e.getMessage());
                    logger.fine(archivalCopyLocation);
                }
            }
        }
    }

    public void setArchivalCopyLocation(String location) {
        this.archivalCopyLocation = location;
        populateArchivalStatus(true);
    }

    public String getDeaccessionLink() {
        return deaccessionLink;
    }

    public void setDeaccessionLink(String deaccessionLink) {
        if (deaccessionLink != null && deaccessionLink.length() > DEACCESSION_LINK_MAX_LENGTH) {
            throw new IllegalArgumentException("Error setting deaccessionLink: String length is greater than maximum (" + DEACCESSION_LINK_MAX_LENGTH + ")."
                    + "  StudyVersion id=" + id + ", deaccessionLink=" + deaccessionLink);
        }
        this.deaccessionLink = deaccessionLink;
    }

    public String getDeaccessionLinkAsURLString() {
        String dLink = null;
        try {
            dLink = new URI(deaccessionLink).toURL().toExternalForm();
        } catch (URISyntaxException | MalformedURLException e) {
            logger.fine("Invalid deaccessionLink - not a URL: " + deaccessionLink);
        }
        return dLink;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        if (createTime == null) {
            createTime = lastUpdateTime;
        }
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getVersionDate() {
        if (this.lastUpdateTime == null){
            return null; 
        }
        return DateUtil.formatDate(lastUpdateTime);
    }

    public String getVersionYear() {
        return new SimpleDateFormat("yyyy").format(lastUpdateTime);
    }

    public Date getReleaseTime() {
        return releaseTime;
    }

    public void setReleaseTime(Date releaseTime) {
        this.releaseTime = releaseTime;
    }

    public List<DatasetVersionUser> getDatasetVersionUsers() {
        return datasetVersionUsers;
    }

    public void setUserDatasets(List<DatasetVersionUser> datasetVersionUsers) {
        this.datasetVersionUsers = datasetVersionUsers;
    }

    public List<String> getVersionContributorIdentifiers() {
        if (this.getDatasetVersionUsers() == null) {
            return Collections.emptyList();
        }
        List<String> ret = new LinkedList<>();
        for (DatasetVersionUser contributor : this.getDatasetVersionUsers()) {
            ret.add(contributor.getAuthenticatedUser().getIdentifier());
        }
        return ret;
    }

    public String getContributorNames() {
        return contributorNames;
    }

    public void setContributorNames(String contributorNames) {
        this.contributorNames = contributorNames;
    }

 
    public String getDeaccessionNote() {
        return deaccessionNote;
    }

    public DatasetVersionDifference getDefaultVersionDifference() {
        //Cache to avoid recalculating the difference many many times in the dataset-versions.xhtml page
        if(dvd!=null) {
            return dvd;
        }
        // if version is deaccessioned ignore it for differences purposes
        int index = 0;
        int size = this.getDataset().getVersions().size();
        if (this.isDeaccessioned()) {
            return null;
        }
        for (DatasetVersion dsv : this.getDataset().getVersions()) {
            if (this.equals(dsv)) {
                if ((index + 1) <= (size - 1)) {
                    for (DatasetVersion dvTest : this.getDataset().getVersions().subList(index + 1, size)) {
                        if (!dvTest.isDeaccessioned()) {
                            dvd = new DatasetVersionDifference(this, dvTest);
                            return dvd;
                        }
                    }
                }
            }
            index++;
        }
        return null;
    }
    

    public VersionState getPriorVersionState() {
        int index = 0;
        int size = this.getDataset().getVersions().size();
        if (this.isDeaccessioned()) {
            return null;
        }
        for (DatasetVersion dsv : this.getDataset().getVersions()) {
            if (this.equals(dsv)) {
                if ((index + 1) <= (size - 1)) {
                    for (DatasetVersion dvTest : this.getDataset().getVersions().subList(index + 1, size)) {
                        return dvTest.getVersionState();
                    }
                }
            }
            index++;
        }
        return null;
    }

    public void setDeaccessionNote(String note) {
        if (note != null && note.length() > DEACCESSION_NOTE_MAX_LENGTH) {
            throw new IllegalArgumentException("Error setting deaccessionNote: String length is greater than maximum (" + DEACCESSION_NOTE_MAX_LENGTH + ")."
                    + "  StudyVersion id=" + id + ", deaccessionNote=" + note);
        }
        this.deaccessionNote = note;
    }
   
    public Long getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Long versionNumber) {
        this.versionNumber = versionNumber;
    }

    public Long getMinorVersionNumber() {
        return minorVersionNumber;
    }

    public void setMinorVersionNumber(Long minorVersionNumber) {
        this.minorVersionNumber = minorVersionNumber;
    }
    
    public String getFriendlyVersionNumber(){
        if (this.isDraft()) {
            return "DRAFT";
        } else {
            return versionNumber.toString() + "." + minorVersionNumber.toString();                    
        }
    }

    public VersionState getVersionState() {
        return versionState;
    }

    public void setVersionState(VersionState versionState) {
        this.versionState = versionState;
    }

    public boolean isReleased() {
        return versionState.equals(VersionState.RELEASED);
    }

    public boolean isPublished() {
        return isReleased();
    }

    public boolean isDraft() {
        return versionState.equals(VersionState.DRAFT);
    }

    public boolean isWorkingCopy() {
        return versionState.equals(VersionState.DRAFT);
    }

    public boolean isArchived() {
        return versionState.equals(VersionState.ARCHIVED);
    }

    public boolean isDeaccessioned() {
        return versionState.equals(VersionState.DEACCESSIONED);
    }

    public boolean isRetiredCopy() {
        return (versionState.equals(VersionState.ARCHIVED) || versionState.equals(VersionState.DEACCESSIONED));
    }

    public boolean isMinorUpdate() {
        if (this.dataset.getLatestVersion().isWorkingCopy()) {
            if (this.dataset.getVersions().size() > 1 && this.dataset.getVersions().get(1) != null) {
                if (this.dataset.getVersions().get(1).isDeaccessioned()) {
                    return false;
                }
            }
        }
        if (this.getDataset().getReleasedVersion() != null) {
            if (this.getFileMetadatas().size() != this.getDataset().getReleasedVersion().getFileMetadatas().size()){
                return false;
            } else {
                List <DataFile> current = new ArrayList<>();
                List <DataFile> previous = new ArrayList<>();
                for (FileMetadata fmdc : this.getFileMetadatas()){
                    current.add(fmdc.getDataFile());
                }
                for (FileMetadata fmdc : this.getDataset().getReleasedVersion().getFileMetadatas()){
                    previous.add(fmdc.getDataFile());
                }
                for (DataFile fmd: current){
                    previous.remove(fmd);
                }
                return previous.isEmpty();                
            }           
        }
        return true;
    }
    
    public boolean isHasPackageFile(){
        if (this.fileMetadatas.isEmpty()){
            return false;
        }
        if(this.fileMetadatas.size() > 1){
            return false;
        }
        return this.fileMetadatas.get(0).getDataFile().getContentType().equals(DataFileServiceBean.MIME_TYPE_PACKAGE_FILE);
    }

    public boolean isHasNonPackageFile(){
        if (this.fileMetadatas.isEmpty()){
            return false;
        }
        // The presence of any non-package file means that HTTP Upload was used (no mixing allowed) so we just check the first file.
        return !this.fileMetadatas.get(0).getDataFile().getContentType().equals(DataFileServiceBean.MIME_TYPE_PACKAGE_FILE);
    }
    
    public boolean isHasRestrictedFile(){
        if (this.fileMetadatas == null || this.fileMetadatas.isEmpty()){
            return false;
        }
        return this.fileMetadatas.stream().anyMatch(fm -> (fm.isRestricted()));
    }

    public void updateDefaultValuesFromTemplate(Template template) {
        if (!template.getDatasetFields().isEmpty()) {
            this.setDatasetFields(this.copyDatasetFields(template.getDatasetFields()));
        }
        if (template.getTermsOfUseAndAccess() != null) {
            TermsOfUseAndAccess terms = template.getTermsOfUseAndAccess().copyTermsOfUseAndAccess();
            terms.setDatasetVersion(this);
            this.setTermsOfUseAndAccess(terms);
        }
    }
    
    public DatasetVersion cloneDatasetVersion(){
        DatasetVersion dsv = new DatasetVersion();
        dsv.setVersionState(this.getPriorVersionState());
        dsv.setFileMetadatas(new ArrayList<>());
        
           if (this.getUNF() != null){
                dsv.setUNF(this.getUNF());
            }
            
            if (this.getDatasetFields() != null && !this.getDatasetFields().isEmpty()) {
                dsv.setDatasetFields(dsv.copyDatasetFields(this.getDatasetFields()));
            }
            
            /*
            adding file metadatas here and updating terms
            because the terms need to know about the files
            in a pre-save validation SEK 12/6/2021
            */
            
            for (FileMetadata fm : this.getFileMetadatas()) {
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
            
            if (this.getTermsOfUseAndAccess()!= null){
                TermsOfUseAndAccess terms = this.getTermsOfUseAndAccess().copyTermsOfUseAndAccess();
                terms.setDatasetVersion(dsv);
                dsv.setTermsOfUseAndAccess(terms);
            } else {
                TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
                terms.setDatasetVersion(dsv);
               // terms.setLicense(TermsOfUseAndAccess.License.CC0);
                dsv.setTermsOfUseAndAccess(terms);
            }

        dsv.setDataset(this.getDataset());
        return dsv;
    }

    public void initDefaultValues(License license) {
        //first clear then initialize - in case values were present 
        // from template or user entry
        this.setDatasetFields(new ArrayList<>());
        this.setDatasetFields(this.initDatasetFields());
        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
        terms.setDatasetVersion(this);
        terms.setLicense(license);
        terms.setFileAccessRequest(true);
        this.setTermsOfUseAndAccess(terms);

    }

    public DatasetVersion getMostRecentlyReleasedVersion() {
        if (this.isReleased()) {
            return this;
        } else {
            if (this.getDataset().isReleased()) {
                for (DatasetVersion testVersion : this.dataset.getVersions()) {
                    if (testVersion.isReleased()) {
                        return testVersion;
                    }
                }
            }
        }
        return null;
    }

    public DatasetVersion getLargestMinorRelease() {
        if (this.getDataset().isReleased()) {
            for (DatasetVersion testVersion : this.dataset.getVersions()) {
                if (testVersion.getVersionNumber() != null && testVersion.getVersionNumber().equals(this.getVersionNumber())) {
                    return testVersion;
                }
            }
        }

        return this;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DatasetVersion)) {
            return false;
        }
        DatasetVersion other = (DatasetVersion) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "[DatasetVersion id:" + getId() + "]";
    }

    public boolean isLatestVersion() {
        return this.equals(this.getDataset().getLatestVersion());
    }

    public String getTitle() {
        String retVal = "";
        for (DatasetField dsfv : this.getDatasetFields()) {
            if (dsfv.getDatasetFieldType().getName().equals(DatasetFieldConstant.title)) {
                retVal = dsfv.getDisplayValue();
            }
        }
        return retVal;
    }

    public String getProductionDate() {
        String retVal = null;
        for (DatasetField dsfv : this.getDatasetFields()) {
            if (dsfv.getDatasetFieldType().getName().equals(DatasetFieldConstant.productionDate)) {
                retVal = dsfv.getDisplayValue();
            }
        }
        return retVal;
    }

    /**
     * @return A string with the description of the dataset as-is from the
     * database (if available, or empty string) without passing it through
     * methods such as stripAllTags, sanitizeBasicHTML or similar.
     */
    public String getDescription() {
        for (DatasetField dsf : this.getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.description)) {
                String descriptionString = "";
                if (dsf.getDatasetFieldCompoundValues() != null && dsf.getDatasetFieldCompoundValues().get(0) != null) {
                    DatasetFieldCompoundValue descriptionValue = dsf.getDatasetFieldCompoundValues().get(0);
                    for (DatasetField subField : descriptionValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.descriptionText) && !subField.isEmptyForDisplay()) {
                            descriptionString = subField.getValue();
                        }
                    }
                }
                logger.log(Level.FINE, "pristine description: {0}", descriptionString);
                return descriptionString;
            }
        }
        return "";
    }

    public List<String> getDescriptions() {
        List<String> descriptions = new ArrayList<>();
        for (DatasetField dsf : this.getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.description)) {
                String descriptionString = "";
                if (dsf.getDatasetFieldCompoundValues() != null && !dsf.getDatasetFieldCompoundValues().isEmpty()) {
                    for (DatasetFieldCompoundValue descriptionValue : dsf.getDatasetFieldCompoundValues()) {
                        for (DatasetField subField : descriptionValue.getChildDatasetFields()) {
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.descriptionText) && !subField.isEmptyForDisplay()) {
                                descriptionString = subField.getValue();
                            }
                        }
                        logger.log(Level.FINE, "pristine description: {0}", descriptionString);
                        descriptions.add(descriptionString);
                    }
                }
            }
        }
        return descriptions;
    }

    /**
     * @return Strip out all A string with the description of the dataset that
     * has been passed through the stripAllTags method to remove all HTML tags.
     */
    public String getDescriptionPlainText() {
        return MarkupChecker.stripAllTags(getDescription());
    }

    /* This method is (only) used in creating schema.org json-jd where Google requires a text description <5000 chars.
     * 
     * @returns - a single string composed of all descriptions (joined with \n if more than one) truncated with a trailing '...' if >=5000 chars
     */
    public String getDescriptionsPlainTextTruncated() {
        List<String> plainTextDescriptions = new ArrayList<String>();
        
        for (String htmlDescription : getDescriptions()) {
            plainTextDescriptions.add(MarkupChecker.stripAllTags(htmlDescription));
        }
        String description = String.join("\n", plainTextDescriptions);
        if (description.length() >= 5000) {
            int endIndex = description.substring(0, 4997).lastIndexOf(" ");
            if (endIndex == -1) {
                //There are no spaces so just break anyway
                endIndex = 4997;
            }
            description = description.substring(0, endIndex) + "...";
        }
        return description;
    }

    /**
     * @return A string with the description of the dataset that has been passed
     * through the escapeHtml method to change the "less than" sign to "&lt;"
     * for example.
     */
    public String getDescriptionHtmlEscaped() {
        return MarkupChecker.escapeHtml(getDescription());
    }

    public List<String[]> getDatasetContacts() {
        boolean getDisplayValues = true;
        return getDatasetContacts(getDisplayValues);
    }

    /**
     * @param getDisplayValues Instead of the retrieving pristine value in the
     * database, run the value through special formatting.
     */
    public List<String[]> getDatasetContacts(boolean getDisplayValues) {
        List <String[]> retList = new ArrayList<>();
        for (DatasetField dsf : this.getDatasetFields()) {
            Boolean addContributor = true;
            String contributorName = "";
            String contributorAffiliation = "";
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContact)) {
                for (DatasetFieldCompoundValue authorValue : dsf.getDatasetFieldCompoundValues()) {
                    for (DatasetField subField : authorValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactName)) {
                            if (subField.isEmptyForDisplay()) {
                                addContributor = false;
                            }
                            // There is no use case yet for getting the non-display value for contributorName.
                            contributorName = subField.getDisplayValue();
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactAffiliation)) {
                            contributorAffiliation = getDisplayValues ? subField.getDisplayValue() : subField.getValue();
                        }

                    }
                    if (addContributor) {
                        String[] datasetContributor = new String[] {contributorName, contributorAffiliation};
                        retList.add(datasetContributor);
                    }
                }
            }
        }       
        return retList;        
    }

    public List<String[]> getDatasetProducers(){
        List <String[]> retList = new ArrayList<>();
        for (DatasetField dsf : this.getDatasetFields()) {
            Boolean addContributor = true;
            String contributorName = "";
            String contributorAffiliation = "";
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.producer)) {
                for (DatasetFieldCompoundValue authorValue : dsf.getDatasetFieldCompoundValues()) {
                    for (DatasetField subField : authorValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.producerName)) {
                            if (subField.isEmptyForDisplay()) {
                                addContributor = false;
                            }
                            contributorName = subField.getDisplayValue();
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.producerAffiliation)) {
                            contributorAffiliation = subField.getDisplayValue();
                        }

                    }
                    if (addContributor) {
                        String[] datasetContributor = new String[] {contributorName, contributorAffiliation};
                        retList.add(datasetContributor);
                    }
                }
            }
        }       
        return retList;        
    }

    public List<DatasetAuthor> getDatasetAuthors() {
        //TODO get "List of Authors" from datasetfieldvalue table
        List <DatasetAuthor> retList = new ArrayList<>();
        for (DatasetField dsf : this.getDatasetFields()) {
            Boolean addAuthor = true;
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.author)) {
                for (DatasetFieldCompoundValue authorValue : dsf.getDatasetFieldCompoundValues()) {                   
                    DatasetAuthor datasetAuthor = new DatasetAuthor();
                    for (DatasetField subField : authorValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorName)) {
                            if (subField.isEmptyForDisplay()) {
                                addAuthor = false;
                            }
                            datasetAuthor.setName(subField);
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorAffiliation)) {
                            datasetAuthor.setAffiliation(subField);
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorIdType)){
                             datasetAuthor.setIdType(subField.getRawValue());
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorIdValue)){
                            datasetAuthor.setIdValue(subField.getDisplayValue());
                        }
                    }
                    if (addAuthor) {                       
                        retList.add(datasetAuthor);
                    }
                }
            }
        }
        return retList;
    }
    
    public List<String> getFunders() {
        List<String> retList = new ArrayList<>();
        for (DatasetField dsf : this.getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.contributor)) {
                boolean addFunder = false;
                for (DatasetFieldCompoundValue contributorValue : dsf.getDatasetFieldCompoundValues()) {
                    String contributorName = null;
                    String contributorType = null;
                    for (DatasetField subField : contributorValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.contributorName)) {
                            contributorName = subField.getDisplayValue();
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.contributorType)) {
                            contributorType = subField.getRawValue();
                        }
                    }
                    //SEK 02/12/2019 move outside loop to prevent contrib type to carry over to next contributor
                    // TODO: Consider how this will work in French, Chinese, etc.
                    if ("Funder".equals(contributorType)) {
                        retList.add(contributorName);
                    }
                }
            }
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.grantNumber)) {
                for (DatasetFieldCompoundValue grantObject : dsf.getDatasetFieldCompoundValues()) {
                    for (DatasetField subField : grantObject.getChildDatasetFields()) {
                        // It would be nice to do something with grantNumberValue (the actual number) but schema.org doesn't support it.
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.grantNumberAgency)) {
                            String grantAgency = subField.getDisplayValue();
                            if (grantAgency != null && !grantAgency.isEmpty()) {
                                retList.add(grantAgency);
                            }
                        }
                    }
                }
            }
        }
        return retList;
    }

    public List<String> getTimePeriodsCovered() {
        List <String> retList = new ArrayList<>();
        for (DatasetField dsf : this.getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.timePeriodCovered)) {
                for (DatasetFieldCompoundValue timePeriodValue : dsf.getDatasetFieldCompoundValues()) {
                    String start = "";
                    String end = "";
                    for (DatasetField subField : timePeriodValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName()
                                .equals(DatasetFieldConstant.timePeriodCoveredStart)) {
                            if (subField.isEmptyForDisplay()) {
                                start = null;
                            } else {
                                // we want to use "getValue()", as opposed to "getDisplayValue()" here -
                                // as the latter method prepends the value with the word "Start:"!
                                start = subField.getValue();
                            }
                        }
                        if (subField.getDatasetFieldType().getName()
                                .equals(DatasetFieldConstant.timePeriodCoveredEnd)) {
                            if (subField.isEmptyForDisplay()) {
                                end = null;
                            } else {
                                // see the comment above
                                end = subField.getValue();
                            }
                        }

                    }
                    if (start != null && end != null) {
                        retList.add(start + "/" + end);
                    }
                }
            }
        }
        return retList;
    }

    public List<String> getDatesOfCollection() {
        List<String> retList = new ArrayList<>();
        for (DatasetField dsf : this.getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.dateOfCollection)) {
                for (DatasetFieldCompoundValue timePeriodValue : dsf.getDatasetFieldCompoundValues()) {
                    String start = "";
                    String end = "";
                    for (DatasetField subField : timePeriodValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName()
                                .equals(DatasetFieldConstant.dateOfCollectionStart)) {
                            if (subField.isEmptyForDisplay()) {
                                start = null;
                            } else {
                                // we want to use "getValue()", as opposed to "getDisplayValue()" here - 
                                // as the latter method prepends the value with the word "Start:"!
                                start = subField.getValue();
                            }
                        }
                        if (subField.getDatasetFieldType().getName()
                                .equals(DatasetFieldConstant.dateOfCollectionEnd)) {
                            if (subField.isEmptyForDisplay()) {
                                end = null;
                            } else {
                                // see the comment above
                                end = subField.getValue();
                            }
                        }

                    }
                    if (start != null && end != null) {
                        retList.add(start + "/" + end);
                    }
                }
            }
        }       
        return retList;        
    }
    
    /**
     * @return List of Strings containing the names of the authors.
     */
    public List<String> getDatasetAuthorNames() {
        List<String> authors = new ArrayList<>();
        for (DatasetAuthor author : this.getDatasetAuthors()) {
            authors.add(author.getName().getValue());
        }
        return authors;
    }

    /**
     * @return List of Strings containing the dataset's subjects
     */
    public List<String> getDatasetSubjects() {
        List<String> subjects = new ArrayList<>();
        for (DatasetField dsf : this.getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.subject)) {
                subjects.addAll(dsf.getValues());
            }
        }
        return subjects;
    }
    
    /**
     * @return List of Strings containing the version's Topic Classifications
     */
    public List<String> getTopicClassifications() {
        return getCompoundChildFieldValues(DatasetFieldConstant.topicClassification,
                DatasetFieldConstant.topicClassValue);
    }
    
    /**
     * @return List of Strings containing the version's Kind Of Data entries
     */
    public List<String> getKindOfData() {
        List<String> kod = new ArrayList<>();
        for (DatasetField dsf : this.getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.kindOfData)) {
                kod.addAll(dsf.getValues());
            }
        }
        return kod;
    }
    
    /**
     * @return List of Strings containing the version's language entries
     */
    public List<String> getLanguages() {
        List<String> languages = new ArrayList<>();
        for (DatasetField dsf : this.getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.language)) {
                languages.addAll(dsf.getValues());
            }
        }
        return languages;
    }
    
        // TODO: consider calling the newer getSpatialCoverages method below with the commaSeparated boolean set to true.
    public List<String> getSpatialCoverages() {
        List<String> retList = new ArrayList<>();
        for (DatasetField dsf : this.getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.geographicCoverage)) {
                for (DatasetFieldCompoundValue geoValue : dsf.getDatasetFieldCompoundValues()) {
                    List<String> coverage = new ArrayList<String>();
                    for (DatasetField subField : geoValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName()
                                .equals(DatasetFieldConstant.country)) {
                            if (!subField.isEmptyForDisplay()) {
                            } else {
                                coverage.add(subField.getValue());
                            }
                        }
                        if (subField.getDatasetFieldType().getName()
                                .equals(DatasetFieldConstant.state)) {
                            if (!subField.isEmptyForDisplay()) {
                                coverage.add(subField.getValue());
                            }
                        }
                        if (subField.getDatasetFieldType().getName()
                                .equals(DatasetFieldConstant.city)) {
                            if (!subField.isEmptyForDisplay()) {
                                coverage.add(subField.getValue());
                            }
                        }
                        if (subField.getDatasetFieldType().getName()
                                .equals(DatasetFieldConstant.otherGeographicCoverage)) {
                            if (!subField.isEmptyForDisplay()) {
                                coverage.add(subField.getValue());
                            }
                        }
                    }
                    if (!coverage.isEmpty()) {
                        retList.add(String.join(",", coverage));
                    }
                }
            }
        }
        return retList;
    }
 
    public List<String> getSpatialCoverages(boolean commaSeparated) {
        List<String> retList = new ArrayList<>();
        for (DatasetField dsf : this.getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.geographicCoverage)) {
                for (DatasetFieldCompoundValue geoValue : dsf.getDatasetFieldCompoundValues()) {
                    Map<String, String> coverageHash = new HashMap<>();
                    for (DatasetField subField : geoValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.country)) {
                            if (!subField.isEmptyForDisplay()) {
                                coverageHash.put(DatasetFieldConstant.country, subField.getValue());
                            }
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.state)) {
                            if (!subField.isEmptyForDisplay()) {
                                coverageHash.put(DatasetFieldConstant.state, subField.getValue());
                            }
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.city)) {
                            if (!subField.isEmptyForDisplay()) {
                                coverageHash.put(DatasetFieldConstant.city, subField.getValue());
                            }
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.otherGeographicCoverage)) {
                            if (!subField.isEmptyForDisplay()) {
                                coverageHash.put(DatasetFieldConstant.otherGeographicCoverage, subField.getValue());
                            }
                        }
                    }
                    if (!coverageHash.isEmpty()) {
                        List<String> coverageSorted = sortSpatialCoverage(coverageHash);
                        if (commaSeparated) {
                            retList.add(String.join(", ", coverageSorted));
                        } else {
                            retList.addAll(coverageSorted);
                        }
                    }
                }
            }
        }
        return retList;
    }

    private List<String> sortSpatialCoverage(Map<String, String> hash) {
        List<String> sorted = new ArrayList<>();
        String city = hash.get(DatasetFieldConstant.city);
        if (city != null) {
            sorted.add(city);
        }
        String state = hash.get(DatasetFieldConstant.state);
        if (state != null) {
            sorted.add(state);
        }
        String country = hash.get(DatasetFieldConstant.country);
        if (country != null) {
            sorted.add(country);
        }
        String otherGeographicCoverage = hash.get(DatasetFieldConstant.otherGeographicCoverage);
        if (otherGeographicCoverage != null) {
            sorted.add(otherGeographicCoverage);
        }
        return sorted;
    }

    /**
     * @return List of Strings containing the version's Keywords
     */
    public List<String> getKeywords() {
        return getCompoundChildFieldValues(DatasetFieldConstant.keyword, DatasetFieldConstant.keywordValue);
    }
    
    public List<String> getRelatedMaterial() {
        List<String> relMaterial = new ArrayList<>();
        for (DatasetField dsf : this.getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.relatedMaterial)) {
                relMaterial.addAll(dsf.getValues());
            }
        }
        return relMaterial;
    } 
    
    public List<String> getDataSource() {
        List<String> dataSources = new ArrayList<>();
        for (DatasetField dsf : this.getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.dataSources)) {
                dataSources.addAll(dsf.getValues());
            }
        }
        return dataSources;
    }
    
    public List<String[]> getGeographicCoverage() {
        List<String[]> geoCoverages = new ArrayList<>();

        for (DatasetField dsf : this.getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.geographicCoverage)) {
                for (DatasetFieldCompoundValue geoCoverage : dsf.getDatasetFieldCompoundValues()) {
                    String country = null;
                    String state = null;
                    String city = null;
                    String other = null;
                    String[] coverageItem = null;
                    for (DatasetField subField : geoCoverage.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.country)) {
                            country = subField.getDisplayValue();
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.state)) {
                            state = subField.getDisplayValue();
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.city)) {
                            city = subField.getDisplayValue();
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.otherGeographicCoverage)) {
                            other = subField.getDisplayValue();
                        }

                        coverageItem = new String[]{country, state, city, other};
                    }
                    geoCoverages.add(coverageItem);
                }
                break;
            }
        }
        return geoCoverages;
    }

    
    public List<DatasetRelPublication> getRelatedPublications() {
        List<DatasetRelPublication> relatedPublications = new ArrayList<>();
        for (DatasetField dsf : this.getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.publication)) {
                for (DatasetFieldCompoundValue publication : dsf.getDatasetFieldCompoundValues()) {
                    DatasetRelPublication relatedPublication = new DatasetRelPublication();
                    for (DatasetField subField : publication.getChildDatasetFields()) {
                        switch (subField.getDatasetFieldType().getName()) {
                            case DatasetFieldConstant.publicationCitation:
                                relatedPublication.setText(subField.getDisplayValue());
                                break;
                            case DatasetFieldConstant.publicationURL:
                                // We have to avoid using subField.getDisplayValue() here - because the
                                // DisplayFormatType
                                // for this url metadata field is likely set up so that the display value is
                                // automatically
                                // turned into a clickable HTML HREF block, which we don't want to end in our
                                // Schema.org
                                // JSON-LD output. So we want to use the raw value of the field instead, with
                                // minimal HTML
                                // sanitation, just in case (this would be done on all URLs in
                                // getDisplayValue()).
                                String url = subField.getValue();
                                if (StringUtils.isBlank(url) || DatasetField.NA_VALUE.equals(url)) {
                                    relatedPublication.setUrl("");
                                } else {
                                    relatedPublication.setUrl(MarkupChecker.sanitizeBasicHTML(url));
                                }
                                break;
                            case DatasetFieldConstant.publicationIDType:
                                // QDR idType has a trailing : now (Aug 2021)
                                // Get value without any display modifications
                                subField.getDatasetFieldType().setDisplayFormat("#VALUE");
                                relatedPublication.setIdType(subField.getDisplayValue());
                                break;
                            case DatasetFieldConstant.publicationIDNumber:
                                // Get sanitized value without any display modifications
                                subField.getDatasetFieldType().setDisplayFormat("#VALUE");
                                relatedPublication.setIdNumber(subField.getDisplayValue());
                                break;
                            case DatasetFieldConstant.publicationRelationType:
                                List<String> values = subField.getValues_nondisplay();
                                if (!values.isEmpty()) {
                                    relatedPublication.setRelationType(values.get(0)); //only one value allowed
                                }
                                break;
                        }
                    }
                    relatedPublications.add(relatedPublication);
                }
            }
        }
        return relatedPublications;
    }
    
    /**
     * @return List of Strings containing the version's Grant Agency(ies)
     */
    public List<String> getUniqueGrantAgencyValues() {

        // Since only grant agency names are returned, use distinct() to avoid repeats
        // (e.g. if there are two grants from the same agency)
        return getCompoundChildFieldValues(DatasetFieldConstant.grantNumber, DatasetFieldConstant.grantNumberAgency)
                .stream().distinct().collect(Collectors.toList());
    }

    /**
     * @return List of Strings containing the version's series title(s)
     */
    public List<String>  getSeriesTitles() {

        List<String> seriesNames = getCompoundChildFieldValues(DatasetFieldConstant.series,
                DatasetFieldConstant.seriesName);
        if (!seriesNames.isEmpty()) {
            return seriesNames;
        } else {
            return null;
        }
    }

    /**
     * @param parentFieldName
     *            compound dataset field A (from DatasetFieldConstant.*)
     * @param childFieldName
     *            dataset field B, child field of A (from DatasetFieldConstant.*)
     * @return List of values of the child field
     */
    public List<String> getCompoundChildFieldValues(String parentFieldName, String childFieldName) {
        List<String> keywords = new ArrayList<>();
        for (DatasetField dsf : this.getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(parentFieldName)) {
                for (DatasetFieldCompoundValue keywordFieldValue : dsf.getDatasetFieldCompoundValues()) {
                    for (DatasetField subField : keywordFieldValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(childFieldName)) {
                            String keyword = subField.getValue();
                            // Field values should NOT be empty or, especially, null,
                            // - in the ideal world. But as we are realizing, they CAN 
                            // be null in real life databases. So, a check, just in case:
                            if (!StringUtil.isEmpty(keyword)) {
                                keywords.add(subField.getValue());
                            }
                        }
                    }
                }
            }
        }
        return keywords;
    }
    
    public List<String> getDatasetProducerNames(){
        List<String> producerNames = new ArrayList<String>();
        for (DatasetField dsf : this.getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.producer)) {
                for (DatasetFieldCompoundValue authorValue : dsf.getDatasetFieldCompoundValues()) {
                    for (DatasetField subField : authorValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.producerName)) {
                            producerNames.add(subField.getDisplayValue().trim());
                        }
                    }
                }
            }
        }
        return producerNames;
    }

    public String getCitation() {
        return getCitation(false);
    }

    public String getCitation(boolean html) {
        return getCitation(DataCitation.Format.Internal, html, false);
    }
    public String getCitation(boolean html, boolean anonymized) {
        return getCitation(DataCitation.Format.Internal, html, anonymized);
    }
    
    public String getCitation(DataCitation.Format format, boolean html, boolean anonymized) {
        return new DataCitation(this).toString(format, html, anonymized);
    }
    
    public Date getCitationDate() {
        DatasetField citationDate = getDatasetField(this.getDataset().getCitationDateDatasetFieldType());        
        if (citationDate != null && citationDate.getDatasetFieldType().getFieldType().equals(FieldType.DATE)){          
            try {  
                return new SimpleDateFormat("yyyy").parse( citationDate.getValue() );
            } catch (ParseException ex) {
                Logger.getLogger(DatasetVersion.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return null;
    }
    
    /**
     * @param dsfType The type of DatasetField required
     * @return the first field of type dsfType encountered.
     */
    public DatasetField getDatasetField(DatasetFieldType dsfType) {
        if (dsfType != null) {
            for (DatasetField dsf : this.getFlatDatasetFields()) {
                if (dsf.getDatasetFieldType().equals(dsfType)) {
                    return dsf;
                }
            }
        }
        return null;
    }

    public String getDistributionDate() {
        //todo get dist date from datasetfieldvalue table
        for (DatasetField dsf : this.getDatasetFields()) {
            if (DatasetFieldConstant.distributionDate.equals(dsf.getDatasetFieldType().getName())) {
                String date = dsf.getValue();
                return date;
            }
            
        }
        return null;
    }

    public String getDistributorName() {
        for (DatasetField dsf : this.getFlatDatasetFields()) {
            if (DatasetFieldConstant.distributorName.equals(dsf.getDatasetFieldType().getName())) {
                return dsf.getValue();
            }
        }
        return null;
    }

    public List<DatasetDistributor> getDatasetDistributors() {
        //todo get distributors from DatasetfieldValues
        return new ArrayList<>();
    }

    public void setDatasetDistributors(List<DatasetDistributor> distributors) {
        //todo implement
    }

    public String getDistributorNames() {
        String str = "";
        for (DatasetDistributor sd : this.getDatasetDistributors()) {
            if (str.trim().length() > 1) {
                str += ";";
            }
            str += sd.getName();
        }
        return str;
    }

    public String getAuthorsStr() {
        return getAuthorsStr(true);
    }

    public String getAuthorsStr(boolean affiliation) {
        String str = "";
        for (DatasetAuthor sa : getDatasetAuthors()) {
            if (sa.getName() == null) {
                break;
            }
            if (str.trim().length() > 1) {
                str += "; ";
            }
            str += sa.getName().getValue();
            if (affiliation) {
                if (sa.getAffiliation() != null) {
                    if (!StringUtil.isEmpty(sa.getAffiliation().getValue())) {
                        str += " (" + sa.getAffiliation().getValue() + ")";
                    }
                }
            }
        }
        return str;
    }

    // TODO: clean up init methods and get them to work, cascading all the way down.
    // right now, only work for one level of compound objects
    private DatasetField initDatasetField(DatasetField dsf) {
        if (dsf.getDatasetFieldType().isCompound()) {
            for (DatasetFieldCompoundValue cv : dsf.getDatasetFieldCompoundValues()) {
                // for each compound value; check the datasetfieldTypes associated with its type
                for (DatasetFieldType dsfType : dsf.getDatasetFieldType().getChildDatasetFieldTypes()) {
                    boolean add = true;
                    for (DatasetField subfield : cv.getChildDatasetFields()) {
                        if (dsfType.equals(subfield.getDatasetFieldType())) {
                            add = false;
                            break;
                        }
                    }

                    if (add) {
                        cv.getChildDatasetFields().add(DatasetField.createNewEmptyChildDatasetField(dsfType, cv));
                    }
                }
            }
        }

        return dsf;
    }

    public List<DatasetField> initDatasetFields() {
        //retList - Return List of values
        List<DatasetField> retList = new ArrayList<>();
        //Running into null on create new dataset
        if (this.getDatasetFields() != null) {
            for (DatasetField dsf : this.getDatasetFields()) {
                retList.add(initDatasetField(dsf));
            }
        }

        //Test to see that there are values for 
        // all fields in this dataset via metadata blocks
        //only add if not added above
        for (MetadataBlock mdb : this.getDataset().getOwner().getMetadataBlocks()) {
            for (DatasetFieldType dsfType : mdb.getDatasetFieldTypes()) {
                if (!dsfType.isSubField()) {
                    boolean add = true;
                    //don't add if already added as a val
                    for (DatasetField dsf : retList) {
                        if (dsfType.equals(dsf.getDatasetFieldType())) {
                            add = false;
                            break;
                        }
                    }

                    if (add) {
                        retList.add(DatasetField.createNewEmptyDatasetField(dsfType, this));
                    }
                }
            }
        }

        //sort via display order on dataset field
        Collections.sort(retList, DatasetField.DisplayOrder);

        return retList;
    }

    /**
     * For the current server, create link back to this Dataset
     *
     * example:
     * http://dvn-build.hmdc.harvard.edu/dataset.xhtml?id=72&versionId=25
     *
     * @param serverName
     * @param dset
     * @return
     */
    public String getReturnToDatasetURL(String serverName, Dataset dset) {
        if (serverName == null) {
            return null;
        }
        if (dset == null) {
            dset = this.getDataset();
            if (dset == null) {        // currently postgres allows this, see https://github.com/IQSS/dataverse/issues/828
                return null;
            }
        }
        return serverName + "/dataset.xhtml?id=" + dset.getId() + "&versionId=" + this.getId();
    } 

    public String getReturnToFilePageURL (String serverName, Dataset dset, DataFile dataFile){
        if (serverName == null || dataFile == null) {
            return null;
        }
        if (dset == null) {
            dset = this.getDataset();
            if (dset == null) {
                return null;
            }
        }
        return serverName + "/file.xhtml?fileId=" + dataFile.getId() + "&version=" + this.getSemanticVersion();        
    }
    
    public List<DatasetField> copyDatasetFields(List<DatasetField> copyFromList) {
        List<DatasetField> retList = new ArrayList<>();

        for (DatasetField sourceDsf : copyFromList) {
            //the copy needs to have the current version
            retList.add(sourceDsf.copy(this));
        }

        return retList;
    }


    public List<DatasetField> getFlatDatasetFields() {
        return getFlatDatasetFields(getDatasetFields());
    }

    private List<DatasetField> getFlatDatasetFields(List<DatasetField> dsfList) {
        List<DatasetField> retList = new LinkedList<>();
        for (DatasetField dsf : dsfList) {
            retList.add(dsf);
            if (dsf.getDatasetFieldType().isCompound()) {
                for (DatasetFieldCompoundValue compoundValue : dsf.getDatasetFieldCompoundValues()) {
                    retList.addAll(getFlatDatasetFields(compoundValue.getChildDatasetFields()));
                }

            }
        }
        return retList;
    }

    public String getSemanticVersion() {
        /**
         * Not prepending a "v" like "v1.1" or "v2.0" because while SemVerTag
         * was in http://semver.org/spec/v1.0.0.html but later removed in
         * http://semver.org/spec/v2.0.0.html
         *
         * See also to v or not to v · Issue #1 · mojombo/semver -
         * https://github.com/mojombo/semver/issues/1#issuecomment-2605236
         */
        if (this.isReleased()) {
            return versionNumber + "." + minorVersionNumber;
        } else if (this.isDraft()){
            return VersionState.DRAFT.toString();
        } else if (this.isDeaccessioned()){
            return versionNumber + "." + minorVersionNumber;
        } else{
            return versionNumber + "." + minorVersionNumber;            
        }
        //     return VersionState.DEACCESSIONED.name();
       // } else {
       //     return "-unkwn semantic version-";
       // }
    }

    public List<ConstraintViolation<DatasetField>> validateRequired() {
        List<ConstraintViolation<DatasetField>> returnListreturnList = new ArrayList<>();
        for (DatasetField dsf : this.getFlatDatasetFields()) {
            dsf.setValidationMessage(null); // clear out any existing validation message
            Set<ConstraintViolation<DatasetField>> constraintViolations = validator.validate(dsf);
            for (ConstraintViolation<DatasetField> constraintViolation : constraintViolations) {
                dsf.setValidationMessage(constraintViolation.getMessage());
                returnListreturnList.add(constraintViolation);
                 break; // currently only support one message, so we can break out of the loop after the first constraint violation
            }
            
        }
        return returnListreturnList;
    }
    
    public boolean isValid() {
        // first clone to leave the original untouched
        final DatasetVersion newVersion = this.cloneDatasetVersion();
        // initDatasetFields
        newVersion.setDatasetFields(newVersion.initDatasetFields());
        // remove special "N/A" values and empty values
        newVersion.removeEmptyValues();
        // check validity of present fields and detect missing mandatory fields
        return newVersion.validate().isEmpty();
    }

    private void removeEmptyValues() {
        if (this.getDatasetFields() != null) {
            for (DatasetField dsf : this.getDatasetFields()) {
                removeEmptyValues(dsf);
            }
        }
    }

    private void removeEmptyValues(DatasetField dsf) {
        if (dsf.getDatasetFieldType().isPrimitive()) { // primitive
            final Iterator<DatasetFieldValue> i = dsf.getDatasetFieldValues().iterator();
            while (i.hasNext()) {
                final String v = i.next().getValue();
                if (StringUtils.isBlank(v) || DatasetField.NA_VALUE.equals(v)) {
                    i.remove();
                }
            }
        } else {
            dsf.getDatasetFieldCompoundValues().forEach(cv -> cv.getChildDatasetFields().forEach(v -> removeEmptyValues(v)));
        }
    }

    public Set<ConstraintViolation> validate() {
        Set<ConstraintViolation> returnSet = new HashSet<>();


        for (DatasetField dsf : this.getFlatDatasetFields()) {
            dsf.setValidationMessage(null); // clear out any existing validation message
            Set<ConstraintViolation<DatasetField>> constraintViolations = validator.validate(dsf);
            for (ConstraintViolation<DatasetField> constraintViolation : constraintViolations) {
                dsf.setValidationMessage(constraintViolation.getMessage());
                returnSet.add(constraintViolation);
                break; // currently only support one message, so we can break out of the loop after the first constraint violation
            }
            for (DatasetFieldValue dsfv : dsf.getDatasetFieldValues()) {
                dsfv.setValidationMessage(null); // clear out any existing validation message
                Set<ConstraintViolation<DatasetFieldValue>> constraintViolations2 = validator.validate(dsfv);
                for (ConstraintViolation<DatasetFieldValue> constraintViolation : constraintViolations2) {
                    dsfv.setValidationMessage(constraintViolation.getMessage());
                    returnSet.add(constraintViolation);
                    break; // currently only support one message, so we can break out of the loop after the first constraint violation                    
                }
            }
        }
        List<FileMetadata> dsvfileMetadatas = this.getFileMetadatas();
        if (dsvfileMetadatas != null) {
            for (FileMetadata fileMetadata : dsvfileMetadatas) {
                Set<ConstraintViolation<FileMetadata>> constraintViolations = validator.validate(fileMetadata);
                if (constraintViolations.size() > 0) {
                    // currently only support one message
                    ConstraintViolation<FileMetadata> violation = constraintViolations.iterator().next();
                    /**
                     * @todo How can we expose this more detailed message
                     * containing the invalid value to the user?
                     */
                    String message = "Constraint violation found in FileMetadata. "
                            + violation.getMessage() + " "
                            + "The invalid value is \"" + violation.getInvalidValue().toString() + "\".";
                    logger.info(message);
                    returnSet.add(violation);
                    break; // currently only support one message, so we can break out of the loop after the first constraint violation
                }
            }
        }
        
        
        TermsOfUseAndAccess toua = this.termsOfUseAndAccess;
        //Only need to test Terms of Use and Access if there are restricted files  
        if (toua != null && this.isHasRestrictedFile()) {
            Set<ConstraintViolation<TermsOfUseAndAccess>> constraintViolations = validator.validate(toua);
            if (constraintViolations.size() > 0) {
                ConstraintViolation<TermsOfUseAndAccess> violation = constraintViolations.iterator().next();
                String message = BundleUtil.getStringFromBundle("dataset.message.toua.invalid");
                logger.info(message);
                this.termsOfUseAndAccess.setValidationMessage(message);
                returnSet.add(violation);
            }
        }

        
        return returnSet;
    }
    
    public List<WorkflowComment> getWorkflowComments() {
        return workflowComments;
    }

    /**
     * dataset publication date unpublished datasets will return an empty
     * string.
     *
     * @return String dataset publication date in ISO 8601 format (yyyy-MM-dd).
     */
    public String getPublicationDateAsString() {
        if (DatasetVersion.VersionState.DRAFT == this.getVersionState()) {
            return "";
        }
        Date rel_date = this.getReleaseTime();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        String r = fmt.format(rel_date.getTime());
        return r;
    }

    // TODO: Consider moving this comment into the Exporter code.
    // The export subsystem assumes there is only
    // one metadata export in a given format per dataset (it uses the current 
    // released (published) version. This JSON fragment is generated for a 
    // specific released version - and we can have multiple released versions. 
    // (A JSON fragment is generated for drafts as well. -- P.D.)
    // So something will need to be modified to accommodate this. -- L.A.  
    /**
     * We call the export format "Schema.org JSON-LD" and extensive Javadoc can
     * be found in {@link edu.harvard.iq.dataverse.export.SchemaDotOrgExporter}.
     */
    public String getJsonLd() {
        // We show published datasets only for "datePublished" field below.
        if (jsonLd != null) {
            return jsonLd;
        }
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("@context", "http://schema.org");
        job.add("@type", "Dataset");
        // Note that whenever you use "@id" you should also use "identifier" and vice versa.
        job.add("@id", this.getDataset().getPersistentURL());
        job.add("identifier", this.getDataset().getPersistentURL());
        job.add("name", this.getTitle());
        JsonArrayBuilder authors = Json.createArrayBuilder();
        for (DatasetAuthor datasetAuthor : this.getDatasetAuthors()) {
            JsonObjectBuilder author = Json.createObjectBuilder();
            String name = datasetAuthor.getName().getDisplayValue();
            String identifierAsUrl = datasetAuthor.getIdentifierAsUrl();
            DatasetField authorAffiliation = datasetAuthor.getAffiliation();
            String affiliation = null;
            if (authorAffiliation != null) {
                affiliation = datasetAuthor.getAffiliation().getValue();
            }
            JsonObject entity = PersonOrOrgUtil.getPersonOrOrganization(name, false, (identifierAsUrl!=null));
            String givenName= entity.containsKey("givenName") ? entity.getString("givenName"):null;
            String familyName= entity.containsKey("familyName") ? entity.getString("familyName"):null;
            
            if (entity.getBoolean("isPerson")) {
                // Person
                author.add("@type", "Person");
                if (givenName != null) {
                    author.add("givenName", givenName);
                }
                if (familyName != null) {
                    author.add("familyName", familyName);
                }
                if (!StringUtil.isEmpty(affiliation)) {
                    author.add("affiliation", Json.createObjectBuilder().add("@type", "Organization").add("name", affiliation));
                }
                //Currently all possible identifier URLs are for people not Organizations
                if(identifierAsUrl != null) {
                    author.add("sameAs", identifierAsUrl);
                    //Legacy - not sure if these are still useful
                    author.add("@id", identifierAsUrl);
                    author.add("identifier", identifierAsUrl);

                }
            } else {
                // Organization
                author.add("@type", "Organization");
                if (!StringUtil.isEmpty(affiliation)) {
                    author.add("parentOrganization", Json.createObjectBuilder().add("@type", "Organization").add("name", affiliation));
                }
            }
            // Both cases
            author.add("name", entity.getString("fullName"));
            //And add to the array
            authors.add(author);
        }
        JsonArray authorsArray = authors.build();
        /**
         * "creator" is being added along side "author" (below) as an
         * experiment. We think Google Dataset Search might like "creator"
         * better".
         */
        job.add("creator", authorsArray);
        /**
         * "author" is still here for backward compatibility. Depending on how
         * the "creator" experiment above goes, we may deprecate it in the
         * future.
         */
        job.add("author", authorsArray);
        /**
         * We are aware that there is a "datePublished" field but it means "Date
         * of first broadcast/publication." This only makes sense for a 1.0
         * version.
         *
         * TODO: Should we remove the comment above about a 1.0 version? We
         * included this "datePublished" field in Dataverse 4.8.4.
         */
        String datePublished = this.getDataset().getPublicationDateFormattedYYYYMMDD();
        if (datePublished != null) {
            job.add("datePublished", datePublished);
        }
        
         /**
         * "dateModified" is more appropriate for a version: "The date on which
         * the CreativeWork was most recently modified or when the item's entry
         * was modified within a DataFeed."
         */
        job.add("dateModified", this.getPublicationDateAsString());
        if (this.isPublished()) {
            job.add("version", this.getVersionNumber().toString());
        } else {
            // This will show "DRAFT" for drafts.
            job.add("version", this.getFriendlyVersionNumber());
        }

        String description = this.getDescriptionsPlainTextTruncated();
        job.add("description", description);

        /**
         * "keywords" - contains subject(s), datasetkeyword(s) and topicclassification(s)
         * metadata fields for the version. -- L.A. 
         * (see #2243 for details/discussion/feedback from Google)
         */
        JsonArrayBuilder keywords = Json.createArrayBuilder();
        
        for (String subject : this.getDatasetSubjects()) {
            keywords.add(subject);
        }
        
        for (String topic : this.getTopicClassifications()) {
            keywords.add(topic);
        }
        
        for (String keyword : this.getKeywords()) {
            keywords.add(keyword);
        }
        
        job.add("keywords", keywords);
        
        /**
         * citation: (multiple) related publication citation and URLs, if present.
         *
         * Schema.org allows text or a CreativeWork object. Google recommends text with
         * either the full citation or the PID URL. This code adds an object if we have
         * the citation text for the work and/or an entry in the URL field (i.e.
         * https://doi.org/...) The URL is reported as the 'url' field while the
         * citation text (which would normally include the name) is reported as 'name'
         * since there doesn't appear to be a better field ('text', which was used
         * previously, is the actual text of the creative work).
         * 
         */
        List<DatasetRelPublication> relatedPublications = getRelatedPublications();
        if (!relatedPublications.isEmpty()) {
            JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
            for (DatasetRelPublication relatedPub : relatedPublications) {
                boolean addToArray = false;
                String pubCitation = relatedPub.getText();
                String pubUrl = relatedPub.getUrl();
                if (pubCitation != null || pubUrl != null) {
                    addToArray = true;
                }
                JsonObjectBuilder citationEntry = Json.createObjectBuilder();
                citationEntry.add("@type", "CreativeWork");
                if (pubCitation != null) {
                    citationEntry.add("name", pubCitation);
                }
                if (pubUrl != null) {
                    citationEntry.add("@id", pubUrl);
                    citationEntry.add("identifier", pubUrl);
                    citationEntry.add("url", pubUrl);
                }
                if (addToArray) {
                    jsonArrayBuilder.add(citationEntry);
                }
            }
            JsonArray jsonArray = jsonArrayBuilder.build();
            if (!jsonArray.isEmpty()) {
                job.add("citation", jsonArray);
            }
        }
        
        /**
         * temporalCoverage:
         * (if available)
         */
        
        List<String> timePeriodsCovered = this.getTimePeriodsCovered();
        if (timePeriodsCovered.size() > 0) {
            JsonArrayBuilder temporalCoverage = Json.createArrayBuilder();
            for (String timePeriod : timePeriodsCovered) {
                temporalCoverage.add(timePeriod);
            }
            job.add("temporalCoverage", temporalCoverage);
        }
        
        /**
         * https://schema.org/version/3.4/ says, "Note that schema.org release
         * numbers are not generally included when you use schema.org. In
         * contexts (e.g. related standards work) when a particular release
         * needs to be cited, this document provides the appropriate URL."
         * 
         * For the reason above we decided to take out schemaVersion but we're
         * leaving this Javadoc in here to remind us that we made this decision.
         * We used to include "https://schema.org/version/3.3" in the output for
         * "schemaVersion".
         */
        TermsOfUseAndAccess terms = this.getTermsOfUseAndAccess();
        if (terms != null) {
            job.add("license",DatasetUtil.getLicenseURI(this));
        }
        
        String installationBrandName = BrandingUtil.getInstallationBrandName();
        
        job.add("includedInDataCatalog", Json.createObjectBuilder()
                .add("@type", "DataCatalog")
                .add("name", installationBrandName)
                .add("url", SystemConfig.getDataverseSiteUrlStatic())
        );
        
        /**
         * Both "publisher" and "provider" are included but they have the same
         * values. Some services seem to prefer one over the other.
         */
        job.add("publisher", Json.createObjectBuilder()
                .add("@type", "Organization")
                .add("name", installationBrandName)
        );
        job.add("provider", Json.createObjectBuilder()
                .add("@type", "Organization")
                .add("name", installationBrandName)
        );

        List<String> funderNames = getFunders();
        if (!funderNames.isEmpty()) {
            JsonArrayBuilder funderArray = Json.createArrayBuilder();
            for (String funderName : funderNames) {
                JsonObjectBuilder funder = NullSafeJsonBuilder.jsonObjectBuilder();
                funder.add("@type", "Organization");
                funder.add("name", funderName);
                funderArray.add(funder);
            }
            job.add("funder", funderArray);
        }

        boolean commaSeparated = true;
        List<String> spatialCoverages = getSpatialCoverages(commaSeparated);
        if (!spatialCoverages.isEmpty()) {
            JsonArrayBuilder spatialArray = Json.createArrayBuilder();
            for (String spatialCoverage : spatialCoverages) {
                spatialArray.add(spatialCoverage);
            }
            job.add("spatialCoverage", spatialArray);
        }

        List<FileMetadata> fileMetadatasSorted = getFileMetadatasSorted();
        if (fileMetadatasSorted != null && !fileMetadatasSorted.isEmpty()) {
            JsonArrayBuilder fileArray = Json.createArrayBuilder();
            String dataverseSiteUrl = SystemConfig.getDataverseSiteUrlStatic();
            for (FileMetadata fileMetadata : fileMetadatasSorted) {
                JsonObjectBuilder fileObject = NullSafeJsonBuilder.jsonObjectBuilder();
                String filePidUrlAsString = null;
                GlobalId gid = fileMetadata.getDataFile().getGlobalId();
                filePidUrlAsString = gid != null ? gid.asURL() : null;
                fileObject.add("@type", "DataDownload");
                fileObject.add("name", fileMetadata.getLabel());
                fileObject.add("encodingFormat", fileMetadata.getDataFile().getContentType());
                fileObject.add("contentSize", fileMetadata.getDataFile().getFilesize());
                fileObject.add("description", fileMetadata.getDescription());
                fileObject.add("@id", filePidUrlAsString);
                fileObject.add("identifier", filePidUrlAsString);
                String hideFilesBoolean = System.getProperty(SystemConfig.FILES_HIDE_SCHEMA_DOT_ORG_DOWNLOAD_URLS);
                if (hideFilesBoolean != null && hideFilesBoolean.equals("true")) {
                    // no-op
                } else {
                    String nullDownloadType = null;
                    fileObject.add("contentUrl", dataverseSiteUrl + FileUtil.getFileDownloadUrlPath(nullDownloadType, fileMetadata.getDataFile().getId(), false, fileMetadata.getId()));
                }
                fileArray.add(fileObject);
            }
            job.add("distribution", fileArray);
        }
        jsonLd = job.build().toString();

        //Most fields above should be stripped/sanitized but, since this is output in the dataset page as header metadata, do a final sanitize step to make sure
        jsonLd = MarkupChecker.stripAllTags(jsonLd);

        return jsonLd;
    }

    public String getLocaleLastUpdateTime() {
        return DateUtil.formatDate(new Timestamp(lastUpdateTime.getTime()));
    }
    
    public String getExternalStatusLabel() {
        return externalStatusLabel;
    }

    public void setExternalStatusLabel(String externalStatusLabel) {
        this.externalStatusLabel = externalStatusLabel;
    }

    public String getVersionNote() {
        return versionNote;
    }

    public void setVersionNote(String note) {
        if (note != null && note.length() > VERSION_NOTE_MAX_LENGTH) {
            throw new IllegalArgumentException("Error setting versionNote: String length is greater than maximum (" + VERSION_NOTE_MAX_LENGTH + ")."
                    + "  StudyVersion id=" + id + ", versionNote=" + note);
        }

        this.versionNote = note;
    }
}

