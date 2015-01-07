/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.DataverseRole;
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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

/**
 *
 * @author gdurand
 * @author mbarsinai
 */
@NamedQueries({
    @NamedQuery(name = "Dataverse.ownedObjectsById", query = "SELECT COUNT(obj) FROM DvObject obj WHERE obj.owner.id=:id")
})
@Entity
public class Dataverse extends DvObjectContainer {

    public enum DataverseType {
        RESEARCHERS, RESEARCH_PROJECTS, JOURNALS, ORGANIZATIONS_INSTITUTIONS, TEACHING_COURSES, UNCATEGORIZED
    };
    
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Please enter a name.")
    private String name;

    @NotBlank(message = "Please enter an alias.")
    @Size(max = 60, message = "Alias must be at most 60 characters.")
    @Pattern(regexp = "[a-zA-Z0-9\\_\\-]*", message = "Found an illegal character(s). Valid characters are a-Z, 0-9, '_', and '-'.")
    private String alias;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Please select a category for your dataverse.")
    private DataverseType dataverseType;
    
    /**
     * When {@code true}, users are not granted permissions the got for parent
     * dataverses.
     */
    protected boolean permissionRoot;

    
    public DataverseType getDataverseType() {
        return dataverseType;
    }

    public void setDataverseType(DataverseType dataverseType) {
        this.dataverseType = dataverseType;
    }
    
    public String getFriendlyCategoryName(){
       switch (this.dataverseType) {
            case RESEARCHERS:
                return "Researchers";
            case RESEARCH_PROJECTS:
                return "Research Projects";
            case JOURNALS:
                return "Journals";            
            case ORGANIZATIONS_INSTITUTIONS:
                return "Organizations & Institutions";            
            case TEACHING_COURSES:
                return "Teaching Courses";            
            case UNCATEGORIZED:
                return "Uncategorized";
            default:
                return "";
        }    
    }
    
    private String affiliation;

	// Note: We can't have "Remove" here, as there are role assignments that refer
    //       to this role. So, adding it would mean violating a forign key contstraint.
    @OneToMany(cascade = {CascadeType.MERGE},
            fetch = FetchType.LAZY,
            mappedBy = "owner")
    private Set<DataverseRole> roles;
    
    @ManyToOne
    @JoinColumn(nullable = false)
    private DataverseRole defaultContributorRole;

    public DataverseRole getDefaultContributorRole() {
        return defaultContributorRole;
    }

    public void setDefaultContributorRole(DataverseRole defaultContributorRole) {
        this.defaultContributorRole = defaultContributorRole;
    }
   
    private boolean metadataBlockRoot;
    private boolean facetRoot;
    private boolean themeRoot;
    private boolean templateRoot;    
    private boolean displayByType;
    private boolean displayFeatured;
    
