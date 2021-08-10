package edu.harvard.iq.dataverse.search.response;

import java.util.ArrayList;
import java.util.List;

public class FacetCategory {

    private String name;
    private List<FacetLabel> facetLabels = new ArrayList<>();
    private String friendlyName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addFacetLabel(FacetLabel facetLabel) {
        this.facetLabels.add(facetLabel);
    }

    public List<FacetLabel> getFacetLabels() {
        return facetLabels;
    }

    public void setFacetLabels(List<FacetLabel> facetLabel) {
        this.facetLabels = facetLabel;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getDisplayName() {
        return getFriendlyName();
    }

    @Override
    public String toString() {
        return "FacetCategory [name=" + name + ", facetLabels=" + facetLabels + ", friendlyName=" + friendlyName + "]";
    }
    
}
