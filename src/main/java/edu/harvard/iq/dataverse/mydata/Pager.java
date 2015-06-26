/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import java.io.IOException;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

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
    public int pageCount = 0;
    public int[] pageNumberList = null;

    public int previousPageNumber = 0;
    public int nextPageNumber = 0;
    
    public int startCardNumber = 0;
    public int endCardNumber = 0;
    
    
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
            this.pageCount += 1;
        }
    
        // Sanity check for the selected page
        if (this.selectedPageNumber > this.pageCount){
            this.selectedPageNumber = 1;
        }
    
        // page number list
        pageNumberList = new int[this.pageCount];
        for(int i=0; i<this.pageCount; i++){
            pageNumberList[i] = i + 1;
         }

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
    
    public String asJSONString() throws JSONException{
        JSONObject obj = new JSONObject();
        
        obj.put("isNecessary", this.isPagerNecessary());
        if (!this.isPagerNecessary()){
            return obj.toString();
        }
        
        obj.put("numResults", this.numResults);
        obj.put("docsPerPage", this.docsPerPage);
        obj.put("selectedPageNumber", this.selectedPageNumber);

        obj.put("pageCount", this.pageCount);
        obj.put("pageNumberList", this.pageNumberList);
        
        obj.put("previousPageNumber", this.previousPageNumber);
        obj.put("nextPageNumber", this.nextPageNumber);
        obj.put("startCardNumber", this.startCardNumber);
        obj.put("endCardNumber", this.endCardNumber);
        
        return obj.toString();
             
    }
    
    public static void main(String[] args) throws IOException {
       
        Pager pager = new Pager(100, 10, 1);
               
    }
    
    private void msg(String s){
        System.out.println(s);
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }
} 