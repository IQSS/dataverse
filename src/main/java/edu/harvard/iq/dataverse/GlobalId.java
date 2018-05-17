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
import java.util.Optional;
import javax.ejb.EJB;

/**
 *
 * @author skraffmiller
 */
public class GlobalId implements java.io.Serializable {

    
    public static final String DOI_PROTOCOL = "doi";
    public static final String HDL_PROTOCOL = "hdl";
    public static final String HDL_RESOLVER_URL = "https://hdl.handle.net/";
    public static final String DOI_RESOLVER_URL = "https://doi.org/";
    
    @EJB
    SettingsServiceBean settingsService;
    
    public static Optional<GlobalId> parse(String identifier, String doiSeparator) {
        return Optional.ofNullable(parsePersistentId(identifier, doiSeparator, new GlobalId(null, null, null)));
    }
    
    /**
     * 
     * @param identifier
     * @deprecated use {@link #parse}. This method assumes that the DOI separator is "/", instead of reading it from the database.
     */
    @Deprecated
    public GlobalId(String identifier) {
        // set the protocol, authority, and identifier via parsePersistentId        
        if (parsePersistentId(identifier, "/", this) == null ){
            throw new IllegalArgumentException("Failed to parse identifier: " + identifier);
        }
    }

    public GlobalId(String protocol, String authority, String identifier) {
        this.protocol = protocol;
        this.authority = authority;
        this.identifier = identifier;
    }

    public GlobalId(Dataset dataset){
        this.authority = dataset.getAuthority();
        this.protocol = dataset.getProtocol();
        this.identifier = dataset.getIdentifier();
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
            if (protocol.equals(DOI_PROTOCOL)){
               url = new URL(DOI_RESOLVER_URL + authority + "/" + identifier); 
            } else if (protocol.equals(HDL_PROTOCOL)){
               url = new URL(HDL_RESOLVER_URL + authority + "/" + identifier);  
            }           
        } catch (MalformedURLException ex) {
            Logger.getLogger(GlobalId.class.getName()).log(Level.SEVERE, null, ex);
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
     * @param separator the string that separates the authority from the identifier.
     * @param destination the global id that will contain the parsed data.
     * @return {@code destination}, after its fields have been updated, or
     *         {@code null} if parsing failed.
     */
    private static GlobalId parsePersistentId(String identifierString, String separator, GlobalId destination){

        if (identifierString == null){
            return null;
        } 
        
        int index1 = identifierString.indexOf(':');
        if (index1==-1) {
            return null; 
        }  
        int index2 = identifierString.lastIndexOf(separator);
       
        String protocol = identifierString.substring(0, index1);
        
        if (!"doi".equals(protocol) && !"hdl".equals(protocol)) {
            return null;
        }
        
        if (index2 == -1) {
            return null;
        } 
        
        destination.protocol = protocol;
        destination.authority = formatIdentifierString(identifierString.substring(index1+1, index2));
        destination.identifier = formatIdentifierString(identifierString.substring(index2+1));
        
        if (destination.protocol.equals(DOI_PROTOCOL)) {
            if (!destination.checkDOIAuthority(destination.authority)) {
                return null;
            }
        }
        return destination;
    }

    
    private static String formatIdentifierString(String str){
        
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
