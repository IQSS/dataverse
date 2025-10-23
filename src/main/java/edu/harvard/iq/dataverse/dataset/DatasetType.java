package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.logging.Logger;

@NamedQueries({
    @NamedQuery(name = "DatasetType.findAll",
            query = "SELECT d FROM DatasetType d"),
    @NamedQuery(name = "DatasetType.findById",
            query = "SELECT d FROM DatasetType d WHERE d.id=:id"),
    @NamedQuery(name = "DatasetType.findByName",
            query = "SELECT d FROM DatasetType d WHERE d.name=:name"),
    @NamedQuery(name = "DatasetType.deleteById",
            query = "DELETE FROM DatasetType d WHERE d.id=:id"),})
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = "name"),}
)

public class DatasetType implements Serializable {

    private static final Logger logger = Logger.getLogger(DatasetType.class.getCanonicalName());

    public static final String DATASET_TYPE_DATASET = "dataset";
    public static final String DATASET_TYPE_SOFTWARE = "software";
    public static final String DATASET_TYPE_WORKFLOW = "workflow";
    public static final String DATASET_TYPE_REVIEW = "review";
    public static final String DEFAULT_DATASET_TYPE = DATASET_TYPE_DATASET;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Machine readable name to use via API.
     */
    // Any constraints? @Pattern regexp?
    @Column(nullable = false)
    private String name;

    /**
     * Human readable name to show in the UI.
     */
    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT ''")
    private String displayName;

    /**
     * The metadata blocks this dataset type is linked to.
     */
    @ManyToMany(cascade = {CascadeType.MERGE})
    private List<MetadataBlock> metadataBlocks = new ArrayList<>();
    
    /**
     * The Licenses this dataset type is linked to.
     */
    @ManyToMany(cascade = {CascadeType.MERGE})
    private List<License> licenses = new ArrayList<>();

    public DatasetType() {
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

    /**
     * In most cases, you should call the getDisplayName(locale) version. This is
     * here in case you really want the value from the database.
     */
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<MetadataBlock> getMetadataBlocks() {
        return metadataBlocks;
    }

    public void setMetadataBlocks(List<MetadataBlock> metadataBlocks) {
        this.metadataBlocks = metadataBlocks;
    }
    
    public List<License> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<License> licenses) {
        this.licenses = licenses;
    }

    public JsonObjectBuilder toJson(Locale locale) {
        JsonArrayBuilder linkedMetadataBlocks = Json.createArrayBuilder();
        for (MetadataBlock metadataBlock : this.getMetadataBlocks()) {
            linkedMetadataBlocks.add(metadataBlock.getName());
        }
        JsonArrayBuilder availableLicenses = Json.createArrayBuilder();
        for (License license : this.getLicenses()) {
            availableLicenses.add(license.getName());
        }
        return Json.createObjectBuilder()
                .add("id", getId())
                .add("name", getName())
                .add("displayName", getDisplayName(locale))
                .add("linkedMetadataBlocks", linkedMetadataBlocks)
                .add("availableLicenses", availableLicenses);
    }

    public String getDisplayName(Locale locale) {
        logger.fine("Getting display name for dataset type " + name + " and locale " + locale);
        if (locale == null) {
            logger.fine("Locale is null, returning default display name: " + displayName);
            return displayName;
        }
        if (locale.getLanguage().isBlank()) {
            logger.fine("Locale couldn't be parsed, returning default display name: " + displayName);
            return displayName;
        }
        if (locale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
            // This is here to prevent looking up datasetTypes_en.properties, which doesn't exist.
            // The English strings are in datasetTypes.properties (no _en).
            logger.fine("Locale is English, returning default display name: " + displayName);
            return displayName;
        }
        String propertiesFile = "datasetTypes_" + locale.toLanguageTag() + ".properties";
        try {
            logger.fine("Looking up " + name + ".displayName in " + propertiesFile);
            return BundleUtil.getStringFromPropertyFile(name + ".displayName", "datasetTypes", locale);
        } catch (MissingResourceException e) {
            logger.warning(name + ".displayName missing from " + propertiesFile + " (or file does not exist). Returning English version.");
            return displayName;
        }
    }
}
