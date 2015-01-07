package edu.harvard.iq.dataverse.search;

import java.util.Arrays;
import java.util.List;

public class SortBy {

    public static final String ASCENDING = "asc";
    public static final String DESCENDING = "desc";

    public static List<String> allowedOrderStrings() {
        return Arrays.asList(ASCENDING, DESCENDING);
    }

    private final String field;
    private final String order;

    public SortBy(String field, String order) {
        this.field = field;
        this.order = order;
    }

    @Override
    public String toString() {
        return "SortBy{" + "field=" + field + ", order=" + order + '}';
    }

    public String getField() {
        return field;
    }

    public String getOrder() {
        return order;
    }

}
