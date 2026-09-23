package edu.harvard.iq.dataverse.datasetrelation;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Represents a relationship between a dataset and an external dataset. This class is used to describe and manage links
 * to datasets that exist outside the current Dataverse instance.
 *
 * This class extends {@link DatasetRelation} and specializes the relationship to describe external datasets by
 * providing additional fields for external-specific details like external identifier, identifier scheme, and dataset
 * type.
 *
 * @author Vera Clemens (ZB MED)
 */
@Entity
@DiscriminatorValue("external")
public class ExternalDatasetRelation extends DatasetRelation {

    @Column(length = 512)
    private String externalIdentifier;
    private String identifierScheme;
    private String datasetType;

    public ExternalDatasetRelation(Dataset dataset, String externalIdentifier, String identifierScheme, String datasetType, DatasetRelationType relationType, DatasetVersion definitionPoint) {
        super(dataset, relationType, definitionPoint);
        this.externalIdentifier = externalIdentifier;
        this.identifierScheme = identifierScheme;
        this.datasetType = datasetType;
    }

    protected ExternalDatasetRelation() {
        super();
    }

    public String getExternalIdentifier() {
        return externalIdentifier;
    }

    public void setExternalIdentifier(String externalIdentifier) {
        this.externalIdentifier = externalIdentifier;
    }

    public String getIdentifierScheme() {
        return identifierScheme;
    }

    public void setIdentifierScheme(String identifierScheme) {
        this.identifierScheme = identifierScheme;
    }

    public String getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }

    @Override
    public String toKey() {
        return toVersionComparisonKey() + "|" + getDefinitionPoint().getId();
    }

    @Override
    public String toVersionComparisonKey() {
        return getDataset().getId() + "|" + externalIdentifier + "|" + (identifierScheme != null ? identifierScheme : "") + "|" + (datasetType != null ? datasetType : "") + "|" + (getRelationType() != null ? getRelationType().getId() : "");
    }

    @Override
    public DatasetRelation copy(DatasetVersion newDefinitionPoint) {
        return new ExternalDatasetRelation(getDataset(), externalIdentifier, identifierScheme, datasetType, getRelationType(), newDefinitionPoint);
    }
}
