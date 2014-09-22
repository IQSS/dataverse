package edu.harvard.iq.dataverse.authorization.providers.shib;

import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.authorization.groups.GroupCreator;
import edu.harvard.iq.dataverse.authorization.groups.impl.AbstractGroup;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.ServletRequest;

public class ShibGroup extends AbstractGroup {

    private Map<String, Pattern> headerMatches;

    public Map<String, Pattern> getHeaderMatches() {
        return headerMatches;
    }

    @Override
    public boolean contains(User aUser, ServletRequest aRequest) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public GroupCreator getCreator() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getIdentifier() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
