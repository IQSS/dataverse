package edu.harvard.iq.dataverse.authorization.groups.impl;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.authorization.groups.GroupCreator;
import edu.harvard.iq.dataverse.authorization.groups.GroupException;
import java.util.List;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;
import javax.servlet.ServletRequest;

@Entity
public class ExplicitGroup implements Group, java.io.Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    
    /**
     * Authenticated users directly added to the group.
     */
    @ManyToMany
    private Set<AuthenticatedUser> users;
    
    /**
     * Group ids of this group's sub groups.
     */
    @ElementCollection
    private List<String> groupIds;
    
    
    private String title;
    
    @Transient
    private ExplicitGroupCreator creator;
    
    /**
     * {@code true} If the guest is part of this group.
     */
    private boolean containsGuest = false;
    
    public void add( User u ) {
        if ( u instanceof GuestUser ) {
            containsGuest = true;
        } else if ( u instanceof AuthenticatedUser ) {
            users.add((AuthenticatedUser)u);
        } else {
            throw new IllegalArgumentException("Unknown user type " + u.getClass() );
       }
    }
    
    /**
     * Adds the group to {@code this} group. Any assignee in {@code g} will be 
     * in {@code this}.
     * 
     * @param g The group to add
     * @throws GroupException if {@code g} is an ancestor of {@code this}.
     */
    public void add( Group g ) throws GroupException {
        // validate no cycle is going to get created
        
        
        // add
    }
    
    public void remove(RoleAssignee roleAssignee) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getAlias() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean contains(User aUser, ServletRequest aRequest) {
        if ( aUser == GuestUser.get() ) {
            return containsGuest;
        } else {
            // FIXEME implement
            return false;
        }
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public GroupCreator getCreator() {
        return creator;
    }
    
    void setCreator( ExplicitGroupCreator c ) {
        creator = c;
    }

    @Override
    public Set<Group> getDirectSubGroups() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getIdentifier() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo(title, null);
    }

}
