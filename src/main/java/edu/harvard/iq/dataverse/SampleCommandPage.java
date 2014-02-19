package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.impl.RenameDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.ReleaseDataverse;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.ArrayList;
import java.util.UUID;
import javax.ejb.EJBException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * A page that shows how to use to command engine in Dataverse.
 * @author michael
 */
@ViewScoped
@Named
public class SampleCommandPage {
	private static final Logger logger = Logger.getLogger(SampleCommandPage.class.getName());
	
	@Inject DataverseSession session;     
	@Inject DataversePage dataversePage;     
	
	@EJB
	DataverseRoleServiceBean rolesService;
	
	@EJB
	EjbDataverseEngine engineService;
	
	@EJB
	DataverseUserServiceBean userService;
	
	@EJB
	DataverseServiceBean dataverseService;
	
	@EJB
	PermissionServiceBean permissionsService;
	
	@PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
	
	private Long selectedDataverseId;
	private String selectedUserUserName;
	private String newName;
	private Long selectedDvObjectId;
	private DataverseUser selectedUser;
	private String message;
	
	
	public void init() {
		if ( selectedUserUserName == null ) {
			List<DataverseUser> users = userService.findAll();
			if ( ! users.isEmpty() ) {
				selectedUser = users.get(0);
				selectedUserUserName = selectedUser.getUserName();
			}
		} else {
			selectedUser = userService.findByUserName(selectedUserUserName);
		}
		
		if ( selectedDvObjectId == null ) {
			List<DvObject> objects = getDvObjects();
			if ( ! objects.isEmpty() ) {
				selectedDvObjectId = objects.get(0).getId();
			}
		}
		
	}
	
	public void addMessage( ActionEvent e ) {
		JH.addMessage(FacesMessage.SEVERITY_FATAL, "FATAL! " + getMessage(), "Such useful details! Oh My!");
		JH.addMessage(FacesMessage.SEVERITY_ERROR, "ERROR! " + getMessage(), "Such useful details! Oh My!");
		JH.addMessage(FacesMessage.SEVERITY_WARN,  "This is just a warning", getMessage());
		JH.addMessage(FacesMessage.SEVERITY_INFO,  "Informational message: " + getMessage() + ", for your info", "Such useful details! Oh My!");
	}
	
	public void actionSave( ActionEvent e ) {
		Dataverse affected = dataverseService.find( getSelectedDataverseId() );
		try {
			engineService.submit( new RenameDataverseCommand(session.getUser(), affected, getNewName()) );
		} catch (CommandException ex) {
			JH.addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage());
		}
	}
	
	public boolean isCanRelease() {
		return permissionsService.on(dataversePage.getDataverse()).canIssue( ReleaseDataverse.class );
	}
	
	public List<Dataverse> getDataverseList() {
		return dataverseService.findAll();
	}
	
	public void setSelectedDataverseId(Long selectedDataverseId) {
		this.selectedDataverseId = selectedDataverseId;
	}
	
	public Long getSelectedDataverseId() {
		return selectedDataverseId;
	}
	
	public List<DataverseUser> getDataverseUsers() {
		return userService.findAll();
	}
	
	public String getNewName() {
		return newName;
	}

	public void setNewName(String newName) {
		this.newName = newName;
	}

	public String getSelectedUserUserName() {
		return selectedUserUserName;
	}

	public void setSelectedUserUserName(String selectedUserUserName) {
		this.selectedUserUserName = selectedUserUserName;
	}
	
	public boolean isSetupNeeded() {
		DataverseUser user = userService.findByUserName("PrivilegedPete");
		return  ( user == null );
	}
	
	public void updateUserDvOPermissions( ActionEvent e ) {
		
	}
	
	public String randomStr() {
		return UUID.randomUUID().toString();
	}
	
	public List<Permission> userPermissionsOnDvObject() {
		if ( getSelectedDvObjectId() != null 
				&& getSelectedUserUserName() != null ) {
			DvObject obj = em.find(DvObject.class, getSelectedDvObjectId());
			DataverseUser user = userService.findByUserName(getSelectedUserUserName());
			ArrayList<Permission> permissions = new ArrayList<>(permissionsService.permissionsFor(user, obj));
			
			logger.info( "permissions: " + permissions );
			
			return permissions;
			
		} else {
			return null;
		}
	}
	
	public List<DvObject> getDvObjects() {
		return em.createNamedQuery("DvObject.findAll", DvObject.class).getResultList();
	}
	
	public void changeUser() {
		session.setUser(null);
		session.setUser( userService.findByUserName(getSelectedUserUserName()) );
	}
	
	public void setupUsers() {
		try {
			dataverseService.findRootDataverse();
		} catch ( EJBException nre ) {
			logger.info( "Setting up a root dataverse");
			Dataverse root = new Dataverse();
			root.setName("Root dataverse");
			root.setAlias("root-dv");
			root.setContactEmail("root@mailinator.com");
			root.setAffiliation("Affiliation value");
			root.setDescription("Auto-generated dataverse, by SampleCommandPage");
			dataverseService.save(root);
			JH.addMessage(FacesMessage.SEVERITY_INFO, "Root dataverse created.");
		}
		
		logger.info("Setting up users" );
		for ( String s : Arrays.asList("Privileged Pete","Unprivileged Uma", "Gabbi Guest") ) {
			DataverseUser dvu = new DataverseUser();
			String[] names = s.split(" ");
			dvu.setUserName(names[0]+names[1]);
			dvu.setFirstName(names[0]);
			dvu.setLastName(names[1]);
			dvu.setEmail(names[0] + "." + names[1] + "@malinator.com");
			dvu.setEncryptedPassword( userService.encryptPassword(names[0]) );
			dvu.setAffiliation("Sample");
			dvu.setPhone("(888) 888-8888");
			dvu.setPosition( "Other" );
			
			dvu = userService.save(dvu);
			logger.info( "setup " + dvu.getUserName() + " with id " + dvu.getId() );
		}
		
		JH.addMessage(FacesMessage.SEVERITY_INFO, "Users added.");
	}

	public DataverseUser getSelectedUser() {
		return selectedUser;
	}

	public Long getSelectedDvObjectId() {
		return selectedDvObjectId;
	}

	public void setSelectedDvObjectId(Long selectedDvObjectId) {
		this.selectedDvObjectId = selectedDvObjectId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	
}
