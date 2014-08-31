package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.List;

public interface UserLister {

    public List<User> listUsers();

}
