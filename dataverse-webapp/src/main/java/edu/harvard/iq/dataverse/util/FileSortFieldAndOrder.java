package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;
import edu.harvard.iq.dataverse.search.SortBy;
import org.apache.commons.lang.StringUtils;

public class FileSortFieldAndOrder {

    private String sortField;
    private SortOrder sortOrder;

    private static String displayOrder = "displayOrder";
    public static String label = "label";
    public static String createDate = "dataFile.createDate";
    public static String size = "dataFile.filesize";
    public static String type = "dataFile.contentType";

    public FileSortFieldAndOrder(String userSuppliedSortField, SortOrder userSuppliedSortOrder) {
        if (StringUtils.isBlank(userSuppliedSortField)) {
            sortField = displayOrder;
        } else if (isUserSuppliedSortField(userSuppliedSortField)) {
            sortField = userSuppliedSortField;
        } else {
            sortField = label;
        }
        
        if (userSuppliedSortOrder == null) {
            sortOrder = SortOrder.asc;
        } else {
            sortOrder = userSuppliedSortOrder;
        }
    }

    public String getSortField() {
        return sortField;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    private boolean isUserSuppliedSortField(String userSuppliedSortField) {
        return userSuppliedSortField.equals(displayOrder) ||
                userSuppliedSortField.equals(label) ||
                userSuppliedSortField.equals(createDate) ||
                userSuppliedSortField.equals(size) ||
                userSuppliedSortField.equals(type);
    }
}
