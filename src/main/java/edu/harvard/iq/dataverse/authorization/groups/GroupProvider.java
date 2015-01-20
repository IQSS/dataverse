package edu.harvard.iq.dataverse.authorization.groups;

import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.authorization.users.UserRequestMetadata;
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
    
    
    public Set<T> groupsFor( User u );
    
    public T get( String groupAlias );

    public Set<T> findAll();
}
