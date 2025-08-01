package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearch;
import edu.harvard.iq.dataverse.storageuse.StorageUse;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

/**
 *
 * @author gdurand
 * @author mbarsinai
 */
@NamedQueries({
    @NamedQuery(name = "Dataverse.findIdStale",query = "SELECT d.id FROM Dataverse d WHERE d.indexTime is NULL OR d.indexTime < d.modificationTime"),
    @NamedQuery(name = "Dataverse.findIdStalePermission",query = "SELECT d.id FROM Dataverse d WHERE d.permissionIndexTime is NULL OR d.permissionIndexTime < d.permissionModificationTime"),
    @NamedQuery(name = "Dataverse.ownedObjectsById", query = "SELECT COUNT(obj) FROM DvObject obj WHERE obj.owner.id=:id"),
    @NamedQuery(name = "Dataverse.findAll", query = "SELECT d FROM Dataverse d order by d.name"),
    @NamedQuery(name = "Dataverse.findRoot", query = "SELECT d FROM Dataverse d where d.owner.id=null"),
    @NamedQuery(name = "Dataverse.findByAlias", query="SELECT dv FROM Dataverse dv WHERE LOWER(dv.alias)=:alias"),
    @NamedQuery(name = "Dataverse.findByOwnerId", query="select object(o) from Dataverse as o where o.owner.id =:ownerId order by o.name"),
    @NamedQuery(name = "Dataverse.findByCreatorId", query="select object(o) from Dataverse as o where o.creator.id =:creatorId order by o.name"),
    @NamedQuery(name = "Dataverse.findByReleaseUserId", query="select object(o) from Dataverse as o where o.releaseUser.id =:releaseUserId order by o.name"),
    @NamedQuery(name = "Dataverse.filterByAlias", query="SELECT dv FROM Dataverse dv WHERE LOWER(dv.alias) LIKE :alias order by dv.alias"),
    @NamedQuery(name = "Dataverse.filterByAliasNameAffiliation", query="SELECT dv FROM Dataverse dv WHERE (LOWER(dv.alias) LIKE :alias) OR (LOWER(dv.name) LIKE :name) OR (LOWER(dv.affiliation) LIKE :affiliation) order by dv.alias"),
    @NamedQuery(name = "Dataverse.filterByName", query="SELECT dv FROM Dataverse dv WHERE LOWER(dv.name) LIKE :name  order by dv.alias"),
    @NamedQuery(name = "Dataverse.countAll", query = "SELECT COUNT(dv) FROM Dataverse dv")
})
@Entity
@Table(indexes = {@Index(columnList="defaultcontributorrole_id")
		, @Index(columnList="defaulttemplate_id")
		, @Index(columnList="alias")
		, @Index(columnList="affiliation")
		, @Index(columnList="dataversetype")
		, @Index(columnList="facetroot")
		, @Index(columnList="guestbookroot")
		, @Index(columnList="metadatablockroot")
		, @Index(columnList="templateroot")
		, @Index(columnList="permissionroot")
		, @Index(columnList="themeroot")})
public class Dataverse extends DvObjectContainer {

