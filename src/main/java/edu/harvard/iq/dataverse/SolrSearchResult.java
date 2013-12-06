package edu.harvard.iq.dataverse;

import java.util.List;

public class SolrSearchResult {

    private Long id;
    private String query;
    private String name;
    private List<String> highlightSnippets;

    SolrSearchResult(String queryFromUser, List<String> highlightSnippets, String name) {
        this.query = queryFromUser;
        this.name = name;
        this.highlightSnippets = highlightSnippets;
    }

    @Override
    public String toString() {
        /**
         * @todo improve string representation
         */
        return this.id + "|" + this.name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getHighlightSnippets() {
        return highlightSnippets;
    }

    public void setHighlightSnippets(List<String> highlightSnippets) {
        this.highlightSnippets = highlightSnippets;
    }

}
