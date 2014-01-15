package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.Command;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import static edu.harvard.iq.dataverse.engine.command.CommandHelper.CH;

/**
 * Your one-stop-shop for deciding which user can do what action on which objects (TM).
 * Note that this bean accesses the permissions/user assignment on a read-only basis. 
 * Changing the permissions a user has is done via roles and groups, over at {@link DataverseRoleServiceBean}.
 * @author michael
 */
@Stateless
@Named
public class PermissionServiceBean {
	private static final Logger logger = Logger.getLogger(PermissionServiceBean.class.getName());
	private static final Map<String, Set<Permission>> permissions;
	
	static {
		permissions = new TreeMap<>();
		permissions.put("PriviledgedPete", EnumSet.allOf(Permission.class));
		permissions.put("UnpriviledgedUma", EnumSet.of(Permission.Access, Permission.EditMetadata, Permission.UndoableEdit));
		permissions.put("GabbiGuest", EnumSet.noneOf(Permission.class) );
	}
	
	@EJB
	DataverseUserServiceBean userService;
	
	@Inject
	DataverseSession session;
	
	public class PermissionQuery {
		final DataverseUser user;
		final Dataverse subject;

		public PermissionQuery(DataverseUser user, Dataverse subject) {
			this.user = user;
			this.subject = subject;
		}
		
		public PermissionQuery user( DataverseUser anotherUser ) {
			return new PermissionQuery(anotherUser, subject);
		}
		
		public boolean canIssue( Class<? extends Command> cmd ) {
			return isUserAllowedOn(user, cmd, subject);
		}
		
		public boolean canIssue( String commandName ) throws ClassNotFoundException {
			return isUserAllowedOn(user, 
					(Class<? extends Command>)Class.forName("edu.harvard.iq.dataverse.engine.command.impl." + commandName), subject);
		}
		
		public Set<Permission> getPermissions() {
			return permissionsFor(user, subject);
		}
		
		public boolean has( Permission p ) {
			return getPermissions().contains(p);
		}
		public boolean has( String pName ) {
			return getPermissions().contains( Permission.valueOf(pName) );
		}
	}
	
    public Set<Permission> permissionsFor( DataverseUser u, Dataverse d ) {
		return permissions.containsKey(u.getUserName()) 
				? permissions.get(u.getUserName())
				: EnumSet.noneOf(Permission.class);
	}
	
	/**
	 * For commands with no named dataverses, this allows a quick check whether 
	 * a user can issue the command on the dataverse or not.
	 * @param u
	 * @param commandClass
	 * @param d
	 * @return 
	 */
	public boolean isUserAllowedOn( DataverseUser u, Class<? extends Command> commandClass, Dataverse d ) {
		Map<String, Set<Permission>> required = CH.permissionsRequired(commandClass);
		if ( required.isEmpty() || required.get("")==null ) {
			return true;
		} else {
			Set<Permission> grantedUserPermissions = permissionsFor(u, d);
			Set<Permission> neededPermissions = required.get("");
			
			return grantedUserPermissions.containsAll(neededPermissions);
		}
	}
	
	public PermissionQuery userOn( DataverseUser u, Dataverse d ) {
		if ( u==null ) {
			// get guest user for dataverse d
			u = userService.findByUserName("GabbiGuest");
		}
		return new PermissionQuery(u, d);
	}

	public PermissionQuery on(  Dataverse d ) {
		return userOn( session.getUser(), d );
	}
	
}
