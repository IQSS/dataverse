package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseGuestRolesCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdatePermissionRootCommand;
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

/**
 * 
 * @author michael
 */
@ViewScoped
@Named
public class ManageRolesPage implements java.io.Serializable {
    
	private static final Logger logger = Logger.getLogger(ManageRolesPage.class.getName());
	
    @Inject DataverseSession session;     
	
	@EJB
	DataverseRoleServiceBean rolesSvc;
	
    @EJB
    RoleAssigneeServiceBean roleAssigneeSvc;
    
	@EJB
	UserServiceBean usersSvc;
	
	@EJB
	EjbDataverseEngine engineSvc;
	
	@EJB
	DataverseServiceBean dvSvc;
	
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
			setRole( rolesSvc.find(viewRoleId) );
			if ( getRole() == null ) {
				JH.addMessage(FacesMessage.SEVERITY_WARN, "Can't find role with id '" + viewRoleId + "'",
								"The role might have existed once, but was deleted");
			} 
		} else {
            setRole( new DataverseRole() );
        }
        dataverse = dvSvc.find( Long.parseLong(dataverseIdParam) );
		dvpage.setDataverse(getDataverse());
		setInheritAssignmentsCbValue( ! getDataverse().isPermissionRoot() );
		guestRolesHere = new LinkedList<>();
		guestRolesUp = new LinkedList<>();
		for ( RoleAssignment ra : rolesSvc.roleAssignments(GuestUser.get(), dataverse).getAssignments() ) {
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
		return rolesSvc.findByOwnerId(dataverse.getId());
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
        setRole( rolesSvc.find(Long.parseLong(roleId)) );
    }
    
    public void updatePermissionRoot(javax.faces.event.AjaxBehaviorEvent event) throws javax.faces.event.AbortProcessingException {
        try {
            dataverse = (Dataverse) engineSvc.submit(new UpdatePermissionRootCommand(!isInheritAssignmentsCbValue(), session.getUser(), getDataverse()) );
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
			engineSvc.submit( new UpdateDataverseGuestRolesCommand(guestRolesToAddHere, session.getUser(), getDataverse()));
			engineSvc.submit(new UpdatePermissionRootCommand(isPermissionRoot(), session.getUser(), getDataverse()));
			JH.addMessage(FacesMessage.SEVERITY_INFO, "Dataverse data updated");
		} catch (CommandException ex) {
			JH.addMessage(FacesMessage.SEVERITY_ERROR, "Update failed: "+ ex.getMessage());
		}
	}
	
	public void updateRole( ActionEvent e ) {
		role.setOwner(getDataverse());
		role.clearPermissions();
		for ( String pmsnStr : getSelectedPermissions() ) {
			role.addPermission(Permission.valueOf(pmsnStr) );
		}
        try {
            setRole( engineSvc.submit( new CreateRoleCommand(role, session.getUser(), getDataverse())) );
            JH.addMessage(FacesMessage.SEVERITY_INFO, "Role '" + role.getName() + "' saved", "");
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, "Cannot save role", ex.getMessage() );
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
				rolesSvc.availableRoles(getDataverse().getId()).entrySet() ) {
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
        
		RoleAssignee roas = roleAssigneeSvc.getRoleAssignee( getAssignRoleUsername() );
		DataverseRole r = rolesSvc.find( getAssignRoleRoleId() );
		logger.warning("User: " + roas + " role:" + r );
		
        try {
			engineSvc.submit( new AssignRoleCommand(roas, r, getDataverse(), session.getUser()));
			JH.addMessage(FacesMessage.SEVERITY_INFO, "Role " + r.getName() + " assigned to " + roas.getDisplayInfo().getTitle() + " on " + getDataverse().getName() );
		} catch (CommandException ex) {
			JH.addMessage(FacesMessage.SEVERITY_ERROR, "Can't assign role: " + ex.getMessage() );
		}
	}
	
	public List<RoleAssignmentRow> getRoleAssignments() {
		Set<RoleAssignment> ras = rolesSvc.rolesAssignments(getDataverse());
		List<RoleAssignmentRow> raList = new ArrayList<>(ras.size());
		for ( RoleAssignment ra : ras ) {
			raList.add( new RoleAssignmentRow(ra,  roleAssigneeSvc.getRoleAssignee(ra.getAssigneeIdentifier()).getDisplayInfo()) );
		}
		
		return raList;
	}
	
	public void revokeRole( Long roleAssignmentId ) {
		try {
			engineSvc.submit( new RevokeRoleCommand(em.find(RoleAssignment.class, roleAssignmentId), session.getUser()));
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
		private final RoleAssigneeDisplayInfo assigneeDisplayInfo;
		private final RoleAssignment ra;

		public RoleAssignmentRow(RoleAssignment anRa, RoleAssigneeDisplayInfo disInf) {
			ra = anRa;
			assigneeDisplayInfo = disInf;
		}

        public RoleAssigneeDisplayInfo getAssigneeDisplayInfo() {
            return assigneeDisplayInfo;
        }

		public DataverseRole getRole() {
			return ra.getRole();
		}

		public Dataverse getAssignDv() {
			return (Dataverse) ra.getDefinitionPoint();
		}

		public String getRoleName() {
			return getRole().getName();
		}
		
		public String getAssignedDvName() {
			return getAssignDv().getName();
		}
		
		public Long getId() {
			return ra.getId();
		}

	}
}
