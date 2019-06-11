/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server;

/**
 *
 * @author Leonid Andreev
 */
public class OaiSetException extends Exception {
    public OaiSetException(String message) {
        super(message);
    }

    public OaiSetException(String message, Throwable cause) {
        super(message, cause);
    }
}
