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
 
    private Integer offset;
    private Integer pageNumber;


     /*
     * Constructor
     */
    public OffsetPageValues(Integer offset, Integer pageNumber) {
        this.pageNumber = pageNumber;
        this.offset = offset;
    }

    
 
    /**
     *  Set offset
     *  @param offset
     */
    public void setOffset(Integer offset){
        this.offset = offset;
    }

    /**
     *  Get for offset
     *  @return Integer
     */
    public Integer getOffset(){
        return this.offset;
    }
    

    /**
     *  Set pageNumber
     *  @param pageNumber
     */
    public void setPageNumber(Integer pageNumber){
        this.pageNumber = pageNumber;
    }

    /**
     *  Get for pageNumber
     *  @return Integer
     */
    public Integer getPageNumber(){
        return this.pageNumber;
    }
      
}
