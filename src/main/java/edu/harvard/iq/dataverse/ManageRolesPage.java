package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * 
 * @author michael
 */
@ViewScoped
@Named
public class ManageRolesPage implements java.io.Serializable {
    
	private static final Logger logger = Logger.getLogger(ManageRolesPage.class.getName());
	
	public enum Intent { LIST, VIEW, EDIT };
	public enum ObjectType { DATAVERSE, ROLES, USERS };
	
    @Inject DataverseSession session;     
	
	@EJB
	DataverseRoleServiceBean rolesService;
	
	@EJB
	DataverseUserServiceBean usersService;
	
	@EJB
	EjbDataverseEngine engineService;
	
	@EJB
	DataverseServiceBean dvService;
	
	@PersistenceContext(unitName = "VDCNet-ejbPU")
	EntityManager em;
	
	@Inject
	DataversePage dvpage;
	
	private List<String> selectedPermissions;
	
	private Intent intent = null;
	
	private String intentParam;
	private Long viewRoleId;
	private int activeTabIndex;
	private DataverseRole role;
	private DataverseRole defaultUserRole;
	private boolean permissionRoot;
	private String objectTypeParam;
	private Long dataverseIdParam;
	private ObjectType objectType;
	private String assignRoleUsername;
	private Dataverse dataverse;
	private Long assignRoleRoleId;
	
	public void init() {
		
		// decide object type
		objectType = JH.enumValue(getObjectTypeParam(), ObjectType.class, ObjectType.DATAVERSE);
		setActiveTab(objectType);
		setIntent( JH.enumValue(getIntentParam(), Intent.class, Intent.LIST));
		dataverse = dvService.find( getDataverseIdParam() );
		if ( viewRoleId != null ) {
			// enter view mode
			setRole( rolesService.find(viewRoleId) );
			if ( getRole() == null ) {
				JH.addMessage(FacesMessage.SEVERITY_WARN, "Can't find role with id '" + viewRoleId + "'",
								"The role might have existed once, but was deleted");
				setIntent( Intent.LIST );
			} 
		}
		dvpage.setDataverse(dataverse);
	}
	
	public List<DataverseRole> getRoles() {
		return rolesService.findByOwnerId(dataverse.getId());
	}
	
	public List<Permission> getPermissions() {
		return Arrays.asList(Permission.values());
	}
	
	public boolean isHasRoles() {
		return ! getRoles().isEmpty();
	}
	
	public void createNewRole( ActionEvent e ) {
		setIntent(Intent.EDIT);
		DataverseRole aRole = new DataverseRole();
		setRole( aRole );
		setActiveTab(ObjectType.ROLES);
		logger.info("Create new role");
	}
	
	public void cancelEdit( ActionEvent e ) {
		intent = Intent.LIST;
	}
	
	public void saveDataverse( ActionEvent e ) {
		// TODO do
	}
	
	public void saveRole( ActionEvent e ) {
		role.setOwner(dataverse);
		role.permissions().clear();
		for ( String pmsnStr : getSelectedPermissions() ) {
			role.addPermission(Permission.valueOf(pmsnStr) );
		}
		setRole( rolesService.save(role) );;
		JH.addMessage(FacesMessage.SEVERITY_INFO, "Role '" + role.getName() + "' saved", "");
		intent = Intent.LIST;
	}

	public List<Permission> getRolePermissions() {
		return (role != null ) ? new ArrayList( role.permissions() ) : Collections.emptyList();
	}
	
	public List<String> getSelectedPermissions() {
		return selectedPermissions;
	}

	public void setSelectedPermissions(List<String> selectedPermissions) {
		this.selectedPermissions = selectedPermissions;
	}
	
	public Intent getIntent() {
		return intent;
	}

	public void setIntent(Intent anIntent) {
		this.intent = anIntent;
	}

	public DataverseRole getRole() {
		return role;
	}

	public void setRole(DataverseRole role) {
		this.role = role;
		selectedPermissions = new LinkedList<>();
		if ( role != null ) {
			for ( Permission p : role.permissions() ) {
				selectedPermissions.add( p.name() );
			}
		}
	}
	
	public List<DataverseRole> availableRoles() {
		List<DataverseRole> roles = new LinkedList<>();
		for ( Map.Entry<Dataverse,Set<DataverseRole>> e : 
				rolesService.availableRoles(dataverse.getId()).entrySet() ) {
			for ( DataverseRole aRole : e.getValue() ) {
				roles.add( aRole );
			}
		}
		
		return roles;
	}
	
