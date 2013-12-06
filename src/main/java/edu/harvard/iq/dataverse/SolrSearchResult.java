package edu.harvard.iq.dataverse;

import java.util.List;

public class SolrSearchResult {

    private String query;
    private String name;
    private Long hits;
    private List<String> highlightSnippets;

    SolrSearchResult(String queryFromUser, List<String> highlightSnippets, String name) {
        this.query = queryFromUser;
        this.name = name;
        this.highlightSnippets = highlightSnippets;
    }

    @Override
    public String toString() {
        return this.name;
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

    public Long getHits() {
        return hits;
    }

    public void setHits(Long hits) {
        this.hits = hits;
    }

    public List<String> getHighlightSnippets() {
        return highlightSnippets;
    }

    public void setHighlightSnippets(List<String> highlightSnippets) {
        this.highlightSnippets = highlightSnippets;
    }

}