    @OneToOne(mappedBy = "dataverse",cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST}, orphanRemoval=true)
      private DataverseTheme dataverseTheme;

    @OneToMany(mappedBy = "dataverse",cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST}, orphanRemoval=true)
    @OrderBy("displayOrder")
    @NotEmpty(message="At least one contact is required.")
    private List<DataverseContact> dataverseContacts = new ArrayList();
    
    @OneToMany(cascade = {CascadeType.MERGE})
    private List<MetadataBlock> metadataBlocks = new ArrayList<MetadataBlock>();

    @OneToMany(mappedBy = "dataverse")
    @OrderBy("displayOrder")
    private List<DataverseFacet> dataverseFacets = new ArrayList<DataverseFacet>();
    
    @OneToMany(mappedBy = "dataverse")
    private List<DataverseFieldTypeInputLevel> dataverseFieldTypeInputLevels = new ArrayList();
    
    @ManyToOne
    @JoinColumn(nullable = true)
    private Template defaultTemplate;  
    
    @OneToMany(cascade = {CascadeType.MERGE})
    private List<Template> templates; 
    
    @OneToMany(cascade = {CascadeType.MERGE})
    private List<Guestbook> guestbooks;

    public List<Guestbook> getGuestbooks() {
        return guestbooks;
    }

    public void setGuestbooks(List<Guestbook> guestbooks) {
        this.guestbooks = guestbooks;
    } 
    
    public List<Guestbook> getParentGuestbooks() {
        List<Guestbook> retList = new ArrayList();
        Dataverse testDV = this;
        while (testDV.getOwner() != null){   
          
           retList.addAll(testDV.getOwner().getGuestbooks());
           
           if(!testDV.getOwner().guestbookRoot){               
               break;
           }           
           testDV = testDV.getOwner();
        }
            return  retList;
    }
    
    public List<Guestbook> getAvailableGuestbooks(){
        
        List<Guestbook> retList = new ArrayList();
        Dataverse testDV = this;
        List<Guestbook> allGbs = new ArrayList();
        if (!this.guestbookRoot){
                    while (testDV.getOwner() != null){   
          
           allGbs.addAll(testDV.getOwner().getGuestbooks());
           
           if(!testDV.getOwner().guestbookRoot){               
               break;
           }           
           testDV = testDV.getOwner();
        }
            
        }
        
        allGbs.addAll(this.getGuestbooks());

        
        for (Guestbook gbt: allGbs){
            if(gbt.isEnabled()){
                retList.add(gbt);
            }
        }
            return  retList;
        
    }
    
    private boolean guestbookRoot;
    
    public boolean isGuestbookRoot() {
        return guestbookRoot;
    }

    public void setGuestbookRoot(boolean guestbookRoot) {
        this.guestbookRoot = guestbookRoot;
    } 
    
    
    public void setDataverseFieldTypeInputLevels(List<DataverseFieldTypeInputLevel> dataverseFieldTypeInputLevels) {
        this.dataverseFieldTypeInputLevels = dataverseFieldTypeInputLevels;
    }

    public List<DataverseFieldTypeInputLevel> getDataverseFieldTypeInputLevels() {
        return dataverseFieldTypeInputLevels;
    }


    public Template getDefaultTemplate() {
        return defaultTemplate;
    }

    public void setDefaultTemplate(Template defaultTemplate) {
        this.defaultTemplate = defaultTemplate;
    }

    public List<Template> getTemplates() {
        return templates;
    }

    public void setTemplates(List<Template> templates) {
        this.templates = templates;
    }

    public List<Template> getParentTemplates() {
        List<Template> retList = new ArrayList();
        Dataverse testDV = this;
        while (testDV.getOwner() != null){   
            
           if (!testDV.getMetadataBlocks().equals(testDV.getOwner().getMetadataBlocks())){
               break;
           }           
           retList.addAll(testDV.getOwner().getTemplates());
           
           if(!testDV.getOwner().templateRoot){               
               break;
           }           
           testDV = testDV.getOwner();
        }
            return  retList;
    }
    
    public boolean isThemeRoot() {
        return themeRoot;
    }
    
    public boolean getThemeRoot() {
        return themeRoot;
    }

    public void setThemeRoot(boolean  themeRoot) {
        this.themeRoot = themeRoot;
    }
    
    public boolean isTemplateRoot() {
        return templateRoot;
    }

    public void setTemplateRoot(boolean templateRoot) {
        this.templateRoot = templateRoot;
    }

   


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
    
    public Long getMetadataRootId(){
        if(metadataBlockRoot || getOwner() == null){
            return this.getId();
        } else { 
            return getOwner().getMetadataRootId();
        }
    }

    
    public DataverseTheme getDataverseTheme() {
        return getDataverseTheme(false);
    }

    public DataverseTheme getDataverseTheme(boolean returnActualDB) {
        if (returnActualDB || themeRoot || getOwner() == null) {
            return dataverseTheme;
        } else {
            return getOwner().getDataverseTheme();
        }
    }
    
    public String getLogoOwnerId() {
        if (themeRoot || getOwner()==null) {
            return this.getId().toString();
        } else {
            return getOwner().getId().toString();
        }
    } 
    
    public void setDataverseTheme(DataverseTheme dataverseTheme) {
        this.dataverseTheme=dataverseTheme;
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
     
    public Long getFacetRootId(){
        if(facetRoot || getOwner() == null){
            return this.getId();
        } else { 
            return getOwner().getFacetRootId();
        }        
    }

    public void setDataverseFacets(List<DataverseFacet> dataverseFacets) {
        this.dataverseFacets = dataverseFacets;
    }
    
    public List<DataverseContact> getDataverseContacts() {
        return dataverseContacts;
    }
    
    public String getContactEmails() {
        return "";
    }

    public void setDataverseContacts(List<DataverseContact> dataverseContacts) {
        this.dataverseContacts = dataverseContacts;
    }
    
    public void addDataverseContact(int index) {
        dataverseContacts.add(index, new DataverseContact(this));
    }

    public void removeDataverseContact(int index) {
        dataverseContacts.remove(index);
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

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
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

    public boolean isDisplayByType() {
        return displayByType;
    }

    public void setDisplayByType(boolean displayByType) {
        this.displayByType = displayByType;
    }

    public boolean isDisplayFeatured() {
        return displayFeatured;
    }

    public void setDisplayFeatured(boolean displayFeatured) {
        this.displayFeatured = displayFeatured;
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
    public <T> T accept(Visitor<T> v) {
        return v.visit(this);
    }

    /**
     * @todo implement in https://github.com/IQSS/dataverse/issues/551
     */
    public String getDepositTermsOfUse() {
        return "Dataverse Deposit Terms of Use will be implemented in https://github.com/IQSS/dataverse/issues/551";
    }
    
    @Override
    public String getDisplayName() {
        return getName() + " Dataverse";
    }
    
    @Override
    public boolean isPermissionRoot() {
        return permissionRoot;
    }

    public void setPermissionRoot(boolean permissionRoot) {
        this.permissionRoot = permissionRoot;
    }

}
