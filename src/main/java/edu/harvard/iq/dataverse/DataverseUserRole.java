package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.util.BitSet;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Transient;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.hibernate.validator.constraints.NotBlank;

/**
 * A role is an annotated set of permissions. A role belongs
 * to a {@link Dataverse}. Users may assume roles from the current dataverse,
 * or from its parent dataverses, up to the first permission root dataverse.
 * 
 * @author michael
 */
@Entity
public class DataverseUserRole implements Serializable  {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    private String name;
    
	@Size(max = 1000, message = "Description must be at most 1000 characters.")
    private String description;
    
	@NotBlank(message = "Please enter an alias.")
    @Size(max = 16, message = "Alias must be at most 16 characters.")
    @Pattern(regexp = "[a-zA-Z0-9\\_\\-]*", message = "Found an illegal character(s). Valid characters are a-Z, 0-9, '_', and '-'.")
    private String alias;
	
	/** Stores the permissions in a bit set.  */
	long permissionBits;
	
	@Transient
	private EnumSet<Permission> granted;
	
	@ManyToOne
    @JoinColumn(nullable=false)     
    private Dataverse owner;
	
	@PrePersist
	protected void prePersist() {
		permissionBits = BitSet.from(granted).getBits();
	}
	
	@PostLoad
	protected void postLoad() {
		granted = new BitSet(permissionBits).asSetOf(Permission.class);
	}

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

	public Dataverse getOwner() {
		return owner;
	}

	public void setOwner(Dataverse owner) {
		this.owner = owner;
	}
	
	public Set<Permission> permissions() {
		return granted;
	}
	
}
