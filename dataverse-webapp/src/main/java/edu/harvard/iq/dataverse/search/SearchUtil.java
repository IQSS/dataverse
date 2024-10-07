package edu.harvard.iq.dataverse.search;

import static edu.harvard.iq.dataverse.common.Util.getDateTimeFormat;
import static edu.harvard.iq.dataverse.search.SearchFields.ADD_DATASET_PERM;
import static edu.harvard.iq.dataverse.search.SearchFields.DEFINITION_POINT;
import static edu.harvard.iq.dataverse.search.SearchFields.DEFINITION_POINT_DVOBJECT_ID;
import static edu.harvard.iq.dataverse.search.SearchFields.DISCOVERABLE_BY;
import static edu.harvard.iq.dataverse.search.SearchFields.DISCOVERABLE_BY_PUBLIC_FROM;
import static edu.harvard.iq.dataverse.search.SearchFields.ID;
import static edu.harvard.iq.dataverse.search.SearchFields.NAME_SORT;
import static edu.harvard.iq.dataverse.search.SearchFields.RELEASE_OR_CREATE_DATE;
import static edu.harvard.iq.dataverse.search.SearchFields.RELEVANCE;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.sql.Timestamp;

import org.apache.solr.common.SolrInputDocument;

import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.search.index.PermissionsSolrDoc;
import edu.harvard.iq.dataverse.search.query.SortBy;

public class SearchUtil {

    // -------------------- LOGIC --------------------

    public static SolrInputDocument createSolrDoc(final PermissionsSolrDoc permDoc) {

        if (permDoc != null) {
            final SolrInputDocument result = new SolrInputDocument();

            result.addField(ID,
                    permDoc.getSolrId() + IndexServiceBean.discoverabilityPermissionSuffix);
            result.addField(DEFINITION_POINT, permDoc.getSolrId());
            result.addField(DEFINITION_POINT_DVOBJECT_ID, permDoc.getDvObjectId());
            result.addField(DISCOVERABLE_BY, permDoc.getSearchPermissions().getPermissions());
            result.addField(DISCOVERABLE_BY_PUBLIC_FROM,
                    permDoc.getSearchPermissions().getPublicFrom().toString());
            result.addField(ADD_DATASET_PERM,
                    permDoc.getAddDatasetPermissions().getPermittedEntities());

            return result;
        } else {
            return null;
        }
    }

    public static String getTimestampOrNull(final Timestamp timestamp) {

        return timestamp != null ? getDateTimeFormat().format(timestamp) : null;
    }

    public static SortBy getSortBy(final String sortField, final String sortOrder)
            throws Exception {

        final String parsedSortField = parseSortField(sortField);
        final SortOrder parsedSortOrder = parseSortOrder(sortOrder, parsedSortField);

        return new SortBy(parsedSortField, parsedSortOrder);
    }

    public static String determineFinalQuery(final String userSuppliedQuery) {

        return isBlank(userSuppliedQuery) ? "*" : userSuppliedQuery;
    }

    // -------------------- PRIVATE --------------------

    private static String parseSortField(final String sortField) {

        if (isBlank(sortField)) {
            return RELEVANCE;
        } else if ("name".equals(sortField)) {
            // "name" sounds better than "name_sort" so we convert it here so users don't
            // have to pass in "name_sort"
            return NAME_SORT;
        } else if ("date".equals(sortField)) {
            // "date" sounds better than "release_or_create_date_dt"
            return RELEASE_OR_CREATE_DATE;
        } else {
            return sortField;
        }
    }

    private static SortOrder parseSortOrder(final String sortOrder,
            final String parsedSortField) throws Exception {

        if (isBlank(sortOrder)) {
            // default sorting per field if not specified
            return SortOrder.defaultFor(parsedSortField);
        } else {
            return SortOrder.fromString(sortOrder)
                    .orElseThrow(() -> new Exception("The 'order' parameter was '" + sortOrder
                            + "' but expected one of " + SortOrder.allowedOrderStrings() + ". "
                            + "(The 'sort' parameter was/became '" + parsedSortField + "'.)"));
        }
    }
}
