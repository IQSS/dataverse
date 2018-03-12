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
public class NoFilesException extends Exception {

    public NoFilesException(String message) {
        super(message);
    }

    public NoFilesException(String message, Throwable cause) {
        super(message, cause);
    }

}


