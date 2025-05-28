package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonLDNamespace;

import java.io.Serializable;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Objects;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 *
 * @author skraffmiller
 */
@Table(indexes = {@Index(columnList="name")
		, @Index(columnList="owner_id")})
@NamedQueries({
    @NamedQuery( name="MetadataBlock.listAll", query = "SELECT mdb FROM MetadataBlock mdb"),
    @NamedQuery( name="MetadataBlock.findByName", query = "SELECT mdb FROM MetadataBlock mdb WHERE mdb.name=:name")
})
@Entity
public class MetadataBlock implements Serializable, Comparable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column( nullable = false )
    private String name;
    @Column( nullable = false )
    private String displayName;

    @Column( name = "namespaceuri", columnDefinition = "TEXT")
    private String namespaceUri;
    
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

    public String getNamespaceUri() {
        return namespaceUri;
    }
    
    public void setNamespaceUri(String namespaceUri) {
        this.namespaceUri = namespaceUri;
    }
    
    private String getAssignedNamespaceUri() {
        String nsUri = getNamespaceUri();
        // Standard blocks will have a namespaceUri
        if (nsUri == null) {
            // Locally created/edited blocks, legacy blocks may not have a defined
            // namespaceUri, so generate one that indicates that this is a locally defined
            // term
            nsUri = SystemConfig.getDataverseSiteUrlStatic() + "/schema/" + name + "#";
        }
        return nsUri;
    }
    
    public JsonLDNamespace getJsonLDNamespace() {
        return JsonLDNamespace.defineNamespace(name, getAssignedNamespaceUri());
    }

    @OneToMany(mappedBy = "metadataBlock", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder")
    private List<DatasetFieldType> datasetFieldTypes;
    public List<DatasetFieldType> getDatasetFieldTypes() {
        return datasetFieldTypes;
    }
    
    public void setDatasetFieldTypes(List<DatasetFieldType> datasetFieldTypes) {
        this.datasetFieldTypes = datasetFieldTypes;
    }
    
    public boolean isDisplayOnCreate() {
        //Localize case - e.g. being called in the context of a specific collection 
        if (getLocalDisplayOnCreate() != null){
            return getLocalDisplayOnCreate();
        }
        // Non-localized case - the datasetFieldTypes are straight from the database and
        // never have dsfType.localDsiplayOnCreate set.
        for (DatasetFieldType dsfType : datasetFieldTypes) {
            boolean shouldDisplayOnCreate = dsfType.isDisplayOnCreate();
            if (shouldDisplayOnCreate) {
                return true;
            }
        }
        return false;
    }
    
    @Transient
    private Boolean localDisplayOnCreate;

    public Boolean getLocalDisplayOnCreate() {
        return localDisplayOnCreate;
    }

    public void setLocalDisplayOnCreate(Boolean localDisplayOnCreate) {
        this.localDisplayOnCreate = localDisplayOnCreate;
    }

    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public boolean isRequired() {
        // eventually this will be dynamic, for now only citation is required
        return "citation".equals(name);
    }
    
    @OneToOne
    @JoinColumn(name="owner_id", unique=false, nullable=true, insertable=true, updatable=true)
    private Dataverse owner;

    public Dataverse getOwner() {
        return owner;
    }

    public void setOwner(Dataverse owner) {
        this.owner = owner;
    }
 
    @Transient
    private boolean empty;

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }
    
    @Transient
    private boolean selected;

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    public boolean isSelected() {         
        return selected;
    }
    
    @Transient
    private boolean hasRequired;

    public void setHasRequired(boolean hasRequired) {
        this.hasRequired = hasRequired;
    }
    
    public boolean isHasRequired() {         
        return hasRequired;
    }

    public String getIdString(){
        return id.toString();
    }

    @Transient
    private boolean showDatasetFieldTypes;

    public void setShowDatasetFieldTypes(boolean showDatasetFieldTypes) {
        this.showDatasetFieldTypes = showDatasetFieldTypes;
    }
    
    public boolean isShowDatasetFieldTypes() {         
        return showDatasetFieldTypes;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof MetadataBlock)) {
            return false;
        }
        MetadataBlock other = (MetadataBlock) object;
        return !(!Objects.equals(this.id, other.id) && (this.id == null || !this.id.equals(other.id)));
    }    
    
    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.MetadataBlock[ id=" + id + " ]";
    }

    public String getLocaleDisplayName() {
        return getLocaleValue("metadatablock.displayName");
    }

    public String getLocaleDisplayFacet() {
        return getLocaleValue("metadatablock.displayFacet");
    }

    // Visible for testing
    String getLocaleValue(String metadataBlockKey) {
        try {
            return BundleUtil.getStringFromPropertyFile(metadataBlockKey, getName());
        } catch (MissingResourceException e) {
            return displayName;
        }
    }

    @Override
    public int compareTo(Object arg0) {
        //guaranteeing that citation will be shown first with custom blocks in order of creation
        MetadataBlock other = (MetadataBlock) arg0;
        Long t = "citation".equals(name) ? -1 : this.getId();
        return t.compareTo("citation".equals(other.name) ? -1 : other.getId());
    }
}
