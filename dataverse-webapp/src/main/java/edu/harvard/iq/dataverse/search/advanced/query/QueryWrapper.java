package edu.harvard.iq.dataverse.search.advanced.query;

import java.util.ArrayList;
import java.util.List;

public class QueryWrapper {
    private String query;
    private List<String> additions = new ArrayList<>();

    // -------------------- CONSTRUCTORS --------------------

    public QueryWrapper(String query) {
        this.query = query;
    }

    // -------------------- GETTERS --------------------

    public String getQuery() {
        return query;
    }

    public List<String> getFilters() {
        return additions;
    }

    // -------------------- SETTERS --------------------

    public void setQuery(String query) {
        this.query = query;
    }
}
