package edu.harvard.iq.dataverse.api.util;

public class Pagination {

    private int numResults = -1;
    private int offset;
    private int limitPerPage;

    private static int DEFAULT_PAGE_SIZE = 10;

    public Pagination(Integer limitPerPage, Integer offset) {
        this.offset = offset != null ? Math.max(offset, 0) : 0;
        this.limitPerPage = limitPerPage != null ? Math.max(limitPerPage, 1) : DEFAULT_PAGE_SIZE;
    }

    public void setNumResults(int numResults) {
        this.numResults = numResults;
    }

    public int getNumResults() {
        return numResults;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimitPerPage() {
        return limitPerPage;
    }
}
