/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 *
 * @author rmp553
 */
public class Pager {
    
    /* inputs */
    public int numResults;
    public int docsPerPage = 10;
    public int selectedPageNumber = 1;

    /* calculated */
    public int pageCount;
    public int[] pageNumberList;

    public int previousPageNumber;
    public int nextPageNumber;
    
    public int startCardNumber;
    public int endCardNumber;
    
    
    public Pager(int numResults, int docsPerPage, int selectedPageNumber) {
        
        if (numResults < 0){
            throw new IllegalArgumentException("numResults must be 0 or higher");
        }
        if (docsPerPage < 1){
            throw new IllegalArgumentException("docsPerPage must be 1 or higher");
        }
        if (selectedPageNumber < 1){
            throw new IllegalArgumentException("selectedPageNumber must be 1 or higher");
        }      
        numResults = numResults;
        docsPerPage = docsPerPage;
        selectedPageNumber = selectedPageNumber;
        makePageStats();
    }

    private void makePageStats(){
       // page count
        this.pageCount = numResults / docsPerPage;
        if ((this.numResults % this.docsPerPage) > 0){
            this.pageCount += 1;
        }
    
        // Sanity check for the selected page
        if (this.selectedPageNumber > this.pageCount){
            this.selectedPageNumber = 1;
        }
    
        // page number list
        pageNumberList = new int[this.pageCount];
        for(int i=1; i<this.pageCount; i++){
            pageNumberList[i] = i + 1;
         }

        // prev/next page numbers
        this.previousPageNumber =  max(this.selectedPageNumber-1, 1); // must be at least 1
        this.nextPageNumber =  min(this.selectedPageNumber+1, this.pageCount); // must be at least 1

        // start/end card numbers
        this.startCardNumber =  (this.docsPerPage * (this.selectedPageNumber - 1)) + 1;
        this.endCardNumber = min(this.startCardNumber + (this.docsPerPage-1), this.numResults );


    }
    
    
    /**
     * get numResults
     */
    public int getNumResults(){
        return this.numResults;
    }
    

    /**
     * set numResults
     */
    public void setNumResults(int numResults){
        this.numResults = numResults;
    }
    

    /**
     * get docsPerPage
     */
    public int getDocsPerPage(){
        return this.docsPerPage;
    }
    

    /**
     * set docsPerPage
     */
    public void setDocsPerPage(int docsPerPage){
        this.docsPerPage = docsPerPage;
    }
    

    /**
     * get selectedPageNumber
     */
    public int getSelectedPageNumber(){
        return this.selectedPageNumber;
    }
    

    /**
     * set selectedPageNumber
     */
    public void setSelectedPageNumber(int selectedPageNumber){
        this.selectedPageNumber = selectedPageNumber;
    }
    

    /**
     * get pageCount
     */
    public int getPageCount(){
        return this.pageCount;
    }
    

    /**
     * set pageCount
     */
    public void setPageCount(int pageCount){
        this.pageCount = pageCount;
    }
    

    /**
     * get pageNumberList
     */
    public int[] getPageNumberList(){
        return this.pageNumberList;
    }
    

    /**
     * set pageNumberList
     */
    public void setPageNumberList(int[] pageNumberList){
        this.pageNumberList = pageNumberList;
    }
    

    /**
     * get previousPageNumber
     */
    public int getPreviousPageNumber(){
        return this.previousPageNumber;
    }
    

    /**
     * set previousPageNumber
     */
    public void setPreviousPageNumber(int previousPageNumber){
        this.previousPageNumber = previousPageNumber;
    }
    

    /**
     * get nextPageNumber
     */
    public int getNextPageNumber(){
        return this.nextPageNumber;
    }
    

    /**
     * set nextPageNumber
     */
    public void setNextPageNumber(int nextPageNumber){
        this.nextPageNumber = nextPageNumber;
    }
    

    /**
     * get startCardNumber
     */
    public int getStartCardNumber(){
        return this.startCardNumber;
    }
    

    /**
     * set startCardNumber
     */
    public void setStartCardNumber(int startCardNumber){
        this.startCardNumber = startCardNumber;
    }
    

    /**
     * get endCardNumber
     */
    public int getEndCardNumber(){
        return this.endCardNumber;
    }
    

    /**
     * set endCardNumber
     */
    public void setEndCardNumber(int endCardNumber){
        this.endCardNumber = endCardNumber;
    }

    
} 