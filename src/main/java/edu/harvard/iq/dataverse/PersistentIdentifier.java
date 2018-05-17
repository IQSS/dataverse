package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.StringUtil;
import java.util.Objects;
import java.util.Optional;

/**
 * A persistent identifier (such as a DOI or a Handle) in a parsed form.
 * @author michael
 */
public class PersistentIdentifier {
    private String protocol;
    private String authority;
    private String identifier;
    
    /**
     * Parses a string into a PID, or fails.
     * 
     * <strong>This is a refactoring form existing code that makes too many
     * assumptions (see L.A.'s comments in code). Will be fixed as a part of
     * a different PR</strong>
     * @param pidString
     * @param doiSeparator Normally {@code SettingsServiceBean.Key.DoiSeparator}
     * @return 
     */
    public static Optional<PersistentIdentifier> parse( String pidString, String doiSeparator ) {
        if ( StringUtil.isEmpty(pidString) ) return Optional.empty();
        pidString = pidString.trim();
        
        PersistentIdentifier pid = new PersistentIdentifier();
        int index1 = pidString.indexOf(':');
        if (index1 == -1) {            
            return Optional.empty();
        } else {
            pid.setProtocol(pidString.substring(0, index1));
        }
        // This is kind of wrong right here: we should not assume that this is *our* DOI - 
        // it can be somebody else's registered DOI that we harvested. And they can 
        // have their own separator characters defined - so we should not assume 
        // that everybody's DOIs will look like ours! 
        // Also, this separator character gets applied to handles lookups too, below. 
        // Which is probably wrong too...
        // -- L.A. 4.2.4
        int index2 = pidString.indexOf(doiSeparator, index1 + 1);
        int index3;
        if (index2 == -1 ) {
            return Optional.empty();
        } else {
            pid.setAuthority(pidString.substring(index1 + 1, index2));
        }
        if (pid.getProtocol().equals("doi")) {

            index3 = pidString.indexOf(doiSeparator, index2 + 1);
            if (index3 == -1 ) {
                pid.setIdentifier(pidString.substring(index2 + 1));
            } else {
                if (index3 > -1) {
                    pid.setAuthority(pidString.substring(index1 + 1, index3));
                    pid.setIdentifier(pidString.substring(index3 + 1).toUpperCase());
                }
            }
        } else {
            pid.setIdentifier(pidString.substring(index2 + 1).toUpperCase());
        }
        return Optional.of(pid);
    }
    
    public PersistentIdentifier() {
    }

    public PersistentIdentifier(String protocol, String authority, String identifier) {
        this.protocol = protocol;
        this.authority = authority;
        this.identifier = identifier;
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

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 61 * hash + Objects.hashCode(this.authority);
        hash = 61 * hash + Objects.hashCode(this.identifier);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PersistentIdentifier other = (PersistentIdentifier) obj;
        return Objects.equals(this.protocol, other.protocol) &&
                Objects.equals(this.authority, other.authority) &&
                Objects.equals(this.identifier, other.identifier);
    }
    
    @Override
    public String toString() {
        return getProtocol() + ":" + getAuthority() + ":" + getIdentifier();
    }
}
