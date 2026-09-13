package edu.harvard.iq.dataverse.api.dto;

/**
 * Data Transfer Object (DTO) representing a dataset relation.
 * This class encapsulates the information required to describe a relationship between datasets.
 *
 * @author Vera Clemens (ZB MED)
 */
public class DatasetRelationDTO {
    private String relationTypeName;

    // For internal dataset relations
    private String relatedDatasetPid;

    // For external dataset relations
    private String externalIdentifier;
    private String identifierScheme;
    private String datasetType;

    public String getRelatedDatasetPid() {
        return relatedDatasetPid;
    }

    public void setRelatedDatasetPid(String relatedDatasetPid) {
        this.relatedDatasetPid = relatedDatasetPid;
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

    public String getRelationTypeName() {
        return relationTypeName;
    }

    public void setRelationTypeName(String relationTypeName) {
        this.relationTypeName = relationTypeName;
    }

    public String getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }
}
