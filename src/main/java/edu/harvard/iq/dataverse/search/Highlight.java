package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.SolrField;
import java.util.List;
import javax.inject.Named;

@Named
public class Highlight {

    SolrField solrField;
    List<String> snippets;

    public Highlight(SolrField solrField, List<String> snippets) {
        this.solrField = solrField;
        this.snippets = snippets;
    }

    public List<String> getSnippets() {
        return snippets;
    }

    public SolrField getSolrField() {
        return solrField;
    }

}
