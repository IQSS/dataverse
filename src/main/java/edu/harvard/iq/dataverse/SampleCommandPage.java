package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.impl.RenameDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.ReleaseDataverse;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;

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
	
	private Long selectedDataverseId;
	private Long selectedUserId;
	private String newName;
	private String destinationUserName;
	
	public void init() {
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

	public Long getSelectedUserId() {
		return selectedUserId;
	}

	public void setSelectedUserId(Long selectedUserId) {
		this.selectedUserId = selectedUserId;
	}
	
	public boolean isSetupNeeded() {
		DataverseUser user = userService.findByUserName("PriviledgedPete");
		return  ( user == null );
	}
	
	public void changeUser() {
		session.setUser(null);
		session.setUser( userService.findByUserName(getDestinationUserName()) );
	}
	
	public void setupUsers() {
		logger.info("Setting up users" );
		for ( String s : Arrays.asList("Priviledged Pete","Unpriviledged Uma", "Gabbi Guest") ) {
			DataverseUser dvu = new DataverseUser();
			String[] names = s.split(" ");
			dvu.setUserName(names[0]+names[1]);
			dvu.setFirstName(names[0]);
			dvu.setLastName(names[1]);
			dvu.setEmail(names[0] + "." + names[1] + "@malinator.com");
			dvu.setEncryptedPassword( userService.encryptPassword(names[0]) );
			dvu.setInstitution("Sample");
			dvu.setPhone("(888) 888-8888");
			dvu.setPosition( "Other" );
			
			dvu = userService.save(dvu);
			logger.info( "setup " + dvu.getUserName() + " with id " + dvu.getId() );
		}
		
		JH.addMessage(FacesMessage.SEVERITY_INFO, "Users added.");
	}

	public String getDestinationUserName() {
		return destinationUserName;
	}

	public void setDestinationUserName(String destinationUserName) {
		this.destinationUserName = destinationUserName;
	}
	
	
}
