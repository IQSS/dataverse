package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.harvard.iq.dataverse.mydata.Pager;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class PagerDTO {
    @JsonProperty("is_necessary")
    private Boolean isNecessary;

    private Integer numResults;
    private String numResultsString;
    private Integer docsPerPage;
    private Integer selectedPageNumber;
    private Integer pageCount;
    private Boolean hasPreviousPageNumber;
    private Integer previousPageNumber;
    private Boolean hasNextPageNumber;
    private Integer nextPageNumber;

    private Integer startResultNumber;
    private Integer endResultNumber;
    private String startResultNumberString;
    private String endResultNumberString;
    private Integer remainingResults;

    private Integer numberNextResults;
    private List<Integer> pageNumberList;

    // -------------------- GETTERS --------------------

    public Boolean getNecessary() {
        return isNecessary;
    }

    public Integer getNumResults() {
        return numResults;
    }

    public String getNumResultsString() {
        return numResultsString;
    }

    public Integer getDocsPerPage() {
        return docsPerPage;
    }

    public Integer getSelectedPageNumber() {
        return selectedPageNumber;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public Boolean getHasPreviousPageNumber() {
        return hasPreviousPageNumber;
    }

    public Integer getPreviousPageNumber() {
        return previousPageNumber;
    }

    public Boolean getHasNextPageNumber() {
        return hasNextPageNumber;
    }

    public Integer getNextPageNumber() {
        return nextPageNumber;
    }

    public Integer getStartResultNumber() {
        return startResultNumber;
    }

    public Integer getEndResultNumber() {
        return endResultNumber;
    }

    public String getStartResultNumberString() {
        return startResultNumberString;
    }

    public String getEndResultNumberString() {
        return endResultNumberString;
    }

    public Integer getRemainingResults() {
        return remainingResults;
    }

    public Integer getNumberNextResults() {
        return numberNextResults;
    }

    public List<Integer> getPageNumberList() {
        return pageNumberList;
    }

    // -------------------- SETTERS --------------------

    public void setNecessary(Boolean necessary) {
        isNecessary = necessary;
    }

    public void setNumResults(Integer numResults) {
        this.numResults = numResults;
    }

    public void setNumResultsString(String numResultsString) {
        this.numResultsString = numResultsString;
    }

    public void setDocsPerPage(Integer docsPerPage) {
        this.docsPerPage = docsPerPage;
    }

    public void setSelectedPageNumber(Integer selectedPageNumber) {
        this.selectedPageNumber = selectedPageNumber;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public void setHasPreviousPageNumber(Boolean hasPreviousPageNumber) {
        this.hasPreviousPageNumber = hasPreviousPageNumber;
    }

    public void setPreviousPageNumber(Integer previousPageNumber) {
        this.previousPageNumber = previousPageNumber;
    }

    public void setHasNextPageNumber(Boolean hasNextPageNumber) {
        this.hasNextPageNumber = hasNextPageNumber;
    }

    public void setNextPageNumber(Integer nextPageNumber) {
        this.nextPageNumber = nextPageNumber;
    }

    public void setStartResultNumber(Integer startResultNumber) {
        this.startResultNumber = startResultNumber;
    }

    public void setEndResultNumber(Integer endResultNumber) {
        this.endResultNumber = endResultNumber;
    }

    public void setStartResultNumberString(String startResultNumberString) {
        this.startResultNumberString = startResultNumberString;
    }

    public void setEndResultNumberString(String endResultNumberString) {
        this.endResultNumberString = endResultNumberString;
    }

    public void setRemainingResults(Integer remainingResults) {
        this.remainingResults = remainingResults;
    }

    public void setNumberNextResults(Integer numberNextResults) {
        this.numberNextResults = numberNextResults;
    }

    public void setPageNumberList(List<Integer> pageNumberList) {
        this.pageNumberList = pageNumberList;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Converter {

        public PagerDTO convert(Pager pager) {
            PagerDTO converted = new PagerDTO();
            converted.setNecessary(pager.isPagerNecessary());
            converted.setNumResults(pager.getNumResults());
            converted.setNumResultsString(pager.addCommasToNumber(pager.getNumResults()));
            converted.setDocsPerPage(pager.getDocsPerPage());
            converted.setSelectedPageNumber(pager.getSelectedPageNumber());
            converted.setPageCount(pager.getPageCount());
            converted.setHasPreviousPageNumber(pager.hasPreviousPageNumber());
            converted.setPreviousPageNumber(pager.getPreviousPageNumber());
            converted.setHasNextPageNumber(pager.hasNextPageNumber());
            converted.setNextPageNumber(pager.getNextPageNumber());

            converted.setStartResultNumber(pager.getStartCardNumber());
            converted.setEndResultNumber(pager.getEndCardNumber());
            converted.setStartResultNumberString(pager.addCommasToNumber(pager.getStartCardNumber()));
            converted.setEndResultNumberString(pager.addCommasToNumber(pager.getEndCardNumber()));
            converted.setRemainingResults(pager.getRemainingCards());

            converted.setNumberNextResults(pager.getNumberNextResults());
            converted.setPageNumberList(Arrays.stream(pager.getPageNumberList())
                    .boxed()
                    .collect(Collectors.toList()));
            return converted;
        }
    }
}
