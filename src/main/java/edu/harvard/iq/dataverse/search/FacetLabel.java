package edu.harvard.iq.dataverse.search;

import jakarta.inject.Named;

@Named
public class FacetLabel implements Comparable<FacetLabel>{

    private String name;
    private Long count;
    private String filterQuery;

    public FacetLabel(String name, Long count) {
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

    @Override
    public int compareTo(FacetLabel otherFacetLabel) {
        // This is used to 'chronologically' order entries in the Publication Year facet
        // display. That should work for 4 digit years (until 10K AD), but this could be
        // changed to do a real numberical comparison instead.
        return name.compareTo(otherFacetLabel.getName());
    }

}
