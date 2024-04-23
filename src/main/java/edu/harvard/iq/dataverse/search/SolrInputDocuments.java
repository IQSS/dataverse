package edu.harvard.iq.dataverse.search;

import java.util.Collection;

import org.apache.solr.common.SolrInputDocument;

public class SolrInputDocuments {
    private Collection<SolrInputDocument> documents;
    private String message;
    private Long datasetId;

    public SolrInputDocuments(Collection<SolrInputDocument> documents, String message, Long datasetId) {
        this.documents = documents;
        this.message = message;
        this.datasetId = datasetId;
    }

    public Collection<SolrInputDocument> getDocuments() {
        return documents;
    }

    public String getMessage() {
        return message;
    }

    public Long getDatasetId() {
        return datasetId;
    }
}