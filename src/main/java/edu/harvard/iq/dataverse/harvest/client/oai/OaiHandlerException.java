/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.client.oai;

/**
 *
 * @author Leonid Andreev
 */
public class OaiHandlerException extends Exception {
    public OaiHandlerException(String message) {
        super(message);
    }

    public OaiHandlerException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
