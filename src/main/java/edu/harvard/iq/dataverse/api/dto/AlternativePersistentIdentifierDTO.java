
package edu.harvard.iq.dataverse.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 *
 * @author stephenkraffmiller
 */

public class AlternativePersistentIdentifierDTO implements java.io.Serializable {

    private String id;
    private String authority; 
    private String identifier;
    private String protocol;

    public String getId() {
        return id;
    }

    public String getAuthority() {
        return authority;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }


    
}
