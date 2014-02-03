package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.Permission;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import sun.security.x509.EDIPartyName;

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
	
	@Inject
	DataversePage dataversePage;
	
	private List<String> selectedPermissions;
	
	private Intent intent = null;
	
	private String intentParam;
	private Long viewRoleId;
	private int activeTabIndex;
	private DataverseRole role;
	private DataverseRole defaultUserRole;
	private boolean permissionRoot;
	private String objectTypeParam;
	private ObjectType objectType;
	
	public void init() {
		
		// decide object type
		objectType = JH.enumValue(getObjectTypeParam(), ObjectType.class, ObjectType.DATAVERSE);
		setActiveTab(objectType);
		setIntent( JH.enumValue(getIntentParam(), Intent.class, Intent.LIST));
		
		if ( viewRoleId != null ) {
			// enter view mode
			setRole( rolesService.find(viewRoleId) );
			if ( getRole() == null ) {
				JH.addMessage(FacesMessage.SEVERITY_WARN, "Can't find role with id '" + viewRoleId + "'",
								"The role might have existed once, but was deleted");
				setIntent( Intent.LIST );
			} 
		}
	}
	
	public List<DataverseRole> getRoles() {
		return rolesService.findByOwnerId(dataversePage.getDataverse().getId());
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
		role.setOwner(dataversePage.getDataverse());
		role.permissions().clear();
		for ( String pmsnStr : getSelectedPermissions() ) {
			role.addPermission(Permission.valueOf(pmsnStr) );
		}
		setRole( rolesService.save(role) );
		
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(FacesMessage.SEVERITY_INFO, "Role '" + role.getName() + "' saved", ""));
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
}
