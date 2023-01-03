/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import static edu.harvard.iq.dataverse.util.StringUtil.isEmpty;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;

/**
 *
 * @author skraffmiller
 */
public class GlobalId implements java.io.Serializable {
    
    public static final String DOI_PROTOCOL = "doi";
    public static final String HDL_PROTOCOL = "hdl";
    public static final String DOI_RESOLVER_URL = "https://doi.org/";
    public static final String DXDOI_RESOLVER_URL = "https://dx.doi.org/";
    public static final String HDL_RESOLVER_URL = "https://hdl.handle.net/";
    public static final String HTTP_DOI_RESOLVER_URL = "http://doi.org/";
    public static final String HTTP_DXDOI_RESOLVER_URL = "http://dx.doi.org/";
    public static final String HTTP_HDL_RESOLVER_URL = "http://hdl.handle.net/";

    public static Optional<GlobalId> parse(String identifierString) {
        try {
            return Optional.of(new GlobalId(identifierString));
        } catch ( IllegalArgumentException _iae) {
            return Optional.empty();
        }
    }
    
    private static final Logger logger = Logger.getLogger(GlobalId.class.getName());
    
    @EJB
    SettingsServiceBean settingsService;

    /**
     * 
     * @param identifier The string to be parsed
     * @throws IllegalArgumentException if the passed string cannot be parsed.
     */
    public GlobalId(String identifier) {
        // set the protocol, authority, and identifier via parsePersistentId        
        if ( ! parsePersistentId(identifier) ){
            throw new IllegalArgumentException("Failed to parse identifier: " + identifier);
        }
    }

    public GlobalId(String protocol, String authority, String identifier) {
        this.protocol = protocol;
        this.authority = authority;
        this.identifier = identifier;
    }
    
    public GlobalId(DvObject dvObject) {
        this.authority = dvObject.getAuthority();
        this.protocol = dvObject.getProtocol();
        this.identifier = dvObject.getIdentifier(); 
    }
        
    private String protocol;
    private String authority;
    private String identifier;

    /**
     * Tests whether {@code this} instance has all the data required for a 
     * global id.
     * @return {@code true} iff all the fields are non-empty; {@code false} otherwise.
     */
    public boolean isComplete() {
        return !(isEmpty(protocol)||isEmpty(authority)||isEmpty(identifier));
    }
    
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
        return asString();
    }
    
    /**
     * Returns {@code this}' string representation. Differs from {@link #toString}
     * which can also contain debug data, if needed.
     * 
     * @return The string representation of this global id.
     */
    public String asString() {
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
     *       authority: 10.5072
     *       identifier: FK2/BYM3IW
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
    private boolean parsePersistentId(String identifierString) {

        if (identifierString == null) {
            return false;
        }
        int index1 = identifierString.indexOf(':');
        if (index1 > 0) { // ':' found with one or more characters before it
            int index2 = identifierString.indexOf('/', index1 + 1);
            if (index2 > 0 && (index2 + 1) < identifierString.length()) { // '/' found with one or more characters
                                                                          // between ':'
                protocol = identifierString.substring(0, index1); // and '/' and there are characters after '/'
                if (!"doi".equals(protocol) && !"hdl".equals(protocol)) {
                    return false;
                }
                //Strip any whitespace, ; and ' from authority (should finding them cause a failure instead?)
                authority = formatIdentifierString(identifierString.substring(index1 + 1, index2));
                if(testforNullTerminator(authority)) return false;
                if (protocol.equals(DOI_PROTOCOL)) {
                    if (!this.checkDOIAuthority(authority)) {
                        return false;
                    }
                }
                // Passed all checks
                //Strip any whitespace, ; and ' from identifier (should finding them cause a failure instead?)
                identifier = formatIdentifierString(identifierString.substring(index2 + 1));
                if(testforNullTerminator(identifier)) return false;               
            } else {
                logger.log(Level.INFO, "Error parsing identifier: {0}: '':<authority>/<identifier>'' not found in string", identifierString);
                return false;
            }
        } else {
            logger.log(Level.INFO, "Error parsing identifier: {0}: ''<protocol>:'' not found in string", identifierString);
            return false;
        }
        return true;
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
    
    private static boolean testforNullTerminator(String str){
        if(str == null) {
            return false;
        }
        return str.indexOf('\u0000') > 0;
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

    /**
     * Verifies that the pid only contains allowed characters.
     *
     * @param pidParam
     * @return true if pid only contains allowed characters false if pid
     * contains characters not specified in the allowed characters regex.
     */
    public static boolean verifyImportCharacters(String pidParam) {

        Pattern p = Pattern.compile(BundleUtil.getStringFromBundle("pid.allowedCharacters"));
        Matcher m = p.matcher(pidParam);

        return m.matches();
    }

    /**
     * Convenience method to get the internal form of a PID string when it may be in
     * the https:// or http:// form ToDo -refactor class to allow creating a
     * GlobalID from any form (which assures it has valid syntax) and then have methods to get
     * the form you want.
     * 
     * @param pidUrlString - a string assumed to be a valid PID in some form
     * @return the internal form as a String
     */
    public static String getInternalFormOfPID(String pidUrlString) {
        String pidString = pidUrlString;
        if(pidUrlString.startsWith(GlobalId.DOI_RESOLVER_URL)) {
            pidString = pidUrlString.replace(GlobalId.DOI_RESOLVER_URL, (GlobalId.DOI_PROTOCOL + ":"));
        } else if(pidUrlString.startsWith(GlobalId.HDL_RESOLVER_URL)) {
            pidString = pidUrlString.replace(GlobalId.HDL_RESOLVER_URL, (GlobalId.HDL_PROTOCOL + ":"));
        } else if(pidUrlString.startsWith(GlobalId.HTTP_DOI_RESOLVER_URL)) {
            pidString = pidUrlString.replace(GlobalId.HTTP_DOI_RESOLVER_URL, (GlobalId.DOI_PROTOCOL + ":"));
        } else if(pidUrlString.startsWith(GlobalId.HTTP_HDL_RESOLVER_URL)) {
            pidString = pidUrlString.replace(GlobalId.HTTP_HDL_RESOLVER_URL, (GlobalId.HDL_PROTOCOL + ":"));
        }
        return pidString;
    }
}
