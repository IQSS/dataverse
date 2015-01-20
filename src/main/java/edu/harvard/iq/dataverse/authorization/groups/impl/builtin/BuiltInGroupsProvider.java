package edu.harvard.iq.dataverse.authorization.groups.impl.builtin;

import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
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
    public Set<Group> groupsFor(User u) {
        return (Set<Group>) ((u instanceof AuthenticatedUser)
                ? CollectionHelper.asSet(AllUsers.get(), AuthenticatedUsers.get())
                : Collections.singleton(AllUsers.get()));
    }

    @Override
    public Group get(String groupAlias) {
        return groupAlias.equals(AllUsers.get().getDisplayName()) ? AllUsers.get()
                : ( groupAlias.equals(AuthenticatedUsers.get().getDisplayName()) ? AuthenticatedUsers.get() : null );
    }

    @Override
    public Set<Group> findAll() {
        return CollectionHelper.asSet(AllUsers.get(), AuthenticatedUsers.get());
    }

}
