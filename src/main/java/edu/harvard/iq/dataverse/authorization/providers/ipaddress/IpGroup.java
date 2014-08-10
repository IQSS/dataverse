package edu.harvard.iq.dataverse.authorization.providers.ipaddress;

import edu.harvard.iq.dataverse.authorization.User;
import edu.harvard.iq.dataverse.authorization.groups.GroupCreator;
import edu.harvard.iq.dataverse.authorization.groups.impl.AbstractGroup;
import javax.servlet.ServletRequest;

public class IpGroup extends AbstractGroup {

    @Override
    public boolean contains(User aUser, ServletRequest aRequest) {
        // Test for IP address, first by header X-Forwarded-for, then by aRequest.getRemoteAddr()
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public GroupCreator getCreator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getIdentifier() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
