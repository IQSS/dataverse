package edu.harvard.iq.dataverse.search;

public class IndexResponse {

    private final String message;
    int numberOfSolrDocumentsIndexed;

    public IndexResponse(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "IndexResponse{" + "message=" + message + '}';
    }

    public String getMessage() {
        return message;
    }

    public int getNumberOfSolrDocumentsIndexed() {
        if (true) {
            /**
             * @todo remove all this, make it real, put in constructor, etc.
             */
            return 0;
        }
        return numberOfSolrDocumentsIndexed;
    }

}
