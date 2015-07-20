package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.IndexServiceBean;
import org.apache.solr.common.SolrInputDocument;

public class SearchUtil {

    /**
     * @param query The query string that might be mutated before feeding it
     * into Solr.
     * @return The query string that may have been mutated or null if null was
     * passed in.
     */
    public static String sanitizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String[] colonParts = query.split(":");
        if (colonParts.length > 0) {
            String first = colonParts[0];
            if (first.startsWith("doi")) {
                query = query.replaceAll("doi:", "doi\\\\:");
            } else if (first.startsWith("hdl")) {
                query = query.replaceAll("hdl:", "hdl\\\\:");
            } else if (first.startsWith("datasetPersistentIdentifier")) {
                query = query.replaceAll("datasetPersistentIdentifier:doi:", "datasetPersistentIdentifier:doi\\\\:");
                query = query.replaceAll("datasetPersistentIdentifier:hdl:", "datasetPersistentIdentifier:hdl\\\\:");
            } else {
                /**
                 * No-op, don't mutate the query.
                 *
                 * Because we want to support advanced search queries like
                 * "title:foo" we can't simply escape the whole query with
                 * `ClientUtils.escapeQueryChars(query)`:
                 *
                 * http://lucene.apache.org/solr/4_6_0/solr-solrj/org/apache/solr/client/solrj/util/ClientUtils.html#escapeQueryChars%28java.lang.String%29
                 */
            }
        }
        return query;
    }

    public static SolrInputDocument createSolrDoc(DvObjectSolrDoc dvObjectSolrDoc) {
        if (dvObjectSolrDoc == null) {
            return null;
        }
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField(SearchFields.ID, dvObjectSolrDoc.getSolrId() + IndexServiceBean.discoverabilityPermissionSuffix);
        solrInputDocument.addField(SearchFields.DEFINITION_POINT, dvObjectSolrDoc.getSolrId());
        solrInputDocument.addField(SearchFields.DEFINITION_POINT_DVOBJECT_ID, dvObjectSolrDoc.getDvObjectId());
        solrInputDocument.addField(SearchFields.DISCOVERABLE_BY, dvObjectSolrDoc.getPermissions());
        return solrInputDocument;
    }

}
