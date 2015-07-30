package edu.harvard.iq.dataverse.search;

import javax.inject.Named;

@Named
public class FacetLabel {

    private String name;
    private Long count;
    private String filterQuery;

    FacetLabel(String name, Long count) {
        this.name = name;
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
