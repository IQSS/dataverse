package edu.harvard.iq.dataverse.authorization.groups.impl.builtin;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import org.hibernate.validator.internal.util.CollectionHelper;

import java.util.Collections;
import java.util.Set;

public class AuthenticatedUsersProvider implements GroupProvider<AuthenticatedUsers> {

    private static final AuthenticatedUsersProvider instance = new AuthenticatedUsersProvider();

    private AuthenticatedUsersProvider() {
    }

    public static AuthenticatedUsersProvider get() {
        return instance;
    }

    @Override
    public String getGroupProviderAlias() {
        return AuthenticatedUsers.get().getAlias();
    }

    @Override
    public String getGroupProviderInfo() {
        return "Holder for groups built into dataverse.";
    }

    @Override
    public Set<AuthenticatedUsers> groupsFor(DataverseRequest req, DvObject dvo) {
        return groupsFor(req.getUser());
    }

    @Override
    public Set<AuthenticatedUsers> groupsFor(RoleAssignee ra, DvObject dvo) {
        return groupsFor(ra);
    }

    @Override
    public Set<AuthenticatedUsers> groupsFor(DataverseRequest req) {
        return groupsFor(req.getUser());
    }

    @Override
    public Set<AuthenticatedUsers> groupsFor(RoleAssignee ra) {
        if (ra instanceof AuthenticatedUser) {
            return Collections.singleton(AuthenticatedUsers.get());
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public AuthenticatedUsers get(String groupAlias) {
        return groupAlias.equals(AuthenticatedUsers.get().getAlias()) ? AuthenticatedUsers.get() : null;
    }

    @Override
    public Set<AuthenticatedUsers> findGlobalGroups() {
        return CollectionHelper.asSet( AuthenticatedUsers.get());
    }

    @Override
    public Class<AuthenticatedUsers> providerFor() {
        return AuthenticatedUsers.class;
    }

    @Override
    public boolean contains(DataverseRequest aRequest, AuthenticatedUsers group) {
        return aRequest.getUser().isAuthenticated();
    }
}
