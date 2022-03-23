/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.search.SearchConstants;
import java.io.IOException;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;

/**
 *
 * @author rmp553
 */
public class Pager {
    
    public final int NUM_VISIBLE_PAGES_BUTTONS = 5;
    public int PAGE_BUTTONS_TO_SHOW = 5;
    
    /* inputs */
    public int numResults;
    public String numResultsString;
    public int docsPerPage = SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE;
    public int selectedPageNumber = 1;

    /* calculated */
    public int pageCount = 0;
    public int[] pageNumberList = null;

    public int previousPageNumber = 0;
    public int nextPageNumber = 0;
    
    public int startCardNumber = 0;
    public int endCardNumber = 0;
    
    public String startCardNumberString;
    public String endCardNumberString;

    public int remainingCards = 0;
    public int numberNextResults =0;
    
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
        this.numResults = numResults;
        this.docsPerPage = docsPerPage;
        this.selectedPageNumber = selectedPageNumber;
        makePageStats();
    }

    private void makePageStats(){
        
        if (numResults == 0){
            this.selectedPageNumber = 0;
            return;
        }
        
       // page count
        this.pageCount = numResults / docsPerPage;
        if ((this.numResults % this.docsPerPage) > 0){
            this.pageCount++;
        }
    
        // Sanity check for the selected page
        if (this.selectedPageNumber > this.pageCount){
            this.selectedPageNumber = 1;
        }
    
        // page number list
        /*this.pageNumberList = new int[pageCount];
            for(int i=0; i<this.pageCount; i++){
               this.pageNumberList[i] = i + 1;
        }*/
        makePageNumberList();

        // prev/next page numbers
        this.previousPageNumber =  max(this.selectedPageNumber-1, 1); // must be at least 1
        this.nextPageNumber =  min(this.selectedPageNumber+1, this.pageCount); // must be at least 1
        this.nextPageNumber = max(this.nextPageNumber, 1);
        
        // start/end card numbers
        this.startCardNumber =  (this.docsPerPage * (this.selectedPageNumber - 1)) + 1;
        if (this.numResults == 0){
            this.endCardNumber = 0;
        }else{
            this.endCardNumber = min(this.startCardNumber + (this.docsPerPage-1), this.numResults );
        }
        
        this.remainingCards = this.numResults - this.endCardNumber;
        this.remainingCards = max(this.remainingCards, 0);
        
        if (this.remainingCards > 0){
            if (this.remainingCards < this.docsPerPage){
                this.numberNextResults = this.remainingCards;
            }else{
                this.numberNextResults = this.docsPerPage;
            }
        }        
    }
    
    
    public boolean isPagerNecessary(){
        
        if (this.pageCount > 1){
            return true;
        }
        return false;
    }
    
    public boolean hasPreviousPageNumber(){
        
        return this.selectedPageNumber > 1;
    }
    
    public boolean hasNextPageNumber(){
        if (this.pageCount > 1){
            if (selectedPageNumber < this.pageCount){
                return true;
            }
        }
        return false;
    }
    
    
    /**
     * get numResults
     * @return 
     */
    public int getNumResults(){
        return this.numResults;
    }
    

    /**
     * @param numResults
     */
    public void setNumResults(int numResults){
        this.numResults = numResults;
    }
    

    /**
     * get docsPerPage
     * @return 
     */
    public int getDocsPerPage(){
        return this.docsPerPage;
    }
    

    /**
     * @param docsPerPage
     */
    public void setDocsPerPage(int docsPerPage){
        this.docsPerPage = docsPerPage;
    }
    

    /**
     * get selectedPageNumber
     * @return 
     */
    public int getSelectedPageNumber(){
        return this.selectedPageNumber;
    }
    

    /**
     * @param selectedPageNumber
     */
    public void setSelectedPageNumber(int selectedPageNumber){
        this.selectedPageNumber = selectedPageNumber;
    }
    

    /**
     * get pageCount
     * @return 
     */
    public int getPageCount(){
        return this.pageCount;
    }
    

    /**
     * @param pageCount
     */
    public void setPageCount(int pageCount){
        this.pageCount = pageCount;
    }
    
    /**
     * get getPageNumberListAsStringList
     * @return 
     */
    public List<String> getPageNumberListAsStringList(){
        List<String> newList = new ArrayList<String>(pageNumberList.length);
        for (int pgNum : pageNumberList) { 
          newList.add(String.valueOf(pgNum)); 
        }
        return newList;
    }

    /**
     * get pageNumberList
     * @return 
     */
    public int[] getPageNumberList(){
        return this.pageNumberList;
    }
    
    public Integer[] getPageListAsIntegerList(){

        if (pageNumberList == null){
            return null;
        }
        
        // source: https://stackoverflow.com/questions/880581/how-to-convert-int-to-integer-in-java            
        return Arrays.stream(pageNumberList).boxed().toArray( Integer[]::new );
        

    }

    /**
     * @param pageNumberList
     */
    public void setPageNumberList(int[] pageNumberList){
        this.pageNumberList = pageNumberList;
    }
    

    /**
     * get previousPageNumber
     * @return 
     */
    public int getPreviousPageNumber(){
        return this.previousPageNumber;
    }
    

    /**
     * @param previousPageNumber
     */
    public void setPreviousPageNumber(int previousPageNumber){
        this.previousPageNumber = previousPageNumber;
    }
    

    /**
     * get nextPageNumber
     * @return 
     */
    public int getNextPageNumber(){
        return this.nextPageNumber;
    }
    

    /**
     * @param nextPageNumber
     */
    public void setNextPageNumber(int nextPageNumber){
        this.nextPageNumber = nextPageNumber;
    }
    

    /**
     * get startCardNumber
     * @return 
     */
    public int getStartCardNumber(){
        return this.startCardNumber;
    }
    
    public String getStartCardNumberString(){
        
        return this.addCommasToNumber(startCardNumber);

    }
    /**
     * @param startCardNumber
     */
    public void setStartCardNumber(int startCardNumber){
        this.startCardNumber = startCardNumber;
    }
    

    /**
     * get endCardNumber
     * @return 
     */
    public int getEndCardNumber(){
        return this.endCardNumber;
    }
    
    public String getEndCardNumberString(){
        
        return this.addCommasToNumber(endCardNumber);

    }
    /**
     * @param endCardNumber
     */
    public void setEndCardNumber(int endCardNumber){
        this.endCardNumber = endCardNumber;
    }

    public void showClasspaths(){
        ClassLoader cl = ClassLoader.getSystemClassLoader();
 
        URL[] urls = ((URLClassLoader)cl).getURLs();
 
        for(URL url: urls){
        	System.out.println(url.getFile());
        }
    }
    
    public String asJSONString(){
        return this.asJsonObjectBuilder().build().toString();
    }
    
    
    public String addCommasToNumber(int count){
        
        return NumberFormat.getInstance().format(count);
    }
    
    
    /** 
     * Originally used for mydata. 
     * 
     * Variables are named using the idea of cards--as in Dataverse cards,
     * Dataset cards, etc. on the homepage
     * 
     * @return 
     */
    public JsonObjectBuilder asJsonObjectBuilderUsingCardTerms(){
    
        return asJsonObjectBuilderCore(true);
    }

    /** 
     * 
     * Variables are named using the idea of number of results
     * 
     * @return 
     */
    public JsonObjectBuilder asJsonObjectBuilder(){
    
        return asJsonObjectBuilderCore(false);
    }

    
    private JsonObjectBuilder asJsonObjectBuilderCore(boolean useCardTerms){
        
        JsonObjectBuilder jsonPageInfo = Json.createObjectBuilder();
                
       
        jsonPageInfo.add("isNecessary", this.isPagerNecessary())
                    .add("numResults", this.numResults)
                    .add("numResultsString", this.addCommasToNumber(numResults))
                    .add("docsPerPage", this.docsPerPage)
                    .add("selectedPageNumber", this.selectedPageNumber)
                    .add("pageCount", this.pageCount)
                    .add("hasPreviousPageNumber", this.hasPreviousPageNumber())
                    .add("previousPageNumber", this.previousPageNumber)
                    .add("hasNextPageNumber", this.hasNextPageNumber())
                    .add("nextPageNumber", this.nextPageNumber);
        
        if (useCardTerms){
            jsonPageInfo.add("startCardNumber", this.startCardNumber)
                    .add("endCardNumber", this.endCardNumber)
                    .add("startCardNumberString", this.addCommasToNumber(this.startCardNumber))
                    .add("endCardNumberString", this.addCommasToNumber(this.endCardNumber))
                    .add("remainingCards", this.remainingCards);
        }else{
            jsonPageInfo.add("startResultNumber", this.startCardNumber)
                    .add("endResultNumber", this.endCardNumber)
                    .add("startResultNumberString", this.addCommasToNumber(this.startCardNumber))
                    .add("endResultNumberString", this.addCommasToNumber(this.endCardNumber))
                    .add("remainingResults", this.remainingCards);
            
        }
        
        jsonPageInfo.add("numberNextResults", this.numberNextResults);
        
        // --------------------
        // pageNumberList
        // --------------------
        JsonArrayBuilder jsonPageNumberArrayBuilder = Json.createArrayBuilder();
        if (this.pageNumberList != null) {
            for (int pg : this.pageNumberList) {
                jsonPageNumberArrayBuilder.add(pg);
            }
            jsonPageInfo.add("pageNumberList", jsonPageNumberArrayBuilder);
        }

        // --------------------
   
        return jsonPageInfo;
             
    }
    
    private void makePageNumberList(){
        if (this.numResults <  1){
            return;
        }
                        
        // In this case, there are 1 to 5 pages
        //
        if ((this.pageCount <= NUM_VISIBLE_PAGES_BUTTONS)||(this.selectedPageNumber <= 3)){
            
            int numButtons = min(this.pageCount, NUM_VISIBLE_PAGES_BUTTONS);
            this.pageNumberList = new int[numButtons];
        
            for(int i=0; i < numButtons; i++){
                this.pageNumberList[i] = i + 1;
            }
            return;
        }
        
        // In this case, there are more than 5 pages
        //        
        // Example:  page 7 of 8
        //
        int defaultButtonsToRight = 2;
        this.pageNumberList = new int[NUM_VISIBLE_PAGES_BUTTONS];

        // 8 - 7 = 1
        int buttonsToRight = this.pageCount - this.selectedPageNumber;
        if (buttonsToRight < 0){
            throw new IllegalStateException("Page count cannot be less than the selected page");
        }
        int startPage;
        if (buttonsToRight >= defaultButtonsToRight){
            startPage = this.selectedPageNumber - defaultButtonsToRight;
        }else{
            // 7 -2 -1 = 4 - start on page 4
            startPage = this.selectedPageNumber - (defaultButtonsToRight-buttonsToRight) - defaultButtonsToRight;           
        }
        for(int i=0; i< NUM_VISIBLE_PAGES_BUTTONS; i++){
            this.pageNumberList[i] = i + startPage;
        }        
    }
    
    public String getNumResultsString(){
        
        return this.addCommasToNumber(numResults);

    }
    
    public static void main(String[] args) throws IOException {
       
        Pager pager = new Pager(100, 10, 1);
               
    }
    
    private void msg(String s){
        //System.out.println(s);
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }
} 