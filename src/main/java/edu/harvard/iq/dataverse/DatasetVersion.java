/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

/**
 *
 * @author skraffmiller
 */
@Entity
public class DatasetVersion implements Serializable {

    // TODO: Determine the UI implications of various version states
    //IMPORTANT: If you add a new value to this enum, you will also have to modify the
    // StudyVersionsFragment.xhtml in order to display the correct value from a Resource Bundle
    public enum VersionState {

        DRAFT, IN_REVIEW, RELEASED, ARCHIVED, DEACCESSIONED
    };

    public DatasetVersion() {

    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Version
    private Long version;
    
    /**
     * This is JPA's optimistic locking mechanism, and has no semantic meaning in the DV object model.
     * @return the object db version
     */
    public Long getVersion() {
        return this.version;
    }

    public void setVersion(Long version) {
    }

    private Long versionNumber;
    private Long minorVersionNumber;
    public static final int VERSION_NOTE_MAX_LENGTH = 1000;
    @Column(length = VERSION_NOTE_MAX_LENGTH)
    private String versionNote;

    @Enumerated(EnumType.STRING)
    private VersionState versionState;

    @ManyToOne
    private Dataset dataset;

    @OneToMany(mappedBy = "datasetVersion", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("category") // this is not our preferred ordering, which is with the AlphaNumericComparator, but does allow the files to be grouped by category
    private List<FileMetadata> fileMetadatas;

    public List<FileMetadata> getFileMetadatas() {
        return fileMetadatas;
    }

    public void setFileMetadatas(List<FileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
    }

    @OneToMany(mappedBy = "datasetVersion", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    //@OrderBy("datasetField.displayOrder") 
    private List<DatasetField> datasetFields = new ArrayList();

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

    /*
     @OneToMany(mappedBy="studyVersion", cascade={CascadeType.REMOVE, CascadeType.PERSIST})
     private List<VersionContributor> versionContributors;
     */
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date createTime;
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastUpdateTime;
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date releaseTime;
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date archiveTime;
    public static final int ARCHIVE_NOTE_MAX_LENGTH = 1000;
    @Column(length = ARCHIVE_NOTE_MAX_LENGTH)
    private String archiveNote;
    private String deaccessionLink;

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
        if (note.length() > ARCHIVE_NOTE_MAX_LENGTH) {
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
    
    public String getVersionDate(){
        return new SimpleDateFormat("MMMM d, yyyy").format(lastUpdateTime);
    }
    
    public String getVersionYear(){
        return new SimpleDateFormat("yyyy").format(lastUpdateTime);
    }

    public Date getReleaseTime() {
        return releaseTime;
    }

    public void setReleaseTime(Date releaseTime) {
        this.releaseTime = releaseTime;
    }
    
    @OneToMany(mappedBy = "datasetVersion")
    private List<DatasetVersionDatasetUser> datasetVersionDataverseUsers;
    
    public List<DatasetVersionDatasetUser> getDatasetVersionDataverseUsers(){
        return datasetVersionDataverseUsers;
    }
    
    public void setUserDatasets(List<DatasetVersionDatasetUser> datasetVersionDataverseUsers){
        this.datasetVersionDataverseUsers = datasetVersionDataverseUsers;
    }
    
    public String getVersionContributors(){
        String retString = ""; 
        if (this.getDatasetVersionDataverseUsers() == null){
            return retString;
        }
        for (DatasetVersionDatasetUser contributor: this.getDatasetVersionDataverseUsers()){
             if (retString.isEmpty()){
                 retString = contributor.getDataverseUser().getDisplayName();
             } else {
                 retString += ", " + contributor.getDataverseUser().getDisplayName();
             }
        }
        if (retString.isEmpty()){
            retString = this.getDataset().getCreator().getDisplayName();
        }
        return retString;
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
    
    public VersionState getPriorVersionState(){
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

    public VersionState getVersionState() {
        return versionState;
    }

    public void setVersionState(VersionState versionState) {

        this.versionState = versionState;
    }

    public boolean isReleased() {
        return versionState.equals(VersionState.RELEASED);
    }

    public boolean isInReview() {
        return versionState.equals(VersionState.IN_REVIEW);
    }

    public boolean isDraft() {
        return versionState.equals(VersionState.DRAFT);
    }

    public boolean isWorkingCopy() {
        return (versionState.equals(VersionState.DRAFT) || versionState.equals(VersionState.IN_REVIEW));
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
        if(this.dataset.getLatestVersion().isWorkingCopy()){
            if (this.dataset.getVersions().size() > 1 && this.dataset.getVersions().get(1) != null){
                if (this.dataset.getVersions().get(1).isDeaccessioned()){
                     return false;
                }
            }
        }
        if (this.getDataset().getReleasedVersion() != null) {
            return this.getFileMetadatas().size() == this.getDataset().getReleasedVersion().getFileMetadatas().size();
        }
        return true;
    }
    
    public DatasetVersion getMostRecentlyReleasedVersion(){
        if (this.isReleased()) { 
            return this;
        } else {
            if (this.getDataset().isReleased()){
                for (DatasetVersion testVersion : this.dataset.getVersions()){
                    if(testVersion.isReleased()){
                        return testVersion; 
                    }
                }
            }
        }
        return null;
    }
    
    public DatasetVersion getLargestMinorRelease(){

            if (this.getDataset().isReleased()){
                for (DatasetVersion testVersion : this.dataset.getVersions()){
                    if(testVersion.getVersionNumber() != null && testVersion.getVersionNumber().equals(this.getVersionNumber())){
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
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "[DatasetVersion id:" + getId() + "]";
    }

    public boolean isLatestVersion() {
        return this.equals(this.getDataset().getLatestVersion());
    }

    public String getTitle() {
        String retVal = "Dataset Title";
        for (DatasetField dsfv : this.getDatasetFields()) {
            if (dsfv.getDatasetFieldType().getName().equals(DatasetFieldConstant.title)) {
                retVal = dsfv.getValue();
            }
        }
        return retVal;
    }
    
    public String getProductionDate() {
        //todo get "Production Date" from datasetfieldvalue table
        return "Production Date";
    }

    public List<DatasetAuthor> getDatasetAuthors() {
        //todo get "List of Authors" from datasetfieldvalue table
        List retList = new ArrayList();
        for (DatasetField dsf : this.getDatasetFields()) {
             if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.author)) {
                for (DatasetFieldCompoundValue authorValue : dsf.getDatasetFieldCompoundValues()) {
                    DatasetAuthor datasetAuthor = new DatasetAuthor();
                    for (DatasetField subField : authorValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorName)) {
                            datasetAuthor.setName(subField);
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorAffiliation)) {
                            datasetAuthor.setAffiliation(subField);
                        }
                    }
                    retList.add(datasetAuthor);
                }                
             } 
        }
        return retList;
    }
    
    public void setDatasetAuthors( List<DatasetAuthor> authors ) {
        // FIXME add the authores to the relevant fields
    }
    
    public String getCitation(){
        return getCitation(false);
    }
    
     public String getCitation(boolean isOnlineVersion) {


        String str = "";

        boolean includeAffiliation = false;
        String authors = this.getAuthorsStr(includeAffiliation);
        if (!StringUtil.isEmpty(authors)) {
            str += authors;
        }

        if (this.getDataset().getPublicationDate() == null || StringUtil.isEmpty(this.getDataset().getPublicationDate().toString())) {
            //if not released use current year
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            str +=  new SimpleDateFormat("yyyy").format(new Timestamp(new Date().getTime())) ;
        } else  {
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            str += new SimpleDateFormat("yyyy").format(new Timestamp(this.getDataset().getPublicationDate().getTime()));             
        } 

        if ( this.getTitle() != null ) {
            if (!StringUtil.isEmpty(this.getTitle())) {
                if (!StringUtil.isEmpty(str)) {
                    str += ", ";
                }
                str += "\"" + this.getTitle() + "\"";
            }
        }
        if (!StringUtil.isEmpty(this.getDataset().getIdentifier())) {
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            if (isOnlineVersion) {
                str += "<a href=\"" + this.getDataset().getPersistentURL() + "\">" + this.getDataset().getIdentifier() + "</a>";
            } else {
                str += this.getDataset().getPersistentURL();
            }
        }

        //Get root dataverse name for Citation
        String dataverseName = getRootDataverseNameforCitation();
        if (!StringUtil.isEmpty(dataverseName)) {
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            str += " " + dataverseName;
        } 
        
        if (this.isDraft()){
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            str += " DRAFT VERSION ";
            
        } else if (this.getVersionNumber() != null) {
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            str += " V" + this.getVersionNumber();

        }
        if (this.isDeaccessioned()){
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            str += " DEACCESSIONED VERSION ";
            
        }
        /*UNF is not calculated yet
         if (!StringUtil.isEmpty(getUNF())) {
         if (!StringUtil.isEmpty(str)) {
         str += " ";
         }
         str += getUNF();
         }
         String distributorNames = getDistributorNames();
         if (distributorNames.trim().length() > 0) {
         str += " " + distributorNames;
         str += " [Distributor]";
         }*/
        return str;
    }


    public String getDistributionDate() {
        //todo get dist date from datasetfieldvalue table
        return "Distribution Date";
    }

    public String getUNF() {
        //todo get dist date from datasetfieldvalue table
        return "UNF";
    }
    
    public String getRootDataverseNameforCitation(){
                    //Get root dataverse name for Citation
        Dataverse root = this.getDataset().getOwner();
        while (root.getOwner() != null) {
            root = root.getOwner();
        }
        String rootDataverseName = root.getName();
        if (!StringUtil.isEmpty(rootDataverseName)) {
            return rootDataverseName + " Dataverse"; 
        } else {
            return "";
        }
    }


    public List<DatasetDistributor> getDatasetDistributors() {
        //todo get distributors from DatasetfieldValues
        return new ArrayList();
    }
    
     public void setDatasetDistributors( List<DatasetDistributor> distributors) {
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

    /**
     * @todo Does this correctly include affiliation if you ask for it?
     */
    public String getAuthorsStr(boolean affiliation) {
        String str = "";
        for (DatasetAuthor sa : getDatasetAuthors()) {
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
        List<DatasetField> retList = new ArrayList();
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
     * example: http://dvn-build.hmdc.harvard.edu/dataset.xhtml?id=72&versionId=25
     * 
     * @param serverName
     * @param dset
     * @return 
     */
    public String getReturnToDatasetURL(String serverName, Dataset dset){
        if (serverName==null){
            return null;
        }
        if (dset==null){
            dset = this.getDataset();
            if (dset==null){        // currently postgres allows this, see https://github.com/IQSS/dataverse/issues/828
                return null;
            }
        }
        return serverName + "/dataset.xhtml?id=" + dset.getId() + "&versionId" + this.getId();
    };
    
    public List<DatasetField> copyDatasetFields(List<DatasetField> copyFromList) {
        List<DatasetField> retList = new ArrayList();

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
        List<DatasetField> retList = new LinkedList();
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
        if (this.isReleased()){
            return versionNumber + "." + minorVersionNumber;
        } else {
            return "DRAFT";
        }        
    }
}
