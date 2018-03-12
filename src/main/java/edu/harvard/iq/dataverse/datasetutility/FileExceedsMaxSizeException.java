/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

/**
 *
 * @author rmp553
 */
public class FileExceedsMaxSizeException extends Exception {

    public FileExceedsMaxSizeException(String message) {
        super(message);
    }

    public FileExceedsMaxSizeException(String message, Throwable cause) {
        super(message, cause);
    }

}
