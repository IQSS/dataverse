package edu.harvard.iq.dataverse.datasetrelation;

import java.io.Serializable;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import jakarta.persistence.*;

/**
 * Represents a relationship between datasets. This is an abstract class that defines basic properties and functionality
 * for dataset relations, such as their originating dataset, relation type, and the version of the dataset definition
 * point.
 *
 * @author Vera Clemens (ZB MED)
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "relation_source", discriminatorType = DiscriminatorType.STRING)
@Table(indexes = {
        @Index(name="index_datasetrelation_dataset", columnList="dataset_id"),
        @Index(name="index_datasetrelation_relateddataset", columnList="relateddataset_id"),
        @Index(name="index_datasetrelation_definitionpoint", columnList="definitionpoint_id"),
        @Index(name="index_datasetrelation_relateddataset_definitionpoint", columnList="relateddataset_id, definitionpoint_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "datasetrelation_internal_unique",
                        columnNames = {"dataset_id", "relateddataset_id", "relationtype_id", "definitionpoint_id"}
                ),
                @UniqueConstraint(
                        name = "datasetrelation_external_unique",
                        columnNames = {"dataset_id", "externalidentifier", "relationtype_id", "definitionpoint_id"}
                )
        }
)
@NamedQueries({
        @NamedQuery(name = "DatasetRelation.getRelationById",
                query="SELECT rel FROM DatasetRelation rel WHERE rel.id=:id"),
        @NamedQuery(name = "DatasetRelation.deleteRelationById",
                query="DELETE FROM DatasetRelation rel WHERE rel.id=:id"),
        @NamedQuery(name = "DatasetRelation.removeRelationsByDatasetVersionId",
                query = "DELETE FROM DatasetRelation rel WHERE rel.definitionPoint.id=:versionId"),
        @NamedQuery(name = "DatasetRelation.getRelationsDefinedAtDatasetVersionId",
                query="SELECT rel FROM DatasetRelation rel WHERE rel.definitionPoint.id=:versionId")
})
public abstract class DatasetRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable=false)
    // The dataset from which this relation originates.
    private Dataset dataset;

    @ManyToOne
    @JoinColumn(nullable=false)
    private DatasetVersion definitionPoint;

    @ManyToOne
    @JoinColumn(nullable=false)
    private DatasetRelationType relationType;

    /**
     * JPA no-args constructor. Client code should use the public constructor
     * and not this one.
     */
    protected DatasetRelation(){}

    protected DatasetRelation(Dataset dataset, DatasetRelationType relationType, DatasetVersion definitionPoint) {
        this.dataset = dataset;
        this.relationType = relationType;
        this.definitionPoint = definitionPoint;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public DatasetVersion getDefinitionPoint() {
        return definitionPoint;
    }

    public void setDefinitionPoint(DatasetVersion definitionPoint) {
        this.definitionPoint = definitionPoint;
    }

    public DatasetRelationType getRelationType() {
        return relationType;
    }

    public void setRelationType(DatasetRelationType type) {
        this.relationType = type;
    }

    @Override
    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    @Override
    public boolean equals(Object object) {
        if ( object == null ) return false;
        if ( object == this ) return true;

        if (!(object instanceof DatasetRelation)) {
            return false;
        }
        DatasetRelation other = (DatasetRelation) object;

        return (id==null && other.id==null) || (id!=null && id.equals(other.getId()));
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.datasetrelation.DatasetRelation[ id=" + id + " ]";
    }

    public abstract String toKey();

    /**
     * Returns a key for comparing the same relation across different dataset
     * versions. Unlike {@link #toKey()}, it does not include the definition point.
     */
    public abstract String toVersionComparisonKey();

    public abstract DatasetRelation copy(DatasetVersion newDefinitionPoint);

}
