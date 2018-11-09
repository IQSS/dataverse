package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.api.Util;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;

public class SearchUtil {

    private static final Logger logger = Logger.getLogger(SearchUtil.class.getCanonicalName());
    
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
        

        if (dvObjectSolrDoc.getFTPermissions() != null) {
            if (dvObjectSolrDoc.getFTPermissions().size() > 0) {
                solrInputDocument.addField(SearchFields.FULL_TEXT_SEARCHABLE_BY, dvObjectSolrDoc.getFTPermissions());
            } else {
                solrInputDocument.addField(SearchFields.FULL_TEXT_SEARCHABLE_BY, dvObjectSolrDoc.getPermissions());
            }
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

    /**
     * expandQuery
     * 
     * This method is only called when full-text indexing is on. It expands a simple
     * query to search for the query items in the default field, where all metadata
     * is indexed, and for the same query items in the full text field. For security
     * reasons, if the query is too complex to be parsed correctly by the current
     * implementation, the method throws an exception. The current implementation
     * does not parse any query with items restricted to specific fields (e.g.
     * title:"Test"), or with use of range queries.
     * 
     * @param query
     * @param joinNeeded
     * @return
     * @throws SearchException
     */
    public static String expandQuery(String query, boolean joinNeeded) throws SearchException {
        // If it isn't 'find all'
        // Note that this query is used to populate the main Dataverse view and, without
        // this check, Dataverse assumes its a real search and displays the hit hints
        // instead of the normal summary
        StringBuilder ftQuery = new StringBuilder();
        if (!query.equals("*")) {
            // what about ~ * ? \ /
            // (\\"[^\\"]*\"|'[^']*'|[\\{\\[][^\\}\\]]*[\\}\\]] | [\\S]+)+
            // Split on any whitespace, but also grab any comma, do not split on comma only
            // (since comma only means the second term is still affected by any field:
            // prefix (in the original and when we expand below)
            // String[] parts = query.split(",*[\\s]+,*");
            // String[] parts = query.split("(\"[^\"]*\"|'[^']*'|[\\{\\[][^\\}\\]]*[\\}\\]]
            // | ,*[\\s]+,*)");

            boolean needSpace = false;
            /* Find:
             * 
             * A range query starting with an optional + or - with [ or { at the start and a } or ] at the end (can be mixed, e.g. {...])
             * A quoted phrase starting with an optional + or -
             * A text term that may include an initial : separated fieldname prefix and comma separated parts, but may not include phrases or ranges (which are treated by solr as new terms despite being ina comma-separated list)
             * See https://regexr.com/ to parse and test the patterns
             * 
             * This term is found by searching for strings of characters that don't include whitespace or "[{' or , (with ' being an ignored separator character for solr),
             * followed optionally by a comma and more characters that are not in the above list or a ":+ or - OR
             * a : and a range query OR
             * a : and a quoted string
             */
            Pattern termPattern = Pattern.compile("[+-]?[\\{\\[][^\\}\\]]*[\\}\\]](\\^\\d+)?|[+-]?\\\"[^\\\"]*\\\"(\\^\\d+)?|(([^\\s\"\\[\\{',\\(\\)\\\\]|[\\\\][\\[\\{\\(\\)\\\\+:'])+([,]?(([^\\s,\\[\\{'\":+\\(\\)\\\\-]|[\\\\][\\[\\{\\(\\)\\\\+:'])|[:][\\{\\[][^\\}\\]]*[\\}\\]]|[:]\\\"[^\\\"]*\\\")+)+)+|([^\\s\"',\\(\\)\\\\]|[\\\\][\\[\\{\\(\\)\\\\+:'])+");
            Matcher regexMatcher = termPattern.matcher(query);
            Pattern specialTokenPattern = Pattern.compile("\\(|\\)|OR|NOT|AND|&&|\\|\\||!|.*[^\\\\][^\\\\][:].*");
            Pattern forbiddenTokenPattern = Pattern.compile("\\\\|\\/|\\^|~|*|?");
            while (regexMatcher.find()) {

                String part = regexMatcher.group();
                logger.info("Parsing found \"" + part + "\"");
                if (needSpace) {
                    ftQuery.append(" ");
                } else {
                    needSpace = true;
                }
                // Don't proceed if there are special characters that are not part of another term (
                if (forbiddenTokenPattern.matcher(part).matches()) {
                    throw new SearchException(BundleUtil.getStringFromBundle("dataverse.search.fullText.error"));
                }
                // If its a boolean logic entry or
                // If it has a : that is not part of an escaped doi or handle (e.g. doi\:), e.g.
                // it is field-specific
                    
                if (!(specialTokenPattern.matcher(part).matches())) {
                    if (part.startsWith("+")) {
                        ftQuery.append(expandPart(part + " OR (+" + SearchFields.FULL_TEXT + ":" + part.substring(1), joinNeeded));
                    } else if (part.startsWith("-")) {
                        ftQuery.append(expandPart(part + " OR (-" + SearchFields.FULL_TEXT + ":" + part.substring(1), joinNeeded));
                    } else if (part.startsWith("-")) {
                        ftQuery.append(expandPart(part + " OR (!" + SearchFields.FULL_TEXT + ":" + part.substring(1), joinNeeded));
                    } else {
                        ftQuery.append(expandPart(part + " OR (" + SearchFields.FULL_TEXT + ":" + part, joinNeeded));
                    }
                } else {
                    if (part.contains(SearchFields.FULL_TEXT + ":")) {
                        // Any reference to the FULL_TEXT field has to be joined with the permission
                        // term
                        ftQuery.append(expandPart("(" + part, joinNeeded));
                    } else {
                        if(!(part.equals("\\") || part.equals("/"))) {
                        ftQuery.append(part);
                        }
                    }
                }
            }
        } else {
            ftQuery.append("*");
        }
        return ftQuery.toString();
    }

    private static Object expandPart(String part, boolean joinNeeded) {
        // TODO Auto-generated method stub
        return "(" + part + (joinNeeded ? " AND {!join from=" + SearchFields.DEFINITION_POINT + " to=id v=$q1}))" : "))");
    }

}
