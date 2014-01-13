package edu.harvard.iq.dataverse;

import com.sun.org.apache.bcel.internal.Constants;
import edu.harvard.iq.dataverse.engine.command.impl.RenameDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * A page that shows how to use to command engine in Dataverse.
 * @author michael
 */
@ViewScoped
@Named
public class SampleCommandPage {
	@Inject DataverseSession session;     
	
	@EJB
	DataverseRoleServiceBean rolesService;
	
	@EJB
	EjbDataverseEngine engineService;
	
	@EJB
	DataverseUserServiceBean userService;
	
	@EJB
	DataverseServiceBean dataverseService;
	
	private Long selectedDataverseId;
	private Long selectedUserId;
	private String newName;

	
	public void init() {
	}
	
	public void actionSave( ActionEvent e ) {
		Dataverse affected = dataverseService.find( getSelectedDataverseId() );
		try {
			engineService.submit( new RenameDataverseCommand(session.getUser(), affected, getNewName()) );
		} catch (CommandException ex) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), ""));
		}
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
	
}
