package edu.harvard.iq.dataverse.search.response;

public class FacetLabel {

    private String name;
    private String displayName;
    private Long count;
    private String filterQuery;

    public FacetLabel(String name, String displayName, Long count) {
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

    @Override
    public String toString() {
        return "FacetLabel [name=" + name + ", displayName=" + displayName + ", count=" + count + ", filterQuery="
                + filterQuery + "]";
    }
}
