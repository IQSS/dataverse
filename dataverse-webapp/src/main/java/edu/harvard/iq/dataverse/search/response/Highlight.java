package edu.harvard.iq.dataverse.search.response;

import edu.harvard.iq.dataverse.search.SolrField;

import java.util.List;

public class Highlight {

    SolrField solrField;
    List<String> snippets;
    String displayName;

    public Highlight(SolrField solrField, List<String> snippets, String displayName) {
        this.solrField = solrField;
        this.snippets = snippets;
        this.displayName = displayName;
    }

    public SolrField getSolrField() {
        return solrField;
    }

    public List<String> getSnippets() {
        return snippets;
    }

    public String getDisplayName() {
        return displayName;
    }

}
