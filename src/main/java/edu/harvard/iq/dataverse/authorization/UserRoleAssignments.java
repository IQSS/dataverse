package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A set of assignment a user has. 
 * @author michael
 */
public class UserRoleAssignments implements Iterable<RoleAssignment> {
	
	private final DataverseUser user;
	private final Set<RoleAssignment> assignments = new HashSet<>();
	
	public UserRoleAssignments( DataverseUser aUser) {
		user = aUser;
	}
	
	public void add( Iterable<RoleAssignment> ras ) {
		for ( RoleAssignment ra : ras ) {
			assignments.add(ra);
		}
	}
	
	public void add( RoleAssignment ra ) {
		assignments.add( ra );
	}
	
	public Set<Permission> getPermissions() {
		BitSet acc = new BitSet();
		for ( RoleAssignment ra : assignments ) {
			acc.union( new BitSet(ra.getRole().getPermissionsBits()) );
		}
		return acc.asSetOf( Permission.class );
	}

	public DataverseUser getUser() {
		return user;
	}

	public Set<RoleAssignment> getAssignments() {
		return assignments;
	}

	@Override
	public Iterator<RoleAssignment> iterator() {
		return assignments.iterator();
	}
	
	public boolean isEmpty() {
		return assignments.isEmpty();
	}
}
