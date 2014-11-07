package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import java.sql.Timestamp;
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
//@Inheritance(strategy=InheritanceType.JOINED)
public abstract class DvObject implements java.io.Serializable {
    
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
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = CascadeType.MERGE)
    private DvObjectContainer owner;

    private Timestamp publicationDate;

    private String releaseUserIdtf;
    
    private Timestamp createDate;

    @ManyToOne
    private BuiltinUser creator;

    public interface Visitor<T> {
        public T visit(Dataverse dv);
        public T visit(Dataset   ds);
        public T visit(DataFile  df);
    }

    /**
     * Sets the owner of the object. This is {@code protected} rather than
     * {@code public}, since different sub-classes have different possible owner
     * types: a {@link DataFile} can only have a {@link DataSet}, for example.
     *
     * @param newOwner
     */
    protected void setOwner(DvObjectContainer newOwner) {
        owner = newOwner;
    }

    public DvObjectContainer getOwner() {
        return owner;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Timestamp getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(Timestamp publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getReleaseUserIdentifier() {
        return releaseUserIdtf;
    }

    public void setReleaseUserIdentifier(String releaseUserIdtf) {
        this.releaseUserIdtf = releaseUserIdtf;
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

    public BuiltinUser getCreator() {
        return creator;
    }

    public void setCreator(BuiltinUser creator) {
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
    
    public Dataset getAsDataset() {
        return (Dataset) this;
    }

}
