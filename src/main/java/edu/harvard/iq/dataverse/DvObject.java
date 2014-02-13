package edu.harvard.iq.dataverse;

import java.util.Objects;
import javax.persistence.*;

/**
 * Base of the object hierarchy for "anything that can be inside a dataverse".
 * @author michael
 */
@NamedQueries({
	@NamedQuery(name = "DvObject.findAll",
				query = "SELECT o FROM DvObject o ORDER BY o.id"),
	@NamedQuery(name = "DvObject.findById",
				query = "SELECT o FROM DvObject o WHERE o.id=:id")
})
@Entity
public abstract class DvObject implements java.io.Serializable {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@ManyToOne( cascade = CascadeType.MERGE )
	private DvObjectContainer owner;
	
	/**
	 * Sets the owner of the object. This is {@code protected} rather than
	 * {@code public}, since different sub-classes have different possible
	 * owner types: a {@link DataFile} can only have a {@link DataSet}, for example.
	 * @param newOwner 
	 */
	protected void setOwner( DvObjectContainer newOwner ) {
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
	
	@Override
    public int hashCode() {
        return Objects.hash( getId() ); 
    }
	
	@Override
	public abstract boolean equals( Object o );
	
	@Override
	public String toString() {
		String classNameComps[] = getClass().getName().split("\\.");
		return String.format("[%s id:%d %s]", classNameComps[classNameComps.length-1],
					getId(), toStringExtras());
	}
	
	/**
	 * Convenience method to add data to the default toString output.
	 * @return 
	 */
	protected String toStringExtras() {return "";}
}
