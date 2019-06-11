package edu.harvard.iq.dataverse.authorization.groups;

import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.impl.PersistedGlobalGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsers;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;


/**
 * An object that contains unbounded number of {@link RoleAssignee}s (e.g Users, other groups).
 * Implementors might want to look at {@link PersistedGlobalGroup} for a more convenient implementation.
 * 
 * @author michael
 */
public interface Group extends RoleAssignee {
    
    public static final String IDENTIFIER_PREFIX = "&";
    public static final String PATH_SEPARATOR = "/";
    
    /**
     * A unique identifier of this group within a Dataverse system installation.
     * Normally consists of the group's provider's alias as a prefix, then {@link Group#PATH_SEPARATOR},
     * and then a unique id of the group, within the provider's groups.
     * <br />
     * <strong>NOTE</strong> Groups live in different tables in the DB, if they live in teh DB at all; Some groups
     * are in-memory only, such as {@link AuthenticatedUsers}. Don't relay on database consistency to maintain 
     * uniqueness.
     * 
     * @return unique id of this group
     * @see #getDisplayName() 
     */
    public String getAlias();
    
    /**
     * @return Name of the group (for display purposes)
     * @see #getAlias() 
     */
    public String getDisplayName();
    
    
    /**
     * @return Description of this group
     */
    public String getDescription();
    
    /**
     * Tests to see whether {@code aRequest} is a part of {@code this} group. Inclusion
     * in groups is not just a matter of user, as it can be specified also by, e.g. IP addresses.
     * The containment may not always be present in the DB - for example, 
     * some groups may determine membership based on request properties, such as IP address.
     * 
     * @param aRequest The request whose inclusion we test 
     * @return {@code true} iff {@code anAssignee} is in this group; {@code false} otherwise.
     */
    public boolean contains( DataverseRequest aRequest );
    
    public boolean isEditable();
    
    /**
     * References the object that created the group, and may also edit it.
     * @return the creator of this group.
     */
    public GroupProvider getGroupProvider();
    
}
