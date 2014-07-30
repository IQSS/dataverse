package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseGuestRolesCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataversePermissionRootCommand;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ResourceBundle;
import java.text.MessageFormat;

/**
 * 
 * @author michael
 */
@ViewScoped
@Named
public class ManageRolesPage implements java.io.Serializable {
        
        ResourceBundle rBundle=ResourceBundle.getBundle("ManageRolesBundle");

	private static final Logger logger = Logger.getLogger(ManageRolesPage.class.getName());
	
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
	private String intentParam;
	private Long viewRoleId;
	private int activeTabIndex;
	private DataverseRole role;
	private DataverseRole defaultUserRole;
	private String assignRoleUsername;
    private String dataverseIdParam;
	private Dataverse dataverse;
	private Long assignRoleRoleId;
	private List<DataverseRole> guestRolesHere;
	private List<RoleAssignment> guestRolesUp;
	private List<String> guestRolesHereId;
	
    private boolean inheritAssignmentsCbValue;
    
	public void init() {
		
		// decide object type
		if ( viewRoleId != null ) {
			// enter view mode
			setRole( rolesService.find(viewRoleId) );
			if ( getRole() == null ) {
				JH.addMessage(FacesMessage.SEVERITY_WARN, MessageFormat.format(rBundle.getString("cannotFindRoleSummary"), "'"+ viewRoleId + "'"),
								rBundle.getString("roleMayExistedDetail"));
			} 
		} else {
            setRole( new DataverseRole() );
        }
        dataverse = dvService.find( Long.parseLong(dataverseIdParam) );
		dvpage.setDataverse(getDataverse());
		setInheritAssignmentsCbValue( ! getDataverse().isPermissionRoot() );
		guestRolesHere = new LinkedList<>();
		guestRolesUp = new LinkedList<>();
		for ( RoleAssignment ra : rolesService.roleAssignments(usersService.findGuestUser(), dataverse).getAssignments() ) {
			if ( ra.getDefinitionPoint().equals(dataverse) ) {
				guestRolesHere.add(ra.getRole());
			} else {
				guestRolesUp.add( ra );
			}
		}
		guestRolesHereId = new LinkedList<>();
		for ( DataverseRole aRole : guestRolesHere ) {
			guestRolesHereId.add( Long.toString(aRole.getId()) );
		}
		
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
		DataverseRole aRole = new DataverseRole();
		setRole( aRole );
	}
	
    public void editRole( String roleId ) {
        setRole( rolesService.find(Long.parseLong(roleId)) );
    }
    
