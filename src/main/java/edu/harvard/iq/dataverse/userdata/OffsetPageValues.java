/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.userdata;

/**
 *
 * @author rmp553
 */
public class OffsetPageValues {
 
    private int offset;
    private int pageNumber;


     /*
     * Constructor
     */
    public OffsetPageValues(int offset, int pageNumber) {
        this.pageNumber = pageNumber;
        this.offset = offset;
    }

    
 
    /**
     *  Set offset
     *  @param offset
     */
    public void setOffset(int offset){
        this.offset = offset;
    }

    /**
     *  Get for offset
     *  @return int
     */
    public int getOffset(){
        return this.offset;
    }
    

    /**
     *  Set pageNumber
     *  @param pageNumber
     */
    public void setPageNumber(int pageNumber){
        this.pageNumber = pageNumber;
    }

    /**
     *  Get for pageNumber
     *  @return int
     */
    public int getPageNumber(){
        return this.pageNumber;
    }
      
}
