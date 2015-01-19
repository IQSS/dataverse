package edu.harvard.iq.dataverse.authorization.providers.shib;

import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.PersistedGlobalGroup;
import java.util.Map;
import java.util.regex.Pattern;

public class ShibGroup extends PersistedGlobalGroup {

    private Map<String, Pattern> headerMatches;

    public Map<String, Pattern> getHeaderMatches() {
        return headerMatches;
    }

    @Override
    public boolean contains(User aUser) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public GroupProvider getGroupProvider() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getIdentifier() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
