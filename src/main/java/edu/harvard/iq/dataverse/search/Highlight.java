package edu.harvard.iq.dataverse.search;

import java.util.List;
import jakarta.inject.Named;

@Named
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
