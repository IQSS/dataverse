/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datafile.file.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class FileReplaceException extends RuntimeException {

    private Reason reason;

    // -------------------- CONSTRUCTORS --------------------

    public FileReplaceException(Reason reason) {
        super(reason.getMessage());
        this.reason = reason;
    }

    public FileReplaceException(Reason reason, Throwable cause) {
        super(reason.getMessage(), cause);
        this.reason = reason;
    }

    // -------------------- GETTERS --------------------

    public Reason getReason() {
        return reason;
    }

    // -------------------- INNER CLASSES --------------------

    public enum Reason {
        ZIP_NOT_SUPPORTED("Zipped files are not supported!"),
        VIRUS_DETECTED("File contains a virus");

        private String message;

        private Reason(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}

