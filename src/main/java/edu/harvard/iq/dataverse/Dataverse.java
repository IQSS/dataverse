/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author gdurand
 * @author mbarsinai
 */
@NamedQueries({
	@NamedQuery(name = "Dataverse.ownedObjectsById", query="SELECT COUNT(obj) FROM DvObject obj WHERE obj.owner.id=:id")
})
@Entity
public class Dataverse extends DvObjectContainer {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Please enter a name.")
    private String name;

    @NotBlank(message = "Please enter an alias.")
    @Size(max = 32, message = "Alias must be at most 32 characters.")
    @Pattern(regexp = "[a-zA-Z0-9\\_\\-]*", message = "Found an illegal character(s). Valid characters are a-Z, 0-9, '_', and '-'.")
    private String alias;

    // #VALIDATION: page defines maxlength in input:textarea component
    @Column(name = "description", columnDefinition = "TEXT")
    @Size(max = 1000, message = "Description must be at most 1000 characters.")
    private String description;

    @NotBlank(message = "Please enter a valid email address.")
    @Email(message = "Please enter a valid email address.")
    private String contactEmail;

    private String affiliation;

	// Note: We can't have "Remove" here, as there are role assignments that refer
	//       to this role. So, adding it would mean violating a forign key contstraint.
    @OneToMany(cascade = {CascadeType.MERGE},
            fetch = FetchType.LAZY,
            mappedBy = "owner")
    private Set<DataverseRole> roles;

    /**
     * When {@code true}, users are not granted permissions the got for parent
     * dataverses.
     */
    private boolean permissionRoot;
    private boolean metadataBlockRoot;
    private boolean facetRoot;

    @OneToMany(cascade = {CascadeType.MERGE})
    private List<MetadataBlock> metadataBlocks = new ArrayList<MetadataBlock>();

    @OneToMany(mappedBy = "dataverse")
    @OrderBy("displayOrder")
    private List<DataverseFacet> dataverseFacets = new ArrayList<DataverseFacet>();
    
    public enum ImageFormat { SQUARE, RECTANGLE }

    @Enumerated(EnumType.STRING)
    private ImageFormat logoFormat;
    
    public enum Alignment { LEFT, CENTER, RIGHT }
    @Enumerated(EnumType.STRING)
    private Alignment logoAlignment;
    private String logoBackgroundColor;
    private String logo;
    private String tagline;
    private String linkUrl;
    private String linkText;
    private String linkColor;
    private String textColor; 
    private String backgroundColor; 

    public List<MetadataBlock> getMetadataBlocks() {
        return getMetadataBlocks(false);
    }

    public List<MetadataBlock> getMetadataBlocks(boolean returnActualDB) {
        if (returnActualDB || metadataBlockRoot || getOwner() == null) {
            return metadataBlocks;
        } else {
            return getOwner().getMetadataBlocks();
        }
    }

    public void setMetadataBlocks(List<MetadataBlock> metadataBlocks) {
        this.metadataBlocks = metadataBlocks;
    }

    public List<DataverseFacet> getDataverseFacets() {
        return getDataverseFacets(false);
    }    
    
    public List<DataverseFacet> getDataverseFacets(boolean returnActualDB) {
        if (returnActualDB || facetRoot || getOwner() == null) {
            return dataverseFacets;
        } else {
            return getOwner().getDataverseFacets();
        }
    }

    public void setDataverseFacets(List<DataverseFacet> dataverseFacets) {
        this.dataverseFacets = dataverseFacets;
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

    public boolean isEffectivlyPermissionRoot() {
        return isPermissionRoot() || (getOwner() == null);
    }

    public boolean isPermissionRoot() {
        return permissionRoot;
    }

    public void setPermissionRoot(boolean permissionRoot) {
        this.permissionRoot = permissionRoot;
    }

    public boolean isMetadataBlockRoot() {
        return metadataBlockRoot;
    }

    public void setMetadataBlockRoot(boolean metadataBlockRoot) {
        this.metadataBlockRoot = metadataBlockRoot;
    }

    public boolean isFacetRoot() {
        return facetRoot;
    }

    public void setFacetRoot(boolean facetRoot) {
        this.facetRoot = facetRoot;
    }

    public ImageFormat getLogoFormat() {
        return logoFormat;
    }

    public void setLogoFormat(ImageFormat logoFormat) {
        this.logoFormat = logoFormat;
    }

    public Alignment getLogoAlignment() {
        return logoAlignment;
    }

    public void setLogoAlignment(Alignment logoAlignment) {
        this.logoAlignment = logoAlignment;
    }

    public String getLogoBackgroundColor() {
        return logoBackgroundColor;
    }

    public void setLogoBackgroundColor(String logoBackgroundColor) {
        this.logoBackgroundColor = logoBackgroundColor;
    }
    
    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getLinkText() {
        return linkText;
    }

    public void setLinkText(String linkText) {
        this.linkText = linkText;
    }

    public String getLinkColor() {
        return linkColor;
    }

    public void setLinkColor(String linkColor) {
        this.linkColor = linkColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
    
    

    public void addRole(DataverseRole role) {
        role.setOwner(this);
        roles.add(role);
    }

    public Set<DataverseRole> getRoles() {
        return roles;
    }

    public List<Dataverse> getOwners() {
        List<Dataverse> owners = new ArrayList<Dataverse>();
        if (getOwner() != null) {
            owners.addAll(getOwner().getOwners());
            owners.add(getOwner());
        }
        return owners;
    }

        
    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Dataverse)) {
            return false;
        }
        Dataverse other = (Dataverse) object;
        return Objects.equals(getId(), other.getId());
    }

    @Override
    protected String toStringExtras() {
        return "name:" + getName();
    }

    @Override
	public <T> T accept( Visitor<T> v ) {
		return v.visit(this);
	}
	
}
