/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
//import org.springframework.format.annotation.DateTimeFormat;

/**
 *
 * @author skraffmiller
 */
@Entity
public class DatasetVersion implements Serializable {

  // TODO: Determine the UI implications of various version states
  //IMPORTANT: If you add a new value to this enum, you will also have to modify the
  // StudyVersionsFragment.xhtml in order to display the correct value from a Resource Bundle
  public enum VersionState { DRAFT, IN_REVIEW, RELEASED, ARCHIVED, DEACCESSIONED};

    public DatasetVersion () {

    }

    private Long versionNumber;
    public static final int VERSION_NOTE_MAX_LENGTH = 1000;
    @Column(length=VERSION_NOTE_MAX_LENGTH)
    private String versionNote;
    
    @Enumerated(EnumType.STRING)
    private VersionState versionState;
    
    @ManyToOne     
    private Dataset dataset;

    @OneToOne(cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(nullable=false)
    private Metadata metadata;

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }
    

    @OneToMany(mappedBy="datasetVersion", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("category") // this is not our preferred ordering, which is with the AlphaNumericComparator, but does allow the files to be grouped by category
    private List<FileMetadata> fileMetadatas; 

    public List<FileMetadata> getFileMetadatas() {
        return fileMetadatas;
    }

    public void setFileMetadatas(List<FileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
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
    @Column(length=ARCHIVE_NOTE_MAX_LENGTH)
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
        if (note.length()>ARCHIVE_NOTE_MAX_LENGTH ) {
            throw new IllegalArgumentException("Error setting archiveNote: String length is greater than maximum ("+ ARCHIVE_NOTE_MAX_LENGTH + ")."
                   +"  StudyVersion id="+id+", archiveNote="+note);
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
        if (createTime==null) {
            createTime = lastUpdateTime;
        }
        this.lastUpdateTime = lastUpdateTime;
    }

    public Date getReleaseTime() {
        return releaseTime;
    }

    public void setReleaseTime(Date releaseTime) {
        this.releaseTime = releaseTime;
    }

    public String getVersionNote() {
        return versionNote;
    }

    public void setVersionNote(String note) {
        if (note != null &&  note.length()>VERSION_NOTE_MAX_LENGTH ) {
            throw new IllegalArgumentException("Error setting versionNote: String length is greater than maximum ("+ VERSION_NOTE_MAX_LENGTH + ")."
                   +"  StudyVersion id="+id+", versionNote="+note);
        }
        this.versionNote = note;
    }

    public Long getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Long versionNumber) {
        this.versionNumber = versionNumber;
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
        return (versionState.equals(VersionState.DRAFT) ||  versionState.equals(VersionState.IN_REVIEW)) ;
    }

    public boolean isArchived() {
         return versionState.equals(VersionState.ARCHIVED);
    }

    public boolean isDeaccessioned() {
         return versionState.equals(VersionState.DEACCESSIONED);
    }

    public boolean isRetiredCopy() {
        return (versionState.equals(VersionState.ARCHIVED) ||  versionState.equals(VersionState.DEACCESSIONED)) ;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Getter for property id.
     * @return Value of property id.
     */
    public Long getId() {
        return this.id;
    }

    /**
     * Setter for property id.
     * @param id New value of property id.
     */
    public void setId(Long id) {
        this.id = id;
    }

    @Version
    private Long version;

    /**
     * Getter for property version.
     * @return Value of property version.
     */
    public Long getVersion() {
        return this.version;
    }

    /**
     * Setter for property version.
     * @param version New value of property version.
     */
    public void setVersion(Long version) {
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
        return "edu.harvard.iq.dataverse[id=" + id + "]";
    }

    public boolean isLatestVersion() {
        return true;
        //return this.equals( this.getDataset().getLatestVersion() );
    }
}
