package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.common.BundleUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.MissingResourceException;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class FacetLabel {

    private String name;
    private String displayName;
    private Long count;
    private String filterQuery;

    FacetLabel(String name, String displayName, Long count) {
        this.name = name;
        this.displayName = displayName;
        this.count = count;
    }

    /**
     * @todo should we simply store as "Dataverses" rather than "dataverses" in
     * Solr?
     */
    public String getCapitalizedName() {
        return Character.toUpperCase(this.name.charAt(0)) + this.name.substring(1);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public String getFilterQuery() {
        return filterQuery;
    }

    public void setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
    }
}
