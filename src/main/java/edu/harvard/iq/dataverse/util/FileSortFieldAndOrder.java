package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.search.SortBy;
import org.apache.commons.lang.StringUtils;

public class FileSortFieldAndOrder {

    private String sortField;
    private String sortOrder;

    public static String label = "label";
    public static String createDate = "dataFile.createDate";
    public static String size = "dataFile.filesize";
    public static String type = "dataFile.contentType";

    public FileSortFieldAndOrder(String userSuppliedSortField, String userSuppliedSortOrder) {
        if (StringUtils.isBlank(userSuppliedSortField)) {
            sortField = label;
        } else if (userSuppliedSortField.equals(label) || userSuppliedSortField.equals(createDate) || userSuppliedSortField.equals(size) || userSuppliedSortField.equals(type)) {
            sortField = userSuppliedSortField;
        } else {
            sortField = label;
        }
        if (StringUtils.isBlank(userSuppliedSortOrder)) {
            sortOrder = SortBy.ASCENDING;
        } else if (userSuppliedSortOrder.equals(SortBy.ASCENDING) || userSuppliedSortOrder.equals(SortBy.DESCENDING)) {
            sortOrder = userSuppliedSortOrder;
        } else {
            sortOrder = SortBy.ASCENDING;
        }
        this.sortField = sortField;
        this.sortOrder = sortOrder;
    }

    public String getSortField() {
        return sortField;
    }

    public String getSortOrder() {
        return sortOrder;
    }

}
