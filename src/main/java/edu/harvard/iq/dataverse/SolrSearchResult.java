package edu.harvard.iq.dataverse;

import java.util.List;

public class SolrSearchResult {

    String query;
    String name;
    List<String> highlightSnippets;

    SolrSearchResult(String queryFromUser, List<String> highlightSnippets, String name) {
        this.query = queryFromUser;
        this.name = name;
        this.highlightSnippets = highlightSnippets;
    }
    
    @Override
    public String toString(){
        return this.name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getHighlightSnippets() {
        return highlightSnippets;
    }

    public void setHighlightSnippets(List<String> highlightSnippets) {
        this.highlightSnippets = highlightSnippets;
    }

}
