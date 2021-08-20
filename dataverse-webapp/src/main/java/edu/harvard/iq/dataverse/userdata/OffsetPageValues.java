/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.userdata;

/**
 * @author rmp553
 */
public class OffsetPageValues {

    private final int offset;
    private final int pageNumber;


    // -------------------- CONSTRUCTORS --------------------

    public OffsetPageValues(int offset, int pageNumber) {
        this.pageNumber = pageNumber;
        this.offset = offset;
    }

    // -------------------- GETTERS --------------------

    /**
     * Get for offset
     *
     * @return int
     */
    public int getOffset() {
        return this.offset;
    }

    /**
     * Get for pageNumber
     *
     * @return int
     */
    public int getPageNumber() {
        return this.pageNumber;
    }

}
