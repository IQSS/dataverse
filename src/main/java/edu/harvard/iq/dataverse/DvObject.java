package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.persistence.*;

/**
 * Base of the object hierarchy for "anything that can be inside a dataverse".
 *
 * @author michael
 */
@NamedQueries({
    @NamedQuery(name = "DvObject.findAll",
            query = "SELECT o FROM DvObject o ORDER BY o.id"),
    @NamedQuery(name = "DvObject.findById",
            query = "SELECT o FROM DvObject o WHERE o.id=:id"),
	@NamedQuery(name = "DvObject.ownedObjectsById",
			query="SELECT COUNT(obj) FROM DvObject obj WHERE obj.owner.id=:id")
})
@Entity
// Inheritance strategy "JOINED" will create 4 db tables - 
// the top-level dvobject, with the common columns, and the 3 child classes - 
// dataverse, dataset and datafile. The ids from the main table will be reused
// in the child tables. (i.e., the id sequences will be "sparse" in the 3 
// child tables). Tested, appears to be working properly. -- L.A. Nov. 4 2014
@Inheritance(strategy=InheritanceType.JOINED)
@Table(indexes = {@Index(columnList="dtype")
		, @Index(columnList="owner_id")
		, @Index(columnList="creator_id")
		, @Index(columnList="releaseuser_id")})
public abstract class DvObject extends DataverseEntity implements java.io.Serializable {
    
    public static final String DATAVERSE_DTYPE_STRING = "Dataverse";
    public static final String DATASET_DTYPE_STRING = "Dataset";
    public static final String DATAFILE_DTYPE_STRING = "DataFile";
    public static final List<String> DTYPE_LIST = Arrays.asList(DATAVERSE_DTYPE_STRING, DATASET_DTYPE_STRING, DATAFILE_DTYPE_STRING);
    
    public static final Visitor<String> NamePrinter = new Visitor<String>(){

        @Override
        public String visit(Dataverse dv) {
            return dv.getName();
        }

        @Override
        public String visit(Dataset ds) {
            return ds.getLatestVersion().getTitle();
        }

        @Override
        public String visit(DataFile df) {
            return df.getFileMetadata().getLabel();
        }
    };
    public static final Visitor<String> NameIdPrinter = new Visitor<String>(){

        @Override
        public String visit(Dataverse dv) {
            return "[" + dv.getId() + " " + dv.getName() + "]";
        }

        @Override
        public String visit(Dataset ds) {
            return "[" + ds.getId() + (ds.getLatestVersion() != null ? " " + ds.getLatestVersion().getTitle() : "") + "]";
        }

        @Override
        public String visit(DataFile df) {
            return "[" + df.getId() + (df.getFileMetadata() != null ? " " + df.getFileMetadata().getLabel() : "") + "]";
        }
    };
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private DvObject owner;

    private Timestamp publicationDate;

    /** The user that released this dataverse */
    @ManyToOne
    private AuthenticatedUser releaseUser;
    
    @Column( nullable = false )
    private Timestamp createDate;

    @Column(nullable = false)
    private Timestamp modificationTime;

    /**
     * @todo Rename this to contentIndexTime (or something) to differentiate it
     * from permissionIndexTime. Content Solr docs vs. permission Solr docs.
     */
    private Timestamp indexTime;

    /**
     * @todo Make this nullable=true. Currently we can't because the
     * CreateDataverseCommand saves the dataverse before it assigns a role.
     */
    @Column(nullable = true)
    private Timestamp permissionModificationTime;

    private Timestamp permissionIndexTime;

    /**
     * previewImageAvailable could also be thought of as "thumbnail has been
     * generated. However, were all three thumbnails generated? We might need a
     * boolean per thumbnail size.
     */
    private boolean previewImageAvailable;
    
    public boolean isPreviewImageAvailable() {
        return previewImageAvailable;
    }
    
    public void setPreviewImageAvailable(boolean status) {
        this.previewImageAvailable = status;
    }

    public Timestamp getModificationTime() {
        return modificationTime;
    }

    /**
     * modificationTime is used for comparison with indexTime so we know if the
     * Solr index is stale.
     * @param modificationTime
     */
    public void setModificationTime(Timestamp modificationTime) {
        this.modificationTime = modificationTime;
    }

    public Timestamp getIndexTime() {
        return indexTime;
    }

    /**
     * indexTime is used for comparison with modificationTime so we know if the
     * Solr index is stale.
     * @param indexTime
     */
    public void setIndexTime(Timestamp indexTime) {
        this.indexTime = indexTime;
    }

    @ManyToOne
    private AuthenticatedUser creator;

    public interface Visitor<T> {
        public T visit(Dataverse dv);
        public T visit(Dataset   ds);
        public T visit(DataFile  df);
    }

    /**
     * Sets the owner of the object. This is {@code protected} rather than
     * {@code public}, since different sub-classes have different possible owner
     * types: a {@link DataFile} can only have a {@link Dataset}, for example.
     *
     * @param newOwner
     */
    protected void setOwner(DvObjectContainer newOwner) {
        owner = newOwner;
    }

    public DvObjectContainer getOwner() {
        return (DvObjectContainer)owner;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    /**
     * @return Whether {@code this} takes no permissions from roles assigned on its parents.
     */
    public abstract boolean isEffectivelyPermissionRoot();

    public Timestamp getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(Timestamp publicationDate) {
        this.publicationDate = publicationDate;
    }

    public AuthenticatedUser getReleaseUser() {
        return releaseUser;
    }
    
    public void setReleaseUser(AuthenticatedUser releaseUser) {
        this.releaseUser = releaseUser;
    }

    public boolean isReleased() {
        return publicationDate != null;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public AuthenticatedUser getCreator() {
        return creator;
    }

    public void setCreator(AuthenticatedUser creator) {
        this.creator = creator;
    }
    
    public abstract <T> T accept(Visitor<T> v);

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public String toString() {
        String classNameComps[] = getClass().getName().split("\\.");
        return String.format("[%s id:%d %s]", classNameComps[classNameComps.length - 1],
                getId(), toStringExtras());
    }

    /**
     * Convenience method to add data to the default toString output.
     *
     * @return
     */
    protected String toStringExtras() {
        return "";
    }
    
    public abstract String getDisplayName();
    
    // helper method used to mimic instanceof on JSF pge
    public boolean isInstanceofDataverse() {
        return this instanceof Dataverse;
    }        

    public boolean isInstanceofDataset() {
        return this instanceof Dataset;
    }
    
    public boolean isInstanceofDataFile() {
        return this instanceof DataFile;
    }

    public Timestamp getPermissionModificationTime() {
        return permissionModificationTime;
    }

    public void setPermissionModificationTime(Timestamp permissionModificationTime) {
        this.permissionModificationTime = permissionModificationTime;
    }

    public Timestamp getPermissionIndexTime() {
        return permissionIndexTime;
    }

    public void setPermissionIndexTime(Timestamp permissionIndexTime) {
        this.permissionIndexTime = permissionIndexTime;
    }

    public Dataverse getDataverseContext() {
        if (this instanceof Dataverse) {
            return (Dataverse) this;
        } else if (this.getOwner() != null){
            return this.getOwner().getDataverseContext();
        }
        
        return null;
    }    
    
    /**
     * 
     * @param other 
     * @return {@code true} iff {@code other} is {@code this} or below {@code this} in the containment hierarchy.
     */
    public abstract boolean isAncestorOf( DvObject other );
    
    @OneToMany(mappedBy = "definitionPoint",cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST}, orphanRemoval=true)
    List<RoleAssignment> roleAssignments;
}
