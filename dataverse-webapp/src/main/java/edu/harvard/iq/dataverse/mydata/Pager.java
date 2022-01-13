package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.search.SearchConstants;

import java.text.NumberFormat;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * @author rmp553
 */
public class Pager {

    public final int NUM_VISIBLE_PAGES_BUTTONS = 5;

    // inputs
    public int numResults;
    public int docsPerPage = SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE;
    public int selectedPageNumber = 1;

    // calculated
    public int pageCount = 0;
    public int[] pageNumberList = null;

    public int previousPageNumber = 0;
    public int nextPageNumber = 0;

    public int startCardNumber = 0;
    public int endCardNumber = 0;

    public int remainingCards = 0;
    public int numberNextResults = 0;

    // -------------------- CONSTRUCTORS --------------------

    public Pager(int numResults, int docsPerPage, int selectedPageNumber) {

        if (numResults < 0) {
            throw new IllegalArgumentException("numResults must be 0 or higher");
        }
        if (docsPerPage < 1) {
            throw new IllegalArgumentException("docsPerPage must be 1 or higher");
        }
        if (selectedPageNumber < 1) {
            throw new IllegalArgumentException("selectedPageNumber must be 1 or higher");
        }
        this.numResults = numResults;
        this.docsPerPage = docsPerPage;
        this.selectedPageNumber = selectedPageNumber;
        makePageStats();
    }

    // -------------------- GETTERS --------------------

    public int getDocsPerPage() {
        return docsPerPage;
    }

    public int getEndCardNumber() {
        return endCardNumber;
    }

    public int getNextPageNumber() {
        return nextPageNumber;
    }

    public int getNumberNextResults() {
        return numberNextResults;
    }

    public int getNumResults() {
        return numResults;
    }

    public int getPageCount() {
        return pageCount;
    }

    public int[] getPageNumberList() {
        return pageNumberList;
    }

    public int getPreviousPageNumber() {
        return previousPageNumber;
    }

    public int getRemainingCards() {
        return remainingCards;
    }

    public int getSelectedPageNumber() {
        return selectedPageNumber;
    }

    public int getStartCardNumber() {
        return startCardNumber;
    }

    // -------------------- LOGIC --------------------

    public boolean isPagerNecessary() {
        return pageCount > 1;
    }

    public boolean hasPreviousPageNumber() {
        return selectedPageNumber > 1;
    }

    public boolean hasNextPageNumber() {
        return pageCount > 1 && selectedPageNumber < pageCount;
    }

    public String addCommasToNumber(int count) {
        return NumberFormat.getInstance().format(count);
    }

    // -------------------- PRIVATE --------------------

    private void makePageStats() {

        if (numResults == 0) {
            selectedPageNumber = 0;
            return;
        }

        // page count
        pageCount = numResults / docsPerPage;
        if ((numResults % docsPerPage) > 0) {
            pageCount++;
        }

        // Sanity check for the selected page
        if (selectedPageNumber > pageCount) {
            selectedPageNumber = 1;
        }

        makePageNumberList();

        // prev/next page numbers
        previousPageNumber = max(selectedPageNumber - 1, 1); // must be at least 1
        nextPageNumber = min(selectedPageNumber + 1, pageCount); // must be at least 1
        nextPageNumber = max(nextPageNumber, 1);

        // start/end card numbers
        startCardNumber = (docsPerPage * (selectedPageNumber - 1)) + 1;
        endCardNumber = numResults == 0
                ? 0 : min(startCardNumber + (docsPerPage - 1), numResults);

        remainingCards = numResults - endCardNumber;
        remainingCards = max(remainingCards, 0);

        if (remainingCards > 0) {
            numberNextResults = Math.min(remainingCards, docsPerPage);
        }
    }

    private void makePageNumberList() {
        if (numResults < 1) {
            return;
        }

        // In this case, there are 1 to 5 pages
        if (pageCount <= NUM_VISIBLE_PAGES_BUTTONS || selectedPageNumber <= 3) {

            int numButtons = min(pageCount, NUM_VISIBLE_PAGES_BUTTONS);
            pageNumberList = new int[numButtons];

            for (int i = 0; i < numButtons; i++) {
                pageNumberList[i] = i + 1;
            }
            return;
        }

        // In this case, there are more than 5 pages
        // Example:  page 7 of 8
        int defaultButtonsToRight = 2;
        pageNumberList = new int[NUM_VISIBLE_PAGES_BUTTONS];

        // 8 - 7 = 1
        int buttonsToRight = pageCount - selectedPageNumber;
        if (buttonsToRight < 0) {
            throw new IllegalStateException("Page count cannot be less than the selected page");
        }
        int startPage = buttonsToRight >= defaultButtonsToRight
                ? selectedPageNumber - defaultButtonsToRight
                : selectedPageNumber - (defaultButtonsToRight - buttonsToRight) - defaultButtonsToRight; // 7 -2 -1 = 4 - start on page 4

        for (int i = 0; i < NUM_VISIBLE_PAGES_BUTTONS; i++) {
            pageNumberList[i] = i + startPage;
        }
    }

    // -------------------- SETTERS --------------------

    public void setDocsPerPage(int docsPerPage) {
        this.docsPerPage = docsPerPage;
    }

    public void setEndCardNumber(int endCardNumber) {
        this.endCardNumber = endCardNumber;
    }

    public void setNextPageNumber(int nextPageNumber) {
        this.nextPageNumber = nextPageNumber;
    }

    public void setNumResults(int numResults) {
        this.numResults = numResults;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public void setPageNumberList(int[] pageNumberList) {
        this.pageNumberList = pageNumberList;
    }

    public void setPreviousPageNumber(int previousPageNumber) {
        this.previousPageNumber = previousPageNumber;
    }

    public void setRemainingCards(int remainingCards) {
        this.remainingCards = remainingCards;
    }

    public void setSelectedPageNumber(int selectedPageNumber) {
        this.selectedPageNumber = selectedPageNumber;
    }

    public void setStartCardNumber(int startCardNumber) {
        this.startCardNumber = startCardNumber;
    }
}