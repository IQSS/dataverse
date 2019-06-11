package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A request for authentication, containing all needed information to allow/deny
 * authentication. While intuitively we may think of such requests as username/password
 * pair, they may also include domain name, IP address etc. Hence single object to rule them all.
 * @author michael
 */
public class AuthenticationRequest {
    
    private final Map<String, String> credentials = new TreeMap<>();
    private IpAddress ipAddress;

    public IpAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(IpAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getCredential(String key) {
        return credentials.get(key);
    }

    public String putCredential(String key, String value) {
        return credentials.put(key, value);
    }

    public Set<String> credentialSet() {
        return credentials.keySet();
    }
    
    
}
