package edu.harvard.iq.dataverse.search;

import java.util.List;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class FacetCategory {

    private String name;
    private List<FacetLabel> facetLabel;
    private String friendlyName;

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

    public String getDisplayName() {
        String bundleDisplayName = getStringFromBundle(format("facets.search.fieldtype.%s.label", this.name));
        return isNotBlank(bundleDisplayName) ? bundleDisplayName : friendlyName;
    }

    @Override
    public String toString() {
        return "FacetCategory [name=" + name + ", facetLabel=" + facetLabel + ", friendlyName=" + friendlyName + "]";
    }
    
}
