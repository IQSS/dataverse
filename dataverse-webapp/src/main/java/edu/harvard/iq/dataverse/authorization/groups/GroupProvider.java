package edu.harvard.iq.dataverse.authorization.groups;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import java.util.Set;

/**
 * Creates and manages groups. Also looks up groups for a 
 * {@link User} and a {@link UserRequestMetadata}.
 * 
 * @param <T> the actual type of the group.
 * @author michael
 */
public interface GroupProvider<T extends Group> {
    
    
    /**
     * The alias of this provider. Has to be unique in the system.
     * @return The alias of the factory.
     */
    public String getGroupProviderAlias();
    
    /**
     * @return A human readable display string describing this factory.
     */
    public String getGroupProviderInfo();
    
    /**
     * Looks up the groups this provider has for a role assignee, in the context of a {@link DvObject}.
     * <B>This method should be used for group management. Groups for actual requests should be determined
     * by calling {@link #groupsFor(edu.harvard.iq.dataverse.engine.command.DataverseRequest, edu.harvard.iq.dataverse.DvObject)}.</B>
     * @param ra 
     * @param dvo the DvObject which is the context for the groups. May be {@code null}
     * @return The set of groups the role assignee is a member of.
     * @see #groupsFor(edu.harvard.iq.dataverse.engine.command.DataverseRequest, edu.harvard.iq.dataverse.DvObject)
     */
    public Set<T> groupsFor( RoleAssignee ra, DvObject dvo );
    
    /**
     * Looks up the groups this provider has for a dataverse request, in the context of a {@link DvObject}.
     * @param req The request whose group memberships we evaluate.
     * @param dvo the DvObject which is the context for the groups. May be {@code null}.
     * @return The set of groups the user is member of.
     */
    public Set<T> groupsFor( DataverseRequest req, DvObject dvo );
    
    public Set<T> groupsFor( RoleAssignee ra);
    
    public Set<T> groupsFor(DataverseRequest req);
    
    public T get( String groupAlias );

    public Set<T> findGlobalGroups();
}
