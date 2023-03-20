package edu.harvard.iq.dataverse.search.query.filter;

import edu.harvard.iq.dataverse.search.response.FilterQuery;
import org.apache.commons.lang3.StringUtils;

public class SpecialFilter {
    public static final SpecialFilter EMPTY = new SpecialFilter(StringUtils.EMPTY, StringUtils.EMPTY, null);

    public final String query;
    public final String solrQuery;
    public final FilterQuery filterQuery;

    // -------------------- CONSTURCTORS --------------------

    public SpecialFilter(String query, String solrQuery, FilterQuery filterQuery) {
        this.query = query;
        this.solrQuery = solrQuery;
        this.filterQuery = filterQuery;
    }
}
