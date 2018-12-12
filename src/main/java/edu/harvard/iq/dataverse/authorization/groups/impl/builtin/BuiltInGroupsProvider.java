package edu.harvard.iq.dataverse.authorization.groups.impl.builtin;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import java.util.Collections;
import java.util.Set;
import org.hibernate.validator.internal.util.CollectionHelper;

/**
 * Provider for the built-in, hard coded groups. This class is a singleton (no
 * point in having more than one) so please use {@link #get()} to obtain the
 * instance.
 *
 * @author michael
 */
public class BuiltInGroupsProvider implements GroupProvider<Group> {
    
    private static final BuiltInGroupsProvider instance = new BuiltInGroupsProvider();
    
    private BuiltInGroupsProvider(){}
    
    public static BuiltInGroupsProvider get() {
        return instance;
    }

    @Override
    public String getGroupProviderAlias() {
        return "builtIn";
    }

    @Override
    public String getGroupProviderInfo() {
        return "Holder for groups built into dataverse.";
    }

    @Override
    public Set<Group> groupsFor(DataverseRequest req, DvObject dvo ) {
        return groupsFor(req.getUser());
    }

    @Override
    public Set<Group> groupsFor( RoleAssignee ra, DvObject dvo ) {
        return groupsFor(ra);
    }
    
    @Override
    public Set<Group> groupsFor(DataverseRequest req) {
        return groupsFor(req.getUser());
    }

    @Override
    public Set<Group> groupsFor(RoleAssignee ra) {
        if (ra instanceof AuthenticatedUser){
            return CollectionHelper.asSet(AllUsers.get(), AuthenticatedUsers.get());
        } else if ( ra instanceof User) {
            return Collections.singleton(AllUsers.get());
        } else {
            return Collections.emptySet();
        }
    }
    
    @Override
    public Group get(String groupAlias) {
        return groupAlias.equals(AllUsers.get().getAlias()) ? AllUsers.get()
                : ( groupAlias.equals(AuthenticatedUsers.get().getAlias()) ? AuthenticatedUsers.get() : null );
    }

    @Override
    public Set<Group> findGlobalGroups() {
        return CollectionHelper.asSet(AllUsers.get(), AuthenticatedUsers.get());
    }
}
