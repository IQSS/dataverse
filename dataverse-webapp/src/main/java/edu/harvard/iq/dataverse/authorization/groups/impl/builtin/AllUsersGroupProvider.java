package edu.harvard.iq.dataverse.authorization.groups.impl.builtin;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import org.hibernate.validator.internal.util.CollectionHelper;

import java.util.Collections;
import java.util.Set;

public class AllUsersGroupProvider implements GroupProvider<AllUsers> {

    private static final AllUsersGroupProvider instance = new AllUsersGroupProvider();

    private AllUsersGroupProvider() {
    }

    public static AllUsersGroupProvider get() {
        return instance;
    }

    @Override
    public String getGroupProviderAlias() {
        return AllUsers.get().getAlias();
    }

    @Override
    public String getGroupProviderInfo() {
        return "Holder for groups built into dataverse.";
    }

    @Override
    public Set<AllUsers> groupsFor(DataverseRequest req, DvObject dvo) {
        return groupsFor(req.getUser());
    }

    @Override
    public Set<AllUsers> groupsFor(RoleAssignee ra, DvObject dvo) {
        return groupsFor(ra);
    }

    @Override
    public Set<AllUsers> groupsFor(DataverseRequest req) {
        return groupsFor(req.getUser());
    }

    @Override
    public Set<AllUsers> groupsFor(RoleAssignee ra) {
        if (ra instanceof User) {
            return CollectionHelper.asSet(AllUsers.get());
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public AllUsers get(String groupAlias) {
        return groupAlias.equals(AllUsers.get().getAlias()) ? AllUsers.get() : null;
    }

    @Override
    public Set<AllUsers> findGlobalGroups() {
        return CollectionHelper.asSet(AllUsers.get());
    }

    @Override
    public Class<AllUsers> providerFor() {
        return AllUsers.class;
    }

    @Override
    public boolean contains(DataverseRequest aRequest, AllUsers group) {
        return true;
    }

}
