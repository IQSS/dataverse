package edu.harvard.iq.dataverse.dataset;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.Serializable;

@NamedQueries({
    @NamedQuery(name = "DatasetType.findAll",
            query = "SELECT d FROM DatasetType d"),
    @NamedQuery(name = "DatasetType.findByName",
            query = "SELECT d FROM DatasetType d WHERE d.name=:name"),
    @NamedQuery(name = "DatasetType.deleteById",
            query = "DELETE FROM DatasetType d WHERE d.id=:id"),})
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = "name"),}
)

public class DatasetType implements Serializable {

    public static final String DEFAULT_DATASET_TYPE = "dataset";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Any constraints? @Pattern regexp?
    @Column(nullable = false)
    private String name;

    /**
     * This default constructor is only here to prevent this error at
     * deployment:
     *
     * Exception Description: The instance creation method
     * [...DatasetType.<Default Constructor>], with no parameters, does not
     * exist, or is not accessible
     *
     * Don't use it.
     */
    @Deprecated
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

    public JsonObjectBuilder toJson() {
        return Json.createObjectBuilder()
                .add("id", getId())
                .add("name", getName());
    }

}
