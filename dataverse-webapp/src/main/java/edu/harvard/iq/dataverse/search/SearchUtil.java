package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.common.Util;
import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;
import edu.harvard.iq.dataverse.search.index.PermissionsSolrDoc;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.search.query.SortBy;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;

import java.sql.Timestamp;

public class SearchUtil {

    
    // -------------------- LOGIC --------------------

    public static SolrInputDocument createSolrDoc(PermissionsSolrDoc dvObjectSolrDoc) {
        if (dvObjectSolrDoc == null) {
            return null;
        }
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField(SearchFields.ID, dvObjectSolrDoc.getSolrId() + IndexServiceBean.discoverabilityPermissionSuffix);
        solrInputDocument.addField(SearchFields.DEFINITION_POINT, dvObjectSolrDoc.getSolrId());
        solrInputDocument.addField(SearchFields.DEFINITION_POINT_DVOBJECT_ID, dvObjectSolrDoc.getDvObjectId());
        solrInputDocument.addField(SearchFields.DISCOVERABLE_BY, dvObjectSolrDoc.getPermissions().getPermissions());
        solrInputDocument.addField(SearchFields.DISCOVERABLE_BY_PUBLIC_FROM, dvObjectSolrDoc.getPermissions().getPublicFrom().toString());
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
        
        String parsedSortField = parseSortField(sortField);
        SortOrder parsedSortOrder = parseSortOrder(sortOrder, parsedSortField);

        return new SortBy(parsedSortField, parsedSortOrder);
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

    // -------------------- PRIVATE --------------------
    
    private static String parseSortField(String sortField) {
        
        if (StringUtils.isBlank(sortField)) {
            return SearchFields.RELEVANCE;
        } else if (StringUtils.equals(sortField, "name")) {
            // "name" sounds better than "name_sort" so we convert it here so users don't have to pass in "name_sort"
            return SearchFields.NAME_SORT;
        } else if (StringUtils.equals(sortField, "date")) {
            // "date" sounds better than "release_or_create_date_dt"
            return SearchFields.RELEASE_OR_CREATE_DATE;
        }
        return sortField;
    }
    
    private static SortOrder parseSortOrder(String sortOrder, String parsedSortField) throws Exception {
        
        if (StringUtils.isBlank(sortOrder)) {
            // default sorting per field if not specified
            if (StringUtils.equals(parsedSortField, SearchFields.RELEVANCE)) {
                return SortOrder.desc;
            } else if (StringUtils.equals(parsedSortField, SearchFields.NAME_SORT)) {
                return SortOrder.asc;
            } else if (StringUtils.equals(parsedSortField, SearchFields.RELEASE_OR_CREATE_DATE)) {
                return SortOrder.desc;
            } else {
                // asc for alphabetical by default despite GitHub using desc by default:
                // "The sort order if sort parameter is provided. One of asc or desc. Default: desc"
                // http://developer.github.com/v3/search/
                return SortOrder.asc;
            }
        }
        
        return SortOrder.fromString(sortOrder)
            .orElseThrow(() -> new Exception("The 'order' parameter was '" + sortOrder + "' but expected one of " + SortOrder.allowedOrderStrings() + ". "
                    + "(The 'sort' parameter was/became '" + parsedSortField + "'.)"));
    }
}
