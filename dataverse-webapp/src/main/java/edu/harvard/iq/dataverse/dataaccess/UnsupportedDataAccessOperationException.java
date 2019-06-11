/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.dataaccess;

import java.io.IOException;

/**
 *
 * @author Leonid Andreev
 */
public class UnsupportedDataAccessOperationException extends IOException {
    
    public UnsupportedDataAccessOperationException(String message) {
        super(message);
    }
    
}
