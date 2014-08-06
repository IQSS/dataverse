package edu.harvard.iq.dataverse.authorization.groups;

import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import java.util.Set;
import javax.servlet.ServletRequest;


/**
 * An object that contains unbounded number of {@link RoleAssignee}s (e.g Users, other groups).
 * Implementors might want to look at {@link AbstractGroup} for a more convenient implementation.
 * 
 * @author michael
 */
public interface Group extends RoleAssignee {
    
    
    /**
     * A unique identifier of this group within a Dataverse system installation.
     * @return unique id of this group
     * @see #getName() 
     */
    public String getAlias();
    
    /**
     * @return Name of the group (for display purposes)
     * @see #getAlias() 
     */
    public String getName();
    
    /**
     * @return Description of this group
     */
    public String getDescription();
    
    /**
     * Tests to see whether the {@code anAssignee} is a part of {@code this} group, based on the assignee itself and the request.
     * Some groups may determine membership based on request properties, such as IP address.
     * 
     * @param anAssignee The assignee 
     * @param aRequest The request the assignee issued.
     * @return {@code true} iff {@code anAssignee} is in this group; {@code false} otherwise.
     */
    public boolean contains( RoleAssignee anAssignee, ServletRequest aRequest );
    
    public boolean isEditable();
    
    /**
     * References the object that created the group, and may also edit it.
     * @return the creator of this group.
     */
    public GroupCreator getCreator();
    
    /**
     * Lists all the direct sub groups of this group. Used to prevent infinite inclusions, where a group contains itself.
     * @return all the direct sub-groups of this group. 
     */
    public Set<Group> getDirectSubGroups();
    

}