	public void assignRole( ActionEvent evt ) {
		DataverseUser u = usersService.findByUserName(getAssignRoleUsername());
		DataverseRole r = rolesService.find( getAssignRoleRoleId() );
		
		try {
			engineService.submit( new AssignRoleCommand(u, r, dataverse, session.getUser()));
			JH.addMessage(FacesMessage.SEVERITY_INFO, "Role " + r.getName() + " assigned to " + u.getFirstName() + " " + u.getLastName() + " on " + dataverse.getName() );
		} catch (CommandException ex) {
			JH.addMessage(FacesMessage.SEVERITY_ERROR, "Can't assign role: " + ex.getMessage() );
		}
	}
	
	// TODO add an itemtip, as per http://www.primefaces.org/showcase/ui/autoCompleteItemtip.jsf
	public List<String> acUsername( String input ) {
		List<DataverseUser> users = usersService.listByUsernamePart(input);
		List<String> out = new ArrayList<>(users.size());
		for ( DataverseUser u : users ) {
			out.add( u.getUserName() );
		}
		return out;
	}
	
	public List<RoleAssignmentRow> getRoleAssignments() {
		Set<RoleAssignment> ras = rolesService.rolesAssignments(dataverse);
		List<RoleAssignmentRow> raList = new ArrayList<>(ras.size());
		for ( RoleAssignment ra : ras ) {
			raList.add( new RoleAssignmentRow(ra) );
		}
		
		return raList;
	}
	
	public void revokeRole( Long roleAssignmentId ) {
		
		try {
			engineService.submit( new RevokeRoleCommand(em.find(RoleAssignment.class, roleAssignmentId), session.getUser()));
			JH.addMessage(FacesMessage.SEVERITY_INFO, "Role assignment revoked successfully");
		} catch (PermissionException ex) {
			JH.addMessage(FacesMessage.SEVERITY_ERROR, "Cannot revoke role assignment - you're missing permission", ex.getRequiredPermissions().toString());
			logger.log( Level.SEVERE, "Error revoking role assignment: " + ex.getMessage(), ex );
			
		} catch (CommandException ex) {
			JH.addMessage(FacesMessage.SEVERITY_ERROR, "Cannot revoke role assignment: " + ex.getMessage());
			logger.log( Level.SEVERE, "Error revoking role assignment: " + ex.getMessage(), ex );
		}
	}
	
	public Long getViewRoleId() {
		return viewRoleId;
	}

	public void setViewRoleId(Long viewRoleId) {
		this.viewRoleId = viewRoleId;
	}

	public String getIntentParam() {
		return intentParam;
	}

	public void setIntentParam(String intentParam) {
		this.intentParam = intentParam;
	}

	public DataverseRole getDefaultUserRole() {
		return defaultUserRole;
	}

	public void setDefaultUserRole(DataverseRole defaultUserRole) {
		this.defaultUserRole = defaultUserRole;
	}

	public boolean isPermissionRoot() {
		return permissionRoot;
 	}

	public void setPermissionRoot(boolean permissionRoot) {
		this.permissionRoot = permissionRoot;
	}

	public int getActiveTabIndex() {
		return activeTabIndex;
	}

	public void setActiveTabIndex(int activeTabIndex) {
		this.activeTabIndex = activeTabIndex;
	}

	public String getObjectTypeParam() {
		return objectTypeParam;
	}

	public void setObjectTypeParam(String objectTypeParam) {
		this.objectTypeParam = objectTypeParam;
	}

	public void setActiveTab( ObjectType t ) {
		setActiveTabIndex( (t!=null) ? t.ordinal() : 0 );
	}

	public String getAssignRoleUsername() {
		return assignRoleUsername;
	}

	public void setAssignRoleUsername(String assignRoleUsername) {
		this.assignRoleUsername = assignRoleUsername;
	}

	public Long getAssignRoleRoleId() {
		return assignRoleRoleId;
	}

	public void setAssignRoleRoleId(Long assignRoleRoleId) {
		this.assignRoleRoleId = assignRoleRoleId;
	}

	public Long getDataverseIdParam() {
		return dataverseIdParam;
	}

	public void setDataverseIdParam(Long dataverseIdParam) {
		this.dataverseIdParam = dataverseIdParam;
	}

	public Dataverse getDataverse() {
		return dataverse;
	}
	
	public static class RoleAssignmentRow {
		private final String name;
		private final RoleAssignment ra;

		public RoleAssignmentRow(RoleAssignment anRa) {
			ra = anRa;
			this.name = ra.getUser().getFirstName() + " " + ra.getUser().getLastName();
			
		}

		public String getName() {
			return name;
		}

		public DataverseRole getRole() {
			return ra.getRole();
		}

		public Dataverse getAssignDv() {
			return (Dataverse) ra.getDefinitionPoint();
		}

		public DataverseUser getUser() {
			return ra.getUser();
		}
		
		public String getRoleName() {
			return getRole().getName();
		}
		
		public String getEmail() {
			return getUser().getEmail();
		}
		
		public String getAssignedDvName() {
			return getAssignDv().getName();
		}
		
		public Long getId() {
			return ra.getId();
		}

	}
}

