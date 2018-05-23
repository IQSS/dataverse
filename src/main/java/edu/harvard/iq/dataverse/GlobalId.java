/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;
import javax.ejb.EJB;
import javax.persistence.Query;

/**
 *
 * @author skraffmiller
 */
public class GlobalId implements java.io.Serializable {

    
    public static final String DOI_PROTOCOL = "doi";
    public static final String HDL_PROTOCOL = "hdl";
    public static final String HDL_RESOLVER_URL = "https://hdl.handle.net/";
    public static final String DOI_RESOLVER_URL = "https://doi.org/";
    
    private static final Logger logger = Logger.getLogger(GlobalId.class.getName());
    
    @EJB
    SettingsServiceBean settingsService;

    public GlobalId(String identifier) {
        
        // set the protocol, authority, and identifier via parsePersistentId        
        if (!this.parsePersistentId(identifier)){
            throw new IllegalArgumentException("Failed to parse identifier: " + identifier);
        }
    }

    public GlobalId(String protocol, String authority, String identifier) {
        this.protocol = protocol;
        this.authority = authority;
        this.identifier = identifier;
    }
    
    public GlobalId(DvObject dvObject)
    {
        this.authority = dvObject.getAuthority();
        this.protocol = dvObject.getProtocol();
        this.identifier = dvObject.getIdentifier(); 
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
        if (protocol == null || authority == null || identifier == null) {
            return "";
        }
        return protocol + ":" + authority + "/" + identifier;
    }
    
    public URL toURL() {
        URL url = null;
        if (identifier == null){
            return null;
        }
        try {
            if (protocol.equals(DOI_PROTOCOL)){
               url = new URL(DOI_RESOLVER_URL + authority + "/" + identifier); 
            } else if (protocol.equals(HDL_PROTOCOL)){
               url = new URL(HDL_RESOLVER_URL + authority + "/" + identifier);  
            }           
        } catch (MalformedURLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }       
        return url;
    }    

    
    /** 
     *   Parse a Persistent Id and set the protocol, authority, and identifier
     * 
     *   Example 1: doi:10.5072/FK2/BYM3IW
     *       protocol: doi
     *       authority: 10.5072/FK2
     *       identifier: BYM3IW
     * 
     *   Example 2: hdl:1902.1/111012
     *       protocol: hdl
     *       authority: 1902.1
     *       identifier: 111012
     *
     * @param identifierString
     * 
     */
    
    private boolean parsePersistentId(String identifierString) {

        if (identifierString == null) {
            return false;
        }
        int index1 = identifierString.indexOf(':');
        if (index1 > 0) { // ':' found with one or more characters before it
            int index2 = identifierString.indexOf('/', index1 + 1);
            if (index2 > 0 && (index2 + 1) < identifierString.length()) { // '/' found with one or more characters
                                                                          // between ':'
                String protocol = identifierString.substring(0, index1); // and '/' and there are characters after '/'
                if (!"doi".equals(protocol) && !"hdl".equals(protocol)) {
                    return false;
                }
                if (this.protocol.equals(DOI_PROTOCOL)) {
                    if (!this.checkDOIAuthority(this.authority)) {
                        return false;
                    }
                }
                // Passed all checks
                this.protocol = protocol;
                //Strip any whitespace, ; and ' from authority and identifier parts (should finding them cause a failure instead?)
                this.authority = formatIdentifierString(identifierString.substring(index1 + 1, index2));
                this.identifier = formatIdentifierString(identifierString.substring(index2 + 1));
            } else {
                logger.info("Error parsing identifier: " + identifierString
                        + ": ':<authority>/<identifier>' not found in string");
                return false;
            }
        } else {
            logger.info("Error parsing identifier: " + identifierString + ": '<protocol>:' not found in string");
            return false;
        }
        return true;
    }
    
    private String formatIdentifierString(String str){
        
        if (str == null){
            return null;
        }
        // remove whitespace, single quotes, and semicolons
        return str.replaceAll("\\s+|'|;","");  
        
        /*
        < 	(%3C)
> 	(%3E)
{ 	(%7B)
} 	(%7D)
^ 	(%5E)
[ 	(%5B)
] 	(%5D)
` 	(%60)
| 	(%7C)
\ 	(%5C)
+
        */
        // http://www.doi.org/doi_handbook/2_Numbering.html
    }
    
    
    private boolean checkDOIAuthority(String doiAuthority){
        
        if (doiAuthority==null){
            return false;
        }
        
        if (!(doiAuthority.startsWith("10."))){
            return false;
        }
        
        return true;
    }
    
    
}
