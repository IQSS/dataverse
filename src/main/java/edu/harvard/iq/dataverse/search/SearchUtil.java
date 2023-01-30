package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.api.Util;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.commons.lang3.StringUtils;
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

    public static String constructQuery(String solrField, String userSuppliedQuery) {
       return constructQuery(solrField, userSuppliedQuery, false);
    }
    
    public static String constructQuery(String solrField, String userSuppliedQuery, boolean addQuotes) {

        StringBuilder queryBuilder = new StringBuilder();
        String delimiter = "[\"]+";

        List<String> queryStrings = new ArrayList<>();

        if (userSuppliedQuery != null && !userSuppliedQuery.equals("")) {
            if (userSuppliedQuery.contains("\"")) {
                String[] tempString = userSuppliedQuery.split(delimiter);
                for (int i = 1; i < tempString.length; i++) {
                    if (!tempString[i].equals(" ") && !tempString[i].isEmpty()) {
                        queryStrings.add(solrField + ":" + "\"" + tempString[i].trim() + "\"");
                    }
                }
            } else {
                StringTokenizer st = new StringTokenizer(userSuppliedQuery);
                while (st.hasMoreElements()) {
                    String nextElement = (String) st.nextElement();
                    //Entries such as URIs will get tokenized into individual words by solr unless they are in quotes
                    if(addQuotes) {
                        nextElement = "\"" + nextElement + "\"";
                    }
                    queryStrings.add(solrField + ":" + nextElement);
                }
            }
        }

        if (queryStrings.size() > 1) {
            queryBuilder.append("(");
        }

        for (int i = 0; i < queryStrings.size(); i++) {
            if (i > 0) {
                queryBuilder.append(" ");
            }
            queryBuilder.append(queryStrings.get(i));
        }

        if (queryStrings.size() > 1) {
            queryBuilder.append(")");
        }

        return queryBuilder.toString().trim();
    }
    
    public static String constructQuery(List<String> queryStrings, boolean isAnd) {
        return constructQuery(queryStrings, isAnd, true);
    }

    public static String constructQuery(List<String> queryStrings, boolean isAnd, boolean surroundWithParens) {
        StringBuilder queryBuilder = new StringBuilder();

        int count = 0;
        for (String string : queryStrings) {
            if (!StringUtils.isBlank(string)) {
                if (++count > 1) {
                    queryBuilder.append(isAnd ? " AND " : " OR ");
                }
                queryBuilder.append(string);
            }
        }

        if (surroundWithParens && count > 1) {
            queryBuilder.insert(0, "(");
            queryBuilder.append(")");
        }

        return queryBuilder.toString().trim();
    }

    /**
     * @return Null if supplied point is null or whitespace.
     * @throws IllegalArgumentException If the lat/long is not separated by a
     * comma.
     * @throws NumberFormatException If the lat/long values are not numbers.
     */
    public static String getGeoPoint(String userSuppliedGeoPoint) throws IllegalArgumentException, NumberFormatException {
        if (userSuppliedGeoPoint == null || userSuppliedGeoPoint.isBlank()) {
            return null;
        }
        String[] parts = userSuppliedGeoPoint.split(",");
        // We'll supply our own errors but Solr gives a decent one:
        // "Point must be in 'lat, lon' or 'x y' format: 42.3;-71.1"
        if (parts.length != 2) {
            String msg = "Must contain a single comma to separate latitude and longitude.";
            throw new IllegalArgumentException(msg);
        }
        float latitude = Float.parseFloat(parts[0]);
        float longitude = Float.parseFloat(parts[1]);
        return latitude + "," + longitude;
    }

    /**
     * @return Null if supplied radius is null or whitespace.
     * @throws NumberFormatException If the radius is not a positive number.
     */
    public static String getGeoRadius(String userSuppliedGeoRadius) throws NumberFormatException {
        if (userSuppliedGeoRadius == null || userSuppliedGeoRadius.isBlank()) {
            return null;
        }
        float radius = 0;
        try {
            radius = Float.parseFloat(userSuppliedGeoRadius);
        } catch (NumberFormatException ex) {
            String msg = "Non-number radius supplied.";
            throw new NumberFormatException(msg);
        }
        if (radius <= 0) {
            String msg = "The supplied radius must be greater than zero.";
            throw new NumberFormatException(msg);
        }
        return userSuppliedGeoRadius;
    }

}
