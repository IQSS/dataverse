/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datafile.file.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class FileReplaceException extends RuntimeException {

    public FileReplaceException(String message) {
        super(message);
    }

    public FileReplaceException(String message, Throwable cause) {
        super(message, cause);
    }

}