    public enum DataverseType {
        RESEARCHERS, RESEARCH_PROJECTS, JOURNALS, ORGANIZATIONS_INSTITUTIONS, TEACHING_COURSES, UNCATEGORIZED, LABORATORY, RESEARCH_GROUP, DEPARTMENT
    };
    
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "{dataverse.name}")
    @Column( nullable = false )
    private String name;

    /**
     * @todo add @Column(nullable = false) for the database to enforce non-null
     */
    @NotBlank(message = "{dataverse.alias}")
    @Column(nullable = false, unique=true)
    @Size(max = 60, message = "{dataverse.aliasLength}")
    @Pattern.List({@Pattern(regexp = "[a-zA-Z0-9\\_\\-]*", message = "{dataverse.nameIllegalCharacters}"),
        @Pattern(regexp=".*\\D.*", message="{dataverse.aliasNotnumber}")})
    private String alias;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "{dataverse.category}")
    @Column( nullable = false )
    private DataverseType dataverseType;
       
    /**
     * When {@code true}, users are not granted permissions the got for parent
     * dataverses.
     */
    protected boolean permissionRoot;
    
    public Dataverse() {
        StorageUse storageUse = new StorageUse(this); 
        this.setStorageUse(storageUse);
    }
    
    public DataverseType getDataverseType() {
        return dataverseType;
    }

    public void setDataverseType(DataverseType dataverseType) {
        this.dataverseType = dataverseType;
    }

    public String getFriendlyCategoryName(){
        String key = getFriendlyCategoryKey();
        return BundleUtil.getStringFromBundle(key);
    }

    public String getFriendlyCategoryKey(){
        switch (this.dataverseType) {
            case RESEARCHERS:
                return  ("dataverse.type.selectTab.researchers");
            case RESEARCH_PROJECTS:
                return  ("dataverse.type.selectTab.researchProjects" );
            case JOURNALS:
                return  ("dataverse.type.selectTab.journals" );
            case ORGANIZATIONS_INSTITUTIONS:
                return  ("dataverse.type.selectTab.organizationsAndInsitutions" );
            case TEACHING_COURSES:
                return  ("dataverse.type.selectTab.teachingCourses" );
            case LABORATORY:
                return  ("dataverse.type.selectTab.laboratory");
            case RESEARCH_GROUP:
                return  ("dataverse.type.selectTab.researchGroup" );
            case DEPARTMENT:
                return  ("dataverse.type.selectTab.department" );
            case UNCATEGORIZED:
                return ("dataverse.type.selectTab.uncategorized");
            default:
                return "";
        }
    }


    public String getIndexableCategoryName() {
        String key = getFriendlyCategoryKey();
        if (key.equals("dataverse.type.selectTab.uncategorized")) {
            return null;
        } else {
            return BundleUtil.getStringFromDefaultBundle(key);
        }
    }

    private String affiliation;
    
    ///private String storageDriver=null;

	// Note: We can't have "Remove" here, as there are role assignments that refer
    //       to this role. So, adding it would mean violating a forign key contstraint.
    @OneToMany(cascade = {CascadeType.MERGE},
            fetch = FetchType.LAZY,
            mappedBy = "owner")
    private Set<DataverseRole> roles;
    
    @ManyToOne
    @JoinColumn(nullable = true)
    private DataverseRole defaultContributorRole;

    public DataverseRole getDefaultContributorRole() {
        return defaultContributorRole;
    }

    public void setDefaultContributorRole(DataverseRole defaultContributorRole) {
        this.defaultContributorRole = defaultContributorRole;
    }
   
    private boolean metadataBlockRoot;
    private boolean facetRoot;
    // By default, themeRoot should be true, as new dataverses should start with the default theme
    private boolean themeRoot = true;
    private boolean templateRoot;    

    
    @OneToOne(mappedBy = "dataverse",cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST}, orphanRemoval=true)
      private DataverseTheme dataverseTheme;

    @OneToMany(mappedBy = "dataverse",cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST}, orphanRemoval=true)
    @OrderBy("displayOrder")
    @NotEmpty(message="At least one contact is required.")
    private List<DataverseContact> dataverseContacts = new ArrayList<>();
    
    @ManyToMany(cascade = {CascadeType.MERGE})
    private List<MetadataBlock> metadataBlocks = new ArrayList<>();

    @OneToMany(mappedBy = "dataverse",cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST}, orphanRemoval=true)
    @OrderBy("displayOrder")
    private List<DataverseFacet> dataverseFacets = new ArrayList<>();
    
    @ManyToMany(cascade = {CascadeType.MERGE})
    @JoinTable(name = "dataverse_citationDatasetFieldTypes",
    joinColumns = @JoinColumn(name = "dataverse_id"),
    inverseJoinColumns = @JoinColumn(name = "citationdatasetfieldtype_id"))
    private List<DatasetFieldType> citationDatasetFieldTypes = new ArrayList<>();
    
    @ManyToMany
    @JoinTable(name = "dataversesubjects",
    joinColumns = @JoinColumn(name = "dataverse_id"),
    inverseJoinColumns = @JoinColumn(name = "controlledvocabularyvalue_id"))
    private Set<ControlledVocabularyValue> dataverseSubjects;
    
    @OneToMany(mappedBy="dataverse", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DataverseFeaturedDataverse> dataverseFeaturedDataverses;

    public List<DataverseFeaturedDataverse> getDataverseFeaturedDataverses() {
        return dataverseFeaturedDataverses;
    }

    public void setDataverseFeaturedDataverses(List<DataverseFeaturedDataverse> dataverseFeaturedDataverses) {
        this.dataverseFeaturedDataverses = dataverseFeaturedDataverses;
    }
    
    @OneToMany(mappedBy="featuredDataverse", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DataverseFeaturedDataverse> dataverseFeaturingDataverses;

    public List<DataverseFeaturedDataverse> getDataverseFeaturingDataverses() {
        return dataverseFeaturingDataverses;
    }

    public void setDataverseFeaturingDataverses(List<DataverseFeaturedDataverse> dataverseFeaturingDataverses) {
        this.dataverseFeaturingDataverses = dataverseFeaturingDataverses;
    }
    
    @OneToMany(mappedBy="dataverse", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DataverseLinkingDataverse> dataverseLinkingDataverses;

    public List<DataverseLinkingDataverse> getDataverseLinkingDataverses() {
        return dataverseLinkingDataverses;
    }

    public void setDataverseLinkingDataverses(List<DataverseLinkingDataverse> dataverseLinkingDataverses) {
        this.dataverseLinkingDataverses = dataverseLinkingDataverses;
    }
       
    @OneToMany(mappedBy="linkingDataverse", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DataverseLinkingDataverse> dataverseLinkedDataverses;

    public List<DataverseLinkingDataverse> getDataverseLinkedDataverses() {
        return dataverseLinkedDataverses;
    }

    public void setDataverseLinkedDataverses(List<DataverseLinkingDataverse> dataverseLinkedDataverses) {
        this.dataverseLinkedDataverses = dataverseLinkedDataverses;
    }
    
    @OneToMany(mappedBy="linkingDataverse", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetLinkingDataverse> datasetLinkingDataverses;

    public List<DatasetLinkingDataverse> getDatasetLinkingDataverses() {
        return datasetLinkingDataverses;
    }

    public void setDatasetLinkingDataverses(List<DatasetLinkingDataverse> datasetLinkingDataverses) {
        this.datasetLinkingDataverses = datasetLinkingDataverses;
    }
        
    public Set<ControlledVocabularyValue> getDataverseSubjects() {
        return dataverseSubjects;
    }

    public void setDataverseSubjects(Set<ControlledVocabularyValue> dataverseSubjects) {
        this.dataverseSubjects = dataverseSubjects;
    }

    
    @OneToMany(mappedBy = "dataverse")
    private List<DataverseFieldTypeInputLevel> dataverseFieldTypeInputLevels = new ArrayList<>();
    
    @ManyToOne
    @JoinColumn(nullable = true)
    private Template defaultTemplate;  
    
    @OneToMany(mappedBy = "definitionPoint", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<SavedSearch> savedSearches;

    public List<SavedSearch> getSavedSearches() {
        return savedSearches;
    }

    public void setSavedSearches(List<SavedSearch> savedSearches) {
        this.savedSearches = savedSearches;
    }
    
    @OneToMany(mappedBy="dataverse", cascade = {CascadeType.MERGE, CascadeType.REMOVE})
    private List<Template> templates; 
    
    @OneToMany(mappedBy="dataverse", cascade = {CascadeType.MERGE, CascadeType.REMOVE})
    private List<Guestbook> guestbooks;
        
    public List<Guestbook> getGuestbooks() {
        return guestbooks;
    }

    public void setGuestbooks(List<Guestbook> guestbooks) {
        this.guestbooks = guestbooks;
    } 
    
    
    @OneToMany (mappedBy="dataverse", cascade={CascadeType.MERGE, CascadeType.REMOVE})
    private List<HarvestingClient> harvestingClientConfigs;

    public List<HarvestingClient> getHarvestingClientConfigs() {
        return this.harvestingClientConfigs;
    }

    public void setHarvestingClientConfigs(List<HarvestingClient> harvestingClientConfigs) {
        this.harvestingClientConfigs = harvestingClientConfigs;
    }
    /*
    public boolean isHarvested() {
        return harvestingClient != null; 
    }
    */
    private boolean metadataBlockFacetRoot;

    public boolean isMetadataBlockFacetRoot() {
        return metadataBlockFacetRoot;
    }

    public void setMetadataBlockFacetRoot(boolean metadataBlockFacetRoot) {
        this.metadataBlockFacetRoot = metadataBlockFacetRoot;
    }

    @OneToMany(mappedBy = "dataverse",cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST }, orphanRemoval=true)
    private List<DataverseMetadataBlockFacet> metadataBlockFacets = new ArrayList<>();

    public List<DataverseMetadataBlockFacet> getMetadataBlockFacets() {
        if (isMetadataBlockFacetRoot() || getOwner() == null) {
            return metadataBlockFacets;
        } else {
            return getOwner().getMetadataBlockFacets();
        }
    }

    public void setMetadataBlockFacets(List<DataverseMetadataBlockFacet> metadataBlockFacets) {
        this.metadataBlockFacets = metadataBlockFacets;
    }

    public List<Guestbook> getParentGuestbooks() {
        List<Guestbook> retList = new ArrayList<>();
        Dataverse testDV = this;
        while (testDV.getOwner() != null){   
           retList.addAll(testDV.getOwner().getGuestbooks());          
           if(testDV.getOwner().guestbookRoot){               
               break;
           }           
           testDV = testDV.getOwner();
        }
            return  retList;
    }
    
    public List<Guestbook> getAvailableGuestbooks() {
        //get all guestbooks
        List<Guestbook> retList = new ArrayList<>();
        Dataverse testDV = this;
        List<Guestbook> allGbs = new ArrayList<>();
        if (!this.guestbookRoot){
                    while (testDV.getOwner() != null){   
          
                allGbs.addAll(testDV.getOwner().getGuestbooks());
                if (testDV.getOwner().isGuestbookRoot()) {
                    break;
                }
                testDV = testDV.getOwner();
            }
        }

        allGbs.addAll(this.getGuestbooks());
        //then only display them if they are enabled
        for (Guestbook gbt : allGbs) {
            if (gbt.isEnabled()) {
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

    public boolean isDatasetFieldTypeRequiredAsInputLevel(Long datasetFieldTypeId) {
        return dataverseFieldTypeInputLevels.stream()
                .anyMatch(inputLevel -> inputLevel.getDatasetFieldType().getId().equals(datasetFieldTypeId) && inputLevel.isRequired());
    }

    public boolean isDatasetFieldTypeIncludedAsInputLevel(Long datasetFieldTypeId) {
        return dataverseFieldTypeInputLevels.stream()
                .anyMatch(inputLevel -> inputLevel.getDatasetFieldType().getId().equals(datasetFieldTypeId) && inputLevel.isInclude());
    }

    public boolean isDatasetFieldTypeInInputLevels(Long datasetFieldTypeId) {
        return dataverseFieldTypeInputLevels.stream()
                .anyMatch(inputLevel -> inputLevel.getDatasetFieldType().getId().equals(datasetFieldTypeId));
    }

    public DataverseFieldTypeInputLevel getDatasetFieldTypeInInputLevels(Long datasetFieldTypeId) {
        return dataverseFieldTypeInputLevels.stream()
                .filter(inputLevel -> inputLevel.getDatasetFieldType().getId().equals(datasetFieldTypeId))
                .findFirst()
                .orElse(null);
    }
    
    public boolean isDatasetFieldTypeDisplayOnCreateAsInputLevel(Long datasetFieldTypeId) {
        return dataverseFieldTypeInputLevels.stream()
                .anyMatch(inputLevel -> inputLevel.getDatasetFieldType().getId().equals(datasetFieldTypeId) 
                         && Boolean.TRUE.equals(inputLevel.getDisplayOnCreate()));
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
        List<Template> retList = new ArrayList<>();
        Dataverse testDV = this;
        while (testDV.getOwner() != null){   
            
           if (!testDV.getMetadataBlocks().equals(testDV.getOwner().getMetadataBlocks())){
               break;
           }           
           retList.addAll(testDV.getOwner().getTemplates());
           
           if(testDV.getOwner().templateRoot){               
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
    
    public String getGuestbookRootDataverseName() {
        Dataverse testDV = this;
        String retName = "Parent";
        while (testDV.getOwner() != null) {
            retName = testDV.getOwner().getDisplayName();
            if (testDV.getOwner().guestbookRoot) {
                break;
            }
            testDV = testDV.getOwner();
        }
        return retName;
    }

    public String getTemplateRootDataverseName() {
        Dataverse testDV = this;
        String retName = "Parent";
        while (testDV.getOwner() != null) {
            retName = testDV.getOwner().getDisplayName();
            if (testDV.getOwner().templateRoot) {
                break;
            }
            testDV = testDV.getOwner();
        }
        return retName;
    }

    public String getThemeRootDataverseName() {
        Dataverse testDV = this;
        String retName = "Parent";
        while (testDV.getOwner() != null) {
            retName = testDV.getOwner().getDisplayName();
            if (testDV.getOwner().themeRoot) {
                break;
            }
            testDV = testDV.getOwner();
        }
        return retName;
    }

    public String getMetadataRootDataverseName() {
        Dataverse testDV = this;
        String retName = "Parent";
        while (testDV.getOwner() != null) {
            retName = testDV.getOwner().getDisplayName();
            if (testDV.getOwner().metadataBlockRoot) {
                break;
            }
            testDV = testDV.getOwner();
        }
        return retName;
    }
    
    public String getFacetRootDataverseName() {
        Dataverse testDV = this;
        String retName = "Parent";
        while (testDV.getOwner() != null) {
            retName = testDV.getOwner().getDisplayName();
            if (testDV.getOwner().facetRoot) {
                break;
            }
            testDV = testDV.getOwner();
        }
        return retName;
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
        this.metadataBlocks = new ArrayList<>(metadataBlocks);
    }

    public void clearMetadataBlocks() {
        this.metadataBlocks.clear();
    }

    public List<DatasetFieldType> getCitationDatasetFieldTypes() {
        return citationDatasetFieldTypes;
    }

    public void setCitationDatasetFieldTypes(List<DatasetFieldType> citationDatasetFieldTypes) {
        this.citationDatasetFieldTypes = citationDatasetFieldTypes;
    }

    @Column(nullable = true)
    private Boolean requireFilesToPublishDataset;
    /**
     * Specifies whether the existance of files in a dataset is required when publishing
     * @return {@code Boolean.TRUE} if explicitly enabled, {@code Boolean.FALSE} if explicitly disabled.
     * {@code null} indicates that the behavior is not explicitly defined, in which
     * case the behavior should follow the explicit configuration of the first
     * direct ancestor collection.
     * @Note: If present, this configuration therefore by default applies to all
     * the sub-collections, unless explicitly overwritten there.
     */
    public Boolean getRequireFilesToPublishDataset() {
        return requireFilesToPublishDataset;
    }
    public void setRequireFilesToPublishDataset(boolean requireFilesToPublishDataset) {
        this.requireFilesToPublishDataset = requireFilesToPublishDataset;
    }

    /**
     * @Note: this setting is Nullable, with {@code null} indicating that the 
     * desired behavior is not explicitly configured for this specific collection. 
     * See the comment below. 
     */
    @Column(nullable = true)
    private Boolean filePIDsEnabled;

    /**
     * Specifies whether the PIDs for Datafiles should be registered when publishing 
     * datasets in this Collection, if the behavior is explicitly configured.
     * @return {@code Boolean.TRUE} if explicitly enabled, {@code Boolean.FALSE} if explicitly disabled. 
     * {@code null} indicates that the behavior is not explicitly defined, in which 
     * case the behavior should follow the explicit configuration of the first 
     * direct ancestor collection, or the instance-wide configuration, if none 
     * present. 
     * @Note: If present, this configuration therefore by default applies to all 
     * the sub-collections, unless explicitly overwritten there.
     * @author landreev
     */
    public Boolean getFilePIDsEnabled() {
        return filePIDsEnabled;
    }
    
    public void setFilePIDsEnabled(boolean filePIDsEnabled) {
        this.filePIDsEnabled = filePIDsEnabled;
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
    
    /**
     * Get the email addresses of the dataverse contacts as a comma-separated
     * concatenation.
     * @return a comma-separated concatenation of email addresses, or the empty
     *  string if there are no contacts.
     * @author bencomp
     */
    public String getContactEmails() {
        if (dataverseContacts != null && !dataverseContacts.isEmpty()) {
            StringBuilder buf = new StringBuilder();
            Iterator<DataverseContact> it = dataverseContacts.iterator();
            while (it.hasNext()) {
                DataverseContact con = it.next();
                buf.append(con.getContactEmail());
                if (it.hasNext()) {
                    buf.append(",");
                }
            }
            return buf.toString();
        } else {
            return "";
        }
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


    public void addRole(DataverseRole role) {
        role.setOwner(this);
        if ( roles == null ) {
            roles = new HashSet<>();
        }
        roles.add(role);
    }
    
    /**
     * Note: to add a role, use {@link #addRole(edu.harvard.iq.dataverse.authorization.DataverseRole)},
     * do not call this method and try to add directly to the list. 
     * @return the roles defined in this Dataverse.
     */
    public Set<DataverseRole> getRoles() {
        if ( roles == null ) {
            roles = new HashSet<>();
        }
        return roles;
    }
    
    public List<Dataverse> getOwners() {
        List<Dataverse> owners = new ArrayList<>();
        if (getOwner() != null) {
            owners.addAll(getOwner().getOwners());
            owners.add(getOwner());
        }
        return owners;
    }

    public boolean getEffectiveRequiresFilesToPublishDataset() {
        Dataverse dv = this;
        while (dv != null) {
            if (dv.getRequireFilesToPublishDataset() != null) {
                return dv.getRequireFilesToPublishDataset();
            }
            dv = dv.getOwner();
        }
        return false;
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
        return getName();
    }
    
    @Override
    public String getCurrentName() {
        return getName();
    }
    
    @Override
    public boolean isPermissionRoot() {
        return permissionRoot;
    }

    public void setPermissionRoot(boolean permissionRoot) {
        this.permissionRoot = permissionRoot;
    }
      
    @Override
    public boolean isAncestorOf( DvObject other ) {
        while ( other != null ) {
            if ( equals(other) ) {
                return true;
            }
            other = other.getOwner();
        }
        return false;
    }
    
    public String getLocalURL() {
        return  SystemConfig.getDataverseSiteUrlStatic() + "/dataverse/" + this.getAlias();
    }

    public void addInputLevelsMetadataBlocksIfNotPresent(List<DataverseFieldTypeInputLevel> inputLevels) {
        for (DataverseFieldTypeInputLevel inputLevel : inputLevels) {
            MetadataBlock inputLevelMetadataBlock = inputLevel.getDatasetFieldType().getMetadataBlock();
            if (!hasMetadataBlock(inputLevelMetadataBlock)) {
                metadataBlocks.add(inputLevelMetadataBlock);
            }
        }
    }

    private boolean hasMetadataBlock(MetadataBlock metadataBlock) {
        return metadataBlocks.stream().anyMatch(block -> block.getId().equals(metadataBlock.getId()));
    }
}
