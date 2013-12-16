package edu.harvard.iq.dataverse;

import java.util.List;

public class FacetCategory {

    private String name;
    private List<FacetLabel> facetLabel;

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

}
