/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.pidproviders.perma.PermaLinkPidProvider;
import edu.harvard.iq.dataverse.util.BundleUtil;
import static edu.harvard.iq.dataverse.util.StringUtil.isEmpty;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author skraffmiller
 */
public class GlobalId implements java.io.Serializable {
    
    private static final Logger logger = Logger.getLogger(GlobalId.class.getName());

    public GlobalId(String protocol, String authority, String identifier, String separator, String urlPrefix, String providerName) {
        this.protocol = protocol;
        this.authority = authority;
        this.identifier = identifier;
        if(separator!=null) {
          this.separator = separator;
        }
        this.urlPrefix = urlPrefix;
        this.managingProviderId = providerName;
    }
    
    // protocol the identifier system, e.g. "doi"
    // authority the namespace that the authority manages in the identifier system
    // identifier the local identifier part
    private String protocol;
    private String authority;
    private String identifier;
    private String managingProviderId;
    private String separator = "/";
    private String urlPrefix;

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

    public String getAuthority() {
        return authority;
    }

    public String getIdentifier() {
        return identifier;
    }
    
    public String getProviderId() {
        return managingProviderId;
    }

    public String toString() {
        return asString();
    }
    
    /**
     * Concatenate the parts that make up a Global Identifier.
     * 
     * @return the Global Identifier, e.g. "doi:10.12345/67890"
     */
    public String asString() {
        if (protocol == null || authority == null || identifier == null) {
            return "";
        }
        return protocol + ":" + authority + separator + identifier;
    }
    
    public String asURL() {
        URL url = null;
        if (identifier == null){
            return null;
        }
        try {
               url = new URL(urlPrefix + authority + separator + identifier);
               return url.toExternalForm();
        } catch (MalformedURLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public String asRawIdentifier() {
        if (protocol == null || authority == null || identifier == null) {
            return "";
        }
        return authority + separator + identifier;
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


}
