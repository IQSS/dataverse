package edu.harvard.iq.dataverse.search.response;

public class SearchParentInfo {

    private String id;
    private String name;
    private String citation;
    private String parentIdentifier;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCitation() {
        return citation;
    }

    public String getParentIdentifier() {
        return parentIdentifier;
    }

    public SearchParentInfo setId(String id) {
        this.id = id;
        return this;
    }

    public SearchParentInfo setName(String name) {
        this.name = name;
        return this;
    }

    public SearchParentInfo setCitation(String citation) {
        this.citation = citation;
        return this;
    }

    public SearchParentInfo setParentIdentifier(String parentIdentifier) {
        this.parentIdentifier = parentIdentifier;
        return this;
    }

    public boolean isInfoMissing() {
        return id == null && name == null && citation == null && parentIdentifier == null;
    }
}
