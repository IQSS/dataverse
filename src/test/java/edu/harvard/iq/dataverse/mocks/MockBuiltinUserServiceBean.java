package edu.harvard.iq.dataverse.mocks;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.passwordreset.PasswordResetException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author michael
 */
public class MockBuiltinUserServiceBean extends BuiltinUserServiceBean {

    public final Map<String, BuiltinUser> users = new HashMap<>();
    
    @Override
    public BuiltinUser findByUserName(String userName) {
        return users.get(userName);
    }

    @Override
    public String requestPasswordUpgradeLink(BuiltinUser aUser) throws PasswordResetException {
        return "http://upgrade/" + aUser.getUserName();
    }

    @Override
    public BuiltinUser save(BuiltinUser aUser) {
        if ( aUser.getId() == null ) {
            aUser.setId( MocksFactory.nextId() );
        }
        users.put( aUser.getUserName(), aUser );
        return aUser;
    }

    @Override
    public void removeUser(String userName) {
        users.remove(userName);
    }
    
    
    
}
