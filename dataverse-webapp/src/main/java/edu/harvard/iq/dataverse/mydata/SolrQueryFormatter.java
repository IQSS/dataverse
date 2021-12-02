package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.search.SearchFields;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Convenience methods for formatting long arrays of ids into solrQuery strings
 *
 * @author rmp553
 */
public class SolrQueryFormatter {

    private static int SOLR_ID_GROUP_SIZE = 1000;

    // -------------------- LOGIC --------------------

    /**
     * SOLR cannot parse over 1024 items in a boolean clause
     * Group IDs in batches of 1000
     */
    public String buildIdQuery(Set<Long> idListSet, String paramName, String dvObjectType) {
        if (paramName == null) {
            throw new NullPointerException("paramName cannot be null");
        }
        if (idListSet == null || idListSet.isEmpty()) {
            return null;
        }

        List<Long> idList = new ArrayList<>(idListSet);
        List<String> queryClauseParts = new ArrayList<>();

        int numIds = idList.size();
        int idCnt = 0;
        int numFullGroups = numIds / SOLR_ID_GROUP_SIZE;
        int extraIdCount = numIds % SOLR_ID_GROUP_SIZE; // Extra ids not evenly divisible by SOLR_ID_GROUP_SIZE

        List<Long> sliceOfIds;

        // Ids in groups of SOLR_ID_GROUP_SIZE
        for (int current_group_num = 0; current_group_num < numFullGroups; current_group_num++) {
            sliceOfIds = idList.subList(idCnt, SOLR_ID_GROUP_SIZE * (current_group_num + 1)); // slice group of ids off
            idCnt += sliceOfIds.size(); // add them to the count
            queryClauseParts.add(formatIdsForSolrClause(sliceOfIds, paramName, dvObjectType)); // format ids into solr OR clause
        }
        if (extraIdCount > 0) {
            sliceOfIds = idList.subList(idCnt, idCnt + extraIdCount); // slice group of ids off
            queryClauseParts.add(formatIdsForSolrClause(sliceOfIds, paramName, dvObjectType)); // format ids into solr OR clause
        }

        return StringUtils.join(queryClauseParts, " OR ");
    }

    // -------------------- PRIVATE --------------------

    private String formatIdsForSolrClause(List<Long> sliceOfIds, String paramName, String dvObjectType) { //='entityId'):
        if (paramName == null) {
            throw new NullPointerException("paramName cannot be null");
        }
        if (sliceOfIds == null) {
            throw new NullPointerException("sliceOfIds cannot be null");
        }
        if (sliceOfIds.isEmpty()) {
            throw new IllegalStateException("sliceOfIds must have at least 1 value");
        }

        String orClause = sliceOfIds.stream()
                .filter(Objects::nonNull)
                .map(id -> "" + id)
                .collect(Collectors.joining(" "));
        return dvObjectType != null
                ? String.format("(%s:(%s) AND %s:(%s))", paramName, orClause, SearchFields.TYPE, dvObjectType)
                : String.format("(%s:(%s))", paramName, orClause);
    }

    // -------------------- SETTERS --------------------

    public void setSolrIdGroupSize(int groupSize) {
        SOLR_ID_GROUP_SIZE = groupSize;
    }
}
