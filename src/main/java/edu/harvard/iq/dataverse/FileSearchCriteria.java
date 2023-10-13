package edu.harvard.iq.dataverse;

public class FileSearchCriteria {

    private final String contentType;
    private final FileAccessStatus accessStatus;
    private final String categoryName;
    private final String tabularTagName;
    private final String searchText;

    /**
     * Status of the particular DataFile based on active embargoes and restriction state
     */
    public enum FileAccessStatus {
        Public, Restricted, EmbargoedThenRestricted, EmbargoedThenPublic
    }

    public FileSearchCriteria(String contentType, FileAccessStatus accessStatus, String categoryName, String tabularTagName, String searchText) {
        this.contentType = contentType;
        this.accessStatus = accessStatus;
        this.categoryName = categoryName;
        this.tabularTagName = tabularTagName;
        this.searchText = searchText;
    }

    public String getContentType() {
        return contentType;
    }

    public FileAccessStatus getAccessStatus() {
        return accessStatus;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getTabularTagName() {
        return tabularTagName;
    }

    public String getSearchText() {
        return searchText;
    }
}
