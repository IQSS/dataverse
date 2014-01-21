package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.Permission;
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

/**
 * 
 * @author michael
 */
@ViewScoped
@Named
public class ManageRolesPage implements java.io.Serializable {
    
	public enum PageMode {LIST, VIEW, EDIT };

    @Inject DataverseSession session;     
	
	@EJB
	DataverseRoleServiceBean rolesService;
	
	@Inject
	DataversePage dataversePage;
	
	private List<String> selectedPermissions;
	
	private PageMode mode = null;
	
	private String intent;
	private Long viewRoleId;
	private DataverseRole role;
	private DataverseRole defaultUserRole;
	private boolean permissionRoot;
	
	public void init() {
		if ( mode != null ) return;
		if ( viewRoleId != null ) {
			// enter view mode
			setRole( rolesService.find(viewRoleId) );
			if ( role == null ) {
				FacesContext.getCurrentInstance().addMessage(null, 
						new FacesMessage(FacesMessage.SEVERITY_WARN,
								"Can't find role with id '" + viewRoleId + "'",
								"The role might have existed once, but was deleted"));
				mode = PageMode.LIST;
			} else {
				mode = "edit".equals(getIntent()) ? PageMode.EDIT : PageMode.VIEW;
				dataversePage.setDataverse( role.getOwner() );
			}
		} else {
			mode = PageMode.LIST;
		}
	}
	
	public List<DataverseRole> getRoles() {
		return rolesService.findByOwnerId(dataversePage.getDataverse().getId());
	}
	
	public List<Permission> getPermissions() {
		return Arrays.asList(Permission.values());
	}
	
	public boolean hasRoles() {
		return ! getRoles().isEmpty();
	}
	
	public void createNewRole( ActionEvent e ) {
		mode = PageMode.EDIT;
		DataverseRole aRole = new DataverseRole();
		aRole.setName("Untitled Role");
		aRole.setAlias("untitled-role");
		setRole( aRole );
	}
	
	public void cancelEdit( ActionEvent e ) {
		mode = PageMode.LIST;
	}
	
	public void saveDataverse( ActionEvent e ) {
		
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
		mode = PageMode.LIST;
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
	
	public PageMode getMode() {
		return mode;
	}

	public void setMode(PageMode mode) {
		this.mode = mode;
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

	public String getIntent() {
		return intent;
	}

	public void setIntent(String intent) {
		this.intent = intent;
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
	
}
