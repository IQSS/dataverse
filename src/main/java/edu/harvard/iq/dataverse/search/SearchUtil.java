package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.api.Util;
import java.sql.Timestamp;
import java.util.List;
import org.apache.commons.lang.StringUtils;
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
        /**
         * In general, don't mutate the query - 
         * because we want to support advanced search queries like
         * "title:foo" we can't simply escape the whole query with
         * `ClientUtils.escapeQueryChars(query)`:
         *
         * http://lucene.apache.org/solr/4_6_0/solr-solrj/org/apache/solr/client/solrj/util/ClientUtils.html#escapeQueryChars%28java.lang.String%29
         */

        query = query.replaceAll("doi:", "doi\\\\:")
                .replaceAll("hdl:", "hdl\\\\:")
                .replaceAll("datasetPersistentIdentifier:doi:", "datasetPersistentIdentifier:doi\\\\:")
                .replaceAll("datasetPersistentIdentifier:hdl:", "datasetPersistentIdentifier:hdl\\\\:");
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
        if(dvObjectSolrDoc.getFTPermissions()!=null) {
            solrInputDocument.addField(SearchFields.FULL_TEXT_SEARCHABLE_BY, dvObjectSolrDoc.getFTPermissions());
        }
        return solrInputDocument;
    }

    public static String getTimestampOrNull(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        /**
         * @todo Is seconds enough precision?
         */
        return Util.getDateTimeFormat().format(timestamp);
    }

    public static SortBy getSortBy(String sortField, String sortOrder) throws Exception {

        if (StringUtils.isBlank(sortField)) {
            sortField = SearchFields.RELEVANCE;
        } else if (sortField.equals("name")) {
            // "name" sounds better than "name_sort" so we convert it here so users don't have to pass in "name_sort"
            sortField = SearchFields.NAME_SORT;
        } else if (sortField.equals("date")) {
            // "date" sounds better than "release_or_create_date_dt"
            sortField = SearchFields.RELEASE_OR_CREATE_DATE;
        }

        if (StringUtils.isBlank(sortOrder)) {
            if (StringUtils.isNotBlank(sortField)) {
                // default sorting per field if not specified
                if (sortField.equals(SearchFields.RELEVANCE)) {
                    sortOrder = SortBy.DESCENDING;
                } else if (sortField.equals(SearchFields.NAME_SORT)) {
                    sortOrder = SortBy.ASCENDING;
                } else if (sortField.equals(SearchFields.RELEASE_OR_CREATE_DATE)) {
                    sortOrder = SortBy.DESCENDING;
                } else {
                    // asc for alphabetical by default despite GitHub using desc by default:
                    // "The sort order if sort parameter is provided. One of asc or desc. Default: desc"
                    // http://developer.github.com/v3/search/
                    sortOrder = SortBy.ASCENDING;
                }
            }
        }

        List<String> allowedSortOrderValues = SortBy.allowedOrderStrings();
        if (!allowedSortOrderValues.contains(sortOrder)) {
            throw new Exception("The 'order' parameter was '" + sortOrder + "' but expected one of " + allowedSortOrderValues + ". (The 'sort' parameter was/became '" + sortField + "'.)");
        }

        return new SortBy(sortField, sortOrder);
    }

    public static String determineFinalQuery(String userSuppliedQuery) {
        String wildcardQuery = "*";
        if (userSuppliedQuery == null) {
            return wildcardQuery;
        } else if (userSuppliedQuery.isEmpty()) {
            return wildcardQuery;
        } else {
            return userSuppliedQuery;
        }
    }

    /** expandQuery
     * 
     *  This method is only called when full-text indexing is on. It expands a simple query to search for the query items in the default field, where all metadata is indexed,
     *  and for the same query items in the full text field. 
     *  For security reasons, if the query is too complex to be parsed correctly by the current implementation, the method throws an exception.
     *  The current implementation does not parse any query with items restricted to specific fields (e.g. title:"Test"), or with use of range queries.
     *   
     * @param query
     * @param joinNeeded 
     * @return
     * @throws SearchException 
     */
    public static String expandQuery(String query, boolean joinNeeded) throws SearchException {
        if (!query.equals("*")) {
            if (!query.matches(".*[^\\\\][^\\\\][:].*")) {
                if (!query.matches(".*[\\{\\[\\]\\}].*")) {
                    String[] parts = query.split("\\s+");
                    StringBuilder ftQuery = new StringBuilder();
                    boolean firstTime = true;
                    for (String part : parts) {
                        if (!firstTime) {
                            ftQuery.append(" ");
                            firstTime = false;
                        }
                        if (!(part.equals("OR") || part.equals("AND") || part.equals("NOT"))) {
                            if (part.startsWith("+")) {
                                ftQuery.append("+" + SearchFields.FULL_TEXT + ":" + part.substring(1));
                            } else if (part.startsWith("-")) {
                                ftQuery.append("-" + SearchFields.FULL_TEXT + ":" + part.substring(1));
                            } else {
                                ftQuery.append(SearchFields.FULL_TEXT + ":" + part);
                            }
                        }
                    }
                    query = query + " OR (" + ftQuery.toString() + (joinNeeded ? "{!join from=" + SearchFields.DEFINITION_POINT + " to=id v=$q1})": ")");
                    // {!join from=definitionPointDocId to=id v=$q1}

                } else {
                    throw new SearchException("Range queries not supported with full-text indexing enabled.");
                }
            } else {
                throw new SearchException("Field-specific queries not supported with full-text indexing enabled.");
            }
        }
        return query;
    }

}
