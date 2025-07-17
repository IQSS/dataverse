package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.MetadataBlock;
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

    public static final String DATASET_TYPE_DATASET = "dataset";
    public static final String DATASET_TYPE_SOFTWARE = "software";
    public static final String DATASET_TYPE_WORKFLOW = "workflow";
    public static final String DEFAULT_DATASET_TYPE = DATASET_TYPE_DATASET;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Any constraints? @Pattern regexp?
    @Column(nullable = false)
    private String name;

    /**
     * The metadata blocks this dataset type is linked to.
     */
    @ManyToMany(cascade = {CascadeType.MERGE})
    private List<MetadataBlock> metadataBlocks = new ArrayList<>();

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

    public List<MetadataBlock> getMetadataBlocks() {
        return metadataBlocks;
    }

    public void setMetadataBlocks(List<MetadataBlock> metadataBlocks) {
        this.metadataBlocks = metadataBlocks;
    }

    public JsonObjectBuilder toJson() {
        JsonArrayBuilder linkedMetadataBlocks = Json.createArrayBuilder();
        for (MetadataBlock metadataBlock : this.getMetadataBlocks()) {
            linkedMetadataBlocks.add(metadataBlock.getName());
        }
        return Json.createObjectBuilder()
                .add("id", getId())
                .add("name", getName())
                .add("linkedMetadataBlocks", linkedMetadataBlocks);
    }

}
