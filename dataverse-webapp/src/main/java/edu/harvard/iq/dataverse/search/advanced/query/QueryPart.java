package edu.harvard.iq.dataverse.search.advanced.query;

import org.apache.commons.lang3.StringUtils;

public class QueryPart {
    public static final QueryPart EMPTY = new QueryPart(QueryPartType.NONE, StringUtils.EMPTY);

    public final QueryPartType queryPartType;
    public final String queryFragment;

    // -------------------- CONSTRUCTORS --------------------

    public QueryPart(QueryPartType queryPartType, String queryFragment) {
        this.queryPartType = queryPartType;
        this.queryFragment = queryFragment;
    }
}
