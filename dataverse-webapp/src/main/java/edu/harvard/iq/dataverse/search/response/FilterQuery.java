package edu.harvard.iq.dataverse.search.response;

public class FilterQuery {

    private String query;
    private String friendlyFieldName;
    private String friendlyFieldValue;

    // -------------------- CONSTRUCTORS --------------------

    public FilterQuery(String query) {
        this.query = query;
    }

    public FilterQuery(String query, String friendlyFieldName, String friendlyFieldValue) {
        this.query = query;
        this.friendlyFieldName = friendlyFieldName;
        this.friendlyFieldValue = friendlyFieldValue;
    }

    // -------------------- GETTERS --------------------

    public String getQuery() {
        return query;
    }

    public String getFriendlyFieldName() {
        return friendlyFieldName;
    }

    public String getFriendlyFieldValue() {
        return friendlyFieldValue;
    }

    // -------------------- LOGIC --------------------

    public boolean hasFriendlyNameAndValue() {
        return friendlyFieldName != null && friendlyFieldValue != null;
    }
}
