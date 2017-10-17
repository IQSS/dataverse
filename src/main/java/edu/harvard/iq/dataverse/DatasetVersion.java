package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.MarkupChecker;
import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.workflows.WorkflowComment;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

/**
 *
 * @author skraffmiller
 */
@Entity
@Table(indexes = {@Index(columnList="dataset_id")},
        uniqueConstraints = @UniqueConstraint(columnNames = {"dataset_id,versionnumber,minorversionnumber"}))
public class DatasetVersion implements Serializable {

    private static final Logger logger = Logger.getLogger(DatasetVersion.class.getCanonicalName());

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

    // TODO: Determine the UI implications of various version states
    //IMPORTANT: If you add a new value to this enum, you will also have to modify the
    // StudyVersionsFragment.xhtml in order to display the correct value from a Resource Bundle
    public enum VersionState {
        DRAFT, RELEASED, ARCHIVED, DEACCESSIONED
    };

    public enum License {
        NONE, CC0
    }

    public static final int ARCHIVE_NOTE_MAX_LENGTH = 1000;
    public static final int VERSION_NOTE_MAX_LENGTH = 1000;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String UNF;

    @Version
    private Long version;

    private Long versionNumber;
    private Long minorVersionNumber;
    
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
    
    @Column(length = ARCHIVE_NOTE_MAX_LENGTH)
    private String archiveNote;
    
    private String deaccessionLink;

    @Transient
    private String contributorNames;

    @OneToMany(mappedBy="datasetVersion", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetVersionUser> datasetVersionUsers;
    
    // Is this the right mapping and cascading for when the workflowcomments table is being used for objects other than DatasetVersion?
    @OneToMany(mappedBy = "datasetVersion", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<WorkflowComment> workflowComments;

    
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
    
    public List<FileMetadata> getFileMetadatas() {
        return fileMetadatas;
    }
    
    public List<FileMetadata> getFileMetadatasSorted() {
        Collections.sort(fileMetadatas, FileMetadata.compareByLabel);
        return fileMetadatas;
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

    public String getArchiveNote() {
        return archiveNote;
    }

    public void setArchiveNote(String note) {
        // @todo should this be using bean validation for trsting note length?
        if (note != null && note.length() > ARCHIVE_NOTE_MAX_LENGTH) {
            throw new IllegalArgumentException("Error setting archiveNote: String length is greater than maximum (" + ARCHIVE_NOTE_MAX_LENGTH + ")."
                    + "  StudyVersion id=" + id + ", archiveNote=" + note);
        }
        this.archiveNote = note;
    }

    public String getDeaccessionLink() {
        return deaccessionLink;
    }

    public void setDeaccessionLink(String deaccessionLink) {
        this.deaccessionLink = deaccessionLink;
    }

    public GlobalId getDeaccessionLinkAsGlobalId() {
        return new GlobalId(deaccessionLink);
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
        return new SimpleDateFormat("MMMM d, yyyy").format(lastUpdateTime);
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

 
    public String getVersionNote() {
        return versionNote;
    }

    public DatasetVersionDifference getDefaultVersionDifference() {
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
                            DatasetVersionDifference dvd = new DatasetVersionDifference(this, dvTest);
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

    public void setVersionNote(String note) {
        if (note != null && note.length() > VERSION_NOTE_MAX_LENGTH) {
            throw new IllegalArgumentException("Error setting versionNote: String length is greater than maximum (" + VERSION_NOTE_MAX_LENGTH + ")."
                    + "  StudyVersion id=" + id + ", versionNote=" + note);
        }
        this.versionNote = note;
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
    
    public void updateDefaultValuesFromTemplate(Template template) {
        if (!template.getDatasetFields().isEmpty()) {
            this.setDatasetFields(this.copyDatasetFields(template.getDatasetFields()));
        }
        if (template.getTermsOfUseAndAccess() != null) {
            TermsOfUseAndAccess terms = template.getTermsOfUseAndAccess().copyTermsOfUseAndAccess();
            terms.setDatasetVersion(this);
            this.setTermsOfUseAndAccess(terms);
        } else {
            TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
            terms.setDatasetVersion(this);
            terms.setLicense(TermsOfUseAndAccess.License.CC0);
            terms.setDatasetVersion(this);
            this.setTermsOfUseAndAccess(terms);
        }
    }

    public void initDefaultValues() {
        //first clear then initialize - in case values were present 
        // from template or user entry
        this.setDatasetFields(new ArrayList<>());
        this.setDatasetFields(this.initDatasetFields());
        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
        terms.setDatasetVersion(this);
        terms.setLicense(TermsOfUseAndAccess.License.CC0);
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
        //todo get "Production Date" from datasetfieldvalue table
        return "Production Date";
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
                logger.fine("pristine description: " + descriptionString);
                return descriptionString;
            }
        }
        return "";
    }

    /**
     * @return Strip out all A string with the description of the dataset that
     * has been passed through the stripAllTags method to remove all HTML tags.
     */
    public String getDescriptionPlainText() {
        return MarkupChecker.stripAllTags(getDescription());
    }

    /**
     * @return A string with the description of the dataset that has been passed
     * through the escapeHtml method to change the "less than" sign to "&lt;"
     * for example.
     */
    public String getDescriptionHtmlEscaped() {
        return MarkupChecker.escapeHtml(getDescription());
    }

    public List<String[]> getDatasetContacts(){
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
                            contributorName = subField.getDisplayValue();
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactAffiliation)) {
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
                             datasetAuthor.setIdType(subField.getDisplayValue());
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

    public String getDatasetProducersString(){
        String retVal = "";
        for (DatasetField dsf : this.getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.producer)) {
                for (DatasetFieldCompoundValue authorValue : dsf.getDatasetFieldCompoundValues()) {
                    for (DatasetField subField : authorValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.producerName)) {
                            if (retVal.isEmpty()){
                                retVal = subField.getDisplayValue();
                            } else {
                                retVal += ", " +  subField.getDisplayValue();
                            }                           
                        }
                    }
                }
            }
        }
        return retVal;
    }

    public void setDatasetAuthors(List<DatasetAuthor> authors) {
        // FIXME add the authors to the relevant fields
    }

    public String getCitation() {
        return getCitation(false);
    }

    public String getCitation(boolean html) {
        return new DataCitation(this).toString(html);
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
    
    public String getRootDataverseNameforCitation(){
                    //Get root dataverse name for Citation
        Dataverse root = this.getDataset().getOwner();
        while (root.getOwner() != null) {
            root = root.getOwner();
        }
        String rootDataverseName = root.getName();
        if (!StringUtil.isEmpty(rootDataverseName)) {
            return rootDataverseName;
        } else {
            return "";
        }
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
    
    /*
    Per #3511 we  are returning all users to the File Landing page
    If we in the future we are going to return them to the referring page we will need the 
    getReturnToDatasetURL method and add something to the call to the api to
    pass the referring page and some kind of decision point in  the getWorldMapDatafileInfo method in 
    WorldMapRelatedData
    SEK 3/24/2017
    */
    
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
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
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
    
    public Set<ConstraintViolation> validate() {
        Set<ConstraintViolation> returnSet = new HashSet<>();

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
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
        return returnSet;
    }

    public List<WorkflowComment> getWorkflowComments() {
        return workflowComments;
    }

}
