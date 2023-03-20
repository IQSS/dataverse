package edu.harvard.iq.dataverse.search.advanced;

import edu.harvard.iq.dataverse.search.advanced.field.SearchField;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPart;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPartType;
import edu.harvard.iq.dataverse.search.advanced.query.QueryWrapper;

import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/** Class used for creating query used in advanced search. */
@Stateless
public class QueryWrapperCreator {

    // -------------------- LOGIC --------------------

    /** Creates wrapped query for given Search Blocks */
    public QueryWrapper constructQueryWrapper(List<SearchBlock> searchBlocks) {
        Map<QueryPartType, List<String>> collectedQueryParts = groupQueryParts(searchBlocks);
        String query = String.join(" AND ", collectedQueryParts.getOrDefault(QueryPartType.QUERY, Collections.emptyList()));
        QueryWrapper wrapper = new QueryWrapper(query);
        wrapper.getFilters().addAll(collectedQueryParts.getOrDefault(QueryPartType.FILTER, Collections.emptyList()));
        return wrapper;
    }

    // -------------------- PRIVATE --------------------

    private Map<QueryPartType, List<String>> groupQueryParts(List<SearchBlock> searchBlocks) {
        Map<QueryPartType, List<String>> collectedQueryParts = new HashMap<>();
        Set<String> filters = new HashSet<>();
        for (SearchBlock block : searchBlocks) {
            for (SearchField field : block.getSearchFields()) {
                QueryPart queryPart = field.getQueryPart();
                QueryPartType key = queryPart.queryPartType;
                String queryFragment = queryPart.queryFragment;
                if (key == QueryPartType.FILTER) {
                    if (filters.contains(queryFragment)) {
                        continue;
                    }
                    filters.add(queryFragment);
                }
                if (collectedQueryParts.containsKey(key)) {
                    collectedQueryParts.get(key).add(queryFragment);
                } else {
                    collectedQueryParts.put(key, new ArrayList<>(Collections.singletonList(queryFragment)));
                }
            }
        }
        return collectedQueryParts;
    }
}
