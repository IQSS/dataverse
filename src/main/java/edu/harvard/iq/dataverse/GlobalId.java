/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;

/**
 *
 * @author skraffmiller
 */
public class GlobalId implements java.io.Serializable {

    public GlobalId(String identifier) {

        int index1 = identifier.indexOf(':');
        int index2 = identifier.indexOf('/');
        if (index1 == -1) {
            throw new IllegalArgumentException("Error parsing identifier: " + identifier + ". ':' not found in string");
        } else {
            protocol = identifier.substring(0, index1);
        }
        if (index2 == -1) {
            throw new IllegalArgumentException("Error parsing identifier: " + identifier + ". '/' not found in string");

        } else {
            authority = identifier.substring(index1 + 1, index2);
        }
        identifier = identifier.substring(index2 + 1).toUpperCase();

    }

    public GlobalId(String protocol, String authority, String identifier) {
        this.protocol = protocol;
        this.authority = authority;
        this.identifier = identifier;
    }

    private String protocol;
    private String authority;
    private String identifier;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String toString() {
        return protocol + ":" + authority + "/" + identifier;
    }
    
    public URL toURL() {
        URL url = null;
        try {
            if (protocol.equals("doi")){
               url = new URL("http://dx.doi.org/" + authority + "/" + identifier); 
            } else {
               url = new URL("http://hdl.handle.net/" + authority + "/" + identifier);  
            }           
        } catch (MalformedURLException ex) {
            Logger.getLogger(GlobalId.class.getName()).log(Level.SEVERE, null, ex);
        }       
        return url;
    }    

}
