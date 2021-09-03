package edu.harvard.iq.dataverse;

import com.univocity.parsers.annotations.Parsed;
import com.univocity.parsers.annotations.Validate;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.metadata.Placeholder;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 *
 * @author skraffmiller
 */
@Table(indexes = {@Index(columnList="name"),
                  @Index(columnList="owner_id")})
@NamedQueries({
    @NamedQuery( name="MetadataBlock.listAll", query = "SELECT mdb FROM MetadataBlock mdb"),
    @NamedQuery( name="MetadataBlock.findByName", query = "SELECT mdb FROM MetadataBlock mdb WHERE mdb.name=:name")
})
@Entity
public class MetadataBlock implements Serializable {
    
    /**
     * Reusable definition of headers used for parsing this model class from data (TSV, JSON, manual, ...)
     * Using the Headers.Constants class to work around annotations not able to use enum values (a Java limitation).
     */
    public enum Headers {
        // Order matters: this must be the same order as we define rules for the TSV format!
        NAME(Constants.NAME),
        OWNER(Constants.OWNER),
        DISPLAY_NAME(Constants.DISPLAY_NAME),
        NAMESPACE_URI(Constants.NAMESPACE_URI);
    
        public static final class Constants {
            public final static String NAME = "name";
            public final static String OWNER = "dataverseAlias";
            public final static String DISPLAY_NAME = "displayName";
            public final static String NAMESPACE_URI = "blockURI";
        }
        
        private final String key;
        Headers(String key) {
            this.key = key;
        }
        public String key() {
            return this.key;
        }
        
        public static String[] keys() {
            return Arrays.stream(values()).map(v -> v.key()).collect(Collectors.toUnmodifiableList()).toArray(new String[]{});
        }
    }

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
    
    @Parsed(field = Headers.Constants.NAME)
    // Docs: No spaces or punctuation, except underscore. By convention, should start with a letter, and use lower camel case
    @Validate(matches = "^[a-z][\\w]+$")
    public void setName(String name) {
        this.name = name;
    }

    public String getNamespaceUri() {
        return namespaceUri;
    }
    
    @Parsed(field = Headers.Constants.NAMESPACE_URI)
    @Validate(nullable = true, matches = "^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
    public void setNamespaceUri(String namespaceUri) {
        this.namespaceUri = namespaceUri;
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
        for (DatasetFieldType dsfType : datasetFieldTypes) {
            if (dsfType.isDisplayOnCreate()) {
                return true;
            }
        }
        return false;
    }

    public String getDisplayName() {
        return displayName;
    }
    
    @Parsed(field = Headers.Constants.DISPLAY_NAME)
    @Validate(matches = "^\\S.{0,255}$") // docs: match all but not blank strings, at least 1 character needed, not nullable, max 256 chars
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
    
    /**
     * Set the (optional) owning Dataverse collection of this metadata block. This and children of the collection
     * will be able to use the metadata block.
     *
     * When this block is parsed by {@link edu.harvard.iq.dataverse.util.metadata.TsvMetadataBlockParser},
     * the alias given in the TSV will be validated. For valid values see the docs
     * ("Special characters (~,`, !, @, #, $, %, ^, &, and *) and spaces are not allowed")
     * and {@link edu.harvard.iq.dataverse.Dataverse#alias} validation patterns.
     * (The possessive matcher "+*" below achieves in 1 regex where the other validator needs 2)
     *
     * During parsing, a placeholder will be injected here, needing replacement and more validation.
     *
     * @param dataverseAlias The alias/identifier of the owning Dataverse collection
     */
    @Parsed(field = Headers.Constants.OWNER)
    @Validate(nullable = true, matches = "^[\\d]*+[\\w\\-]+$")
    protected void setOwner(String dataverseAlias) {
        if (dataverseAlias == null)
            return;
        this.owner = new Placeholder.Dataverse();
        this.owner.setAlias(dataverseAlias);
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
        return "MetadataBlock{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", displayName='" + displayName + '\'' +
            ", namespaceUri='" + namespaceUri + '\'' +
            ", datasetFieldTypes=" + datasetFieldTypes +
            ", owner=" + owner +
            ", empty=" + empty +
            ", selected=" + selected +
            ", hasRequired=" + hasRequired +
            ", showDatasetFieldTypes=" + showDatasetFieldTypes +
            '}';
    }
    
    public String getLocaleDisplayName() {
        try {
            return BundleUtil.getStringFromPropertyFile("metadatablock.displayName", getName());
        } catch (MissingResourceException e) {
            return displayName;
        }
    }
}
