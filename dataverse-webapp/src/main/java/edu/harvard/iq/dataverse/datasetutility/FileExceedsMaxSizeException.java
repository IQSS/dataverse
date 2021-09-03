/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import javax.ejb.ApplicationException;

/**
 * @author rmp553
 */
@ApplicationException(rollback = true)
public class FileExceedsMaxSizeException extends RuntimeException {

    private long maxFileSize;

    public FileExceedsMaxSizeException(long maxFileSize, String message) {
        super(message);
        this.maxFileSize = maxFileSize;
    }

    public FileExceedsMaxSizeException(long maxFileSize, String message, Throwable cause) {
        super(message, cause);
        this.maxFileSize = maxFileSize;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

}