    public void updatePermissionRoot(javax.faces.event.AjaxBehaviorEvent event) throws javax.faces.event.AbortProcessingException {
        try {
            dataverse = engineService.submit( new UpdateDataversePermissionRootCommand(!isInheritAssignmentsCbValue(), session.getUser(), getDataverse()) );
            setInheritAssignmentsCbValue( ! dataverse.isPermissionRoot() );
        } catch (CommandException ex) {
            Logger.getLogger(ManageRolesPage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
	public void cancelEdit( ActionEvent e ) {
	}
	
	public void saveDataverse( ActionEvent e ) {
		Set<DataverseRole> guestRolesToAddHere = new HashSet<>();
		for ( String roleId : getGuestRolesHereId() ) {
			guestRolesToAddHere.add( em.find(DataverseRole.class, Long.parseLong(roleId)) );
		}
		
		try {
			engineService.submit( new UpdateDataverseGuestRolesCommand(guestRolesToAddHere, session.getUser(), getDataverse()));
			engineService.submit( new UpdateDataversePermissionRootCommand(isPermissionRoot(), session.getUser(), getDataverse()));
			JH.addMessage(FacesMessage.SEVERITY_INFO, rBundle.getString("dataverseDataUpdatedMsg"));
		} catch (CommandException ex) {
			JH.addMessage(FacesMessage.SEVERITY_ERROR, MessageFormat.format(rBundle.getString("updateFailedMsg"), ex.getMessage()));
		}
	}
	
	public void updateRole( ActionEvent e ) {
		role.setOwner(getDataverse());
		role.clearPermissions();
		for ( String pmsnStr : getSelectedPermissions() ) {
			role.addPermission(Permission.valueOf(pmsnStr) );
		}
        try {
            setRole( engineService.submit( new CreateRoleCommand(role, session.getUser(), getDataverse())) );
            JH.addMessage(FacesMessage.SEVERITY_INFO, MessageFormat.format(rBundle.getString("roleNameSave"), "'"+role.getName()+"'"));
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, rBundle.getString("cannotSaveRoleSummary"), ex.getMessage() );
            Logger.getLogger(ManageRolesPage.class.getName()).log(Level.SEVERE, null, ex);
        }
	}
    
    public void saveNewRole( ActionEvent e ) {
        role.setId( null );
        updateRole( e );
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
				rolesService.availableRoles(getDataverse().getId()).entrySet() ) {
			for ( DataverseRole aRole : e.getValue() ) {
				roles.add( aRole );
			}
		}
		Collections.sort(roles, DataverseRole.CMP_BY_NAME );
		return roles;
	}
	
	public void assignRole( ActionEvent evt ) {
		logger.warning("Username: " + getAssignRoleUsername() );
		logger.warning("RoleID: " + getAssignRoleRoleId());
        
		DataverseUser u = usersService.findByUserName( getAssignRoleUsername() );
		DataverseRole r = rolesService.find( getAssignRoleRoleId() );
		logger.warning("User: " + u + " role:" + r );
		
        try {
			engineService.submit( new AssignRoleCommand(u, r, getDataverse(), session.getUser()));
			JH.addMessage(FacesMessage.SEVERITY_INFO, MessageFormat.format("roleNameAssigntoDataverseSummary", r.getName(),u.getFirstName(),u.getLastName(),getDataverse().getName()));
		} catch (CommandException ex) {
			JH.addMessage(FacesMessage.SEVERITY_ERROR, MessageFormat.format("cannotAssignRoleSummary", ex.getMessage()));
		}
	}
	
	public List<DataverseUser> acUsername( String input ) {
		return usersService.listByUsernamePart(input);
	}
	
	public List<RoleAssignmentRow> getRoleAssignments() {
		Set<RoleAssignment> ras = rolesService.rolesAssignments(getDataverse());
		List<RoleAssignmentRow> raList = new ArrayList<>(ras.size());
		for ( RoleAssignment ra : ras ) {
			raList.add( new RoleAssignmentRow(ra) );
		}
		
		return raList;
	}
	
	public void revokeRole( Long roleAssignmentId ) {
		try {
			engineService.submit( new RevokeRoleCommand(em.find(RoleAssignment.class, roleAssignmentId), session.getUser()));
			JH.addMessage(FacesMessage.SEVERITY_INFO, rBundle.getString("revokedSuccessfullySummary"));
		} catch (PermissionException ex) {
			JH.addMessage(FacesMessage.SEVERITY_ERROR, rBundle.getString("cannotRevokedSummary"), ex.getRequiredPermissions().toString());
			logger.log( Level.SEVERE, "Error revoking role assignment: " + ex.getMessage(), ex );
			
		} catch (CommandException ex) {
			JH.addMessage(FacesMessage.SEVERITY_ERROR, MessageFormat.format(rBundle.getString("cannotRevokeMsg"), ex.getMessage()));
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
		return getDataverse().isPermissionRoot();
 	}

	public int getActiveTabIndex() {
		return activeTabIndex;
	}

	public void setActiveTabIndex(int activeTabIndex) {
		this.activeTabIndex = activeTabIndex;
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

	public Dataverse getDataverse() {
		return dataverse;
	}
	
	public List<RoleAssignment> getGuestRolesUp() {
		return guestRolesUp;
	}

	public List<DataverseRole> getGuestRolesHere() {
		return guestRolesHere;
	}

	public void setGuestRolesHere(List<DataverseRole> guestRolesHere) {
		this.guestRolesHere = guestRolesHere;
	}

	public List<String> getGuestRolesHereId() {
		return guestRolesHereId;
	}

	public void setGuestRolesHereId(List<String> guestUserRolesHereId) {
		this.guestRolesHereId = guestUserRolesHereId;
	}

    public boolean isInheritAssignmentsCbValue() {
        return inheritAssignmentsCbValue;
    }

    public void setInheritAssignmentsCbValue(boolean inheritAssignmentsCbValue) {
        this.inheritAssignmentsCbValue = inheritAssignmentsCbValue;
    }

    public String getDataverseIdParam() {
        return dataverseIdParam;
    }

    public void setDataverseIdParam(String dataverseIdParam) {
        this.dataverseIdParam = dataverseIdParam;
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
