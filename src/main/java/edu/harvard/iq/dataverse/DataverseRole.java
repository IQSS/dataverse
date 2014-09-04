package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.util.BitSet;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * A role is an annotated set of permissions. A role belongs
 * to a {@link Dataverse}. Users may assume roles from the current dataverse,
 * or from its parent dataverses, up to the first permission root dataverse.
 * 
 * @author michael
 */
@NamedQueries({
	@NamedQuery(name = "DataverseRole.findByOwnerId",
			    query= "SELECT r FROM DataverseRole r WHERE r.owner.id=:ownerId ORDER BY r.name"),
	@NamedQuery(name = "DataverseRole.listAll",
			    query= "SELECT r FROM DataverseRole r"),
	@NamedQuery(name = "DataverseRole.deleteById",
			    query= "DELETE FROM DataverseRole r WHERE r.id=:id")
})
@Entity
public class DataverseRole implements Serializable  {
	public static final Comparator<DataverseRole> CMP_BY_NAME = new Comparator<DataverseRole>(){

		@Override
		public int compare(DataverseRole o1, DataverseRole o2) {
			int cmp = o1.getName().compareTo(o2.getName());
			if ( cmp != 0 ) return cmp;
			return o1.getOwner().getId().compareTo( o2.getOwner().getId() );
		}
	};
	public static Set<Permission> permissionSet( Iterable<DataverseRole> roles ) {
		long miniset = 0l;
		for ( DataverseRole role : roles ) {
			miniset |= role.permissionBits;
		}
		return new BitSet(miniset).asSetOf(Permission.class);
	}
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Pattern(regexp=".+", message="{MustHaveNameMsg}")
    private String name;
    
	@Size(max = 1000, message = "{descriptionOutOfBoundsMsg}")
    private String description;
    
    @Size(max = 16, message = "{aliasOutOf16Msg}")
    @Pattern(regexp = "[a-zA-Z0-9\\_\\-]+", message = "{aliasValidMsg}")
    private String alias;
	
	/** Stores the permissions in a bit set.  */
	private long permissionBits;
	
	@ManyToOne
    @JoinColumn(nullable=false)     
    private DvObject owner;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public DvObject getOwner() {
		return owner;
	}

	public void setOwner(DvObject owner) {
		this.owner = owner;
	}
	
	public void addPermissions( Collection<Permission> ps ) {
		for ( Permission p : ps ) addPermission(p);
	}
	
	public void addPermission( Permission p ) {
		permissionBits = new BitSet(permissionBits).set(p.ordinal()).getBits();
	}
	
	public void clearPermissions() {
		permissionBits = 0l;
	}
	
	public Set<Permission> permissions() {
		return new BitSet(permissionBits).asSetOf(Permission.class);
	}
	
	public long getPermissionsBits() {
		return permissionBits;
	}

	@Override
	public String toString() {
		return "DataverseRole{" + "id=" + id + ", alias=" + alias + '}';
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + Objects.hashCode(this.id);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final DataverseRole other = (DataverseRole) obj;
		if (!Objects.equals(this.id, other.id)) {
			return false;
		}
		return true;
	}
}
