/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author gdurand
 * @author mbarsinai
 */
@Entity
public class Dataverse implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Please enter a name.")
    private String name;

    @NotBlank(message = "Please enter an alias.")
    @Size(max = 32, message = "Alias must be at most 32 characters.")
    @Pattern(regexp = "[a-zA-Z0-9\\_\\-]*", message = "Found an illegal character(s). Valid characters are a-Z, 0-9, '_', and '-'.")
    private String alias;

    // #VALIDATION: page defines maxlength in input:textarea component
    @Size(max = 1000, message = "Description must be at most 1000 characters.")
    private String description;

    @NotBlank(message = "Please enter a valid email address.")
    @Email(message = "Please enter a valid email address.")
    private String contactEmail;

    private String affiliation;
	
	@OneToMany(cascade = {CascadeType.MERGE, CascadeType.REMOVE},
		fetch = FetchType.LAZY )
	private Set<DataverseRole> roles;
	
    @ManyToOne
    private Dataverse owner;
	
	/** 
	 * When {@code true}, users are not granted permissions the got for parent dataverses. 
	 */
	private boolean permissionRoot;

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

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public Dataverse getOwner() {
        return owner;
    }

    public void setOwner(Dataverse owner) {
        this.owner = owner;
    }

	public boolean isEffectivlyPermissionRoot() {
		return isPermissionRoot() || (getOwner()==null);
	}
	
	public boolean isPermissionRoot() {
		return permissionRoot;
	}

	public void setPermissionRoot(boolean permissionRoot) {
		this.permissionRoot = permissionRoot;
	}
    
    public List<Dataverse> getOwners() {
        List owners = new ArrayList();
        if (owner != null) {
            owners.addAll( owner.getOwners() );
            owners.add(owner);
        }
        return owners;
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
        if (!(object instanceof Dataverse)) {
            return false;
        }
        Dataverse other = (Dataverse) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.Dataverse[ id=" + id + " ]";
    }
}
