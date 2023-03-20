package edu.harvard.iq.dataverse.search.query.filter;

public interface SpecialFilterCreator {
    String getKey();

    SpecialFilter create(String query, String... params);
}
