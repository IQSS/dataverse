package edu.harvard.iq.dataverse.search;

import java.util.List;

public class FacetCategory {

    private String name;
    private List<FacetLabel> facetLabel;
    private String friendlyName;
    private Long datasetFieldTypeId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<FacetLabel> getFacetLabel() {
        return facetLabel;
    }

    public void setFacetLabel(List<FacetLabel> facetLabel) {
        this.facetLabel = facetLabel;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public Long getDatasetFieldTypeId() {
        return datasetFieldTypeId;
    }

    public void setDatasetFieldTypeId(Long datasetFieldId) {
        this.datasetFieldTypeId = datasetFieldId;
    }

}
