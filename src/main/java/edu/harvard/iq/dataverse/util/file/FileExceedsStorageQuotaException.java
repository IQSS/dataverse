/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.util.file;

/**
 *
 * @author landreev
 */
public class FileExceedsStorageQuotaException extends Exception {

    public FileExceedsStorageQuotaException(String message) {
        super(message);
    }

    public FileExceedsStorageQuotaException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
