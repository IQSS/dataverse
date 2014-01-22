/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.SearchFields;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.NoResultException;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named("DataversePage")
public class DataversePage implements java.io.Serializable {

    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
	@Inject
	DataverseSession session;
	@EJB
	EjbDataverseEngine commandEngine;

    private Dataverse dataverse = new Dataverse();
    private boolean editMode = false;
    private Long ownerId;
    private String q;
    private String dataversePath;
    private String fq0;

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getDataversePath() {
        if (isRootDataverse()) {
            // null? it's because we don't want fq0
            // to return anything about subtrees for searches
            // from the root dataverse
            return null;
        } else {
            // for non-root dataverses, we do want fq0 to have a subtree in it.
            // i.e. "/mra" as a quoted dataverse path
            String dataversePath = dataverseService.determineDataversePath(this.dataverse);
            return SearchFields.SUBTREE + ":\"" + dataversePath + "\"";
        }
    }

    public void setDataversePath(String dataversePath) {
        this.dataversePath = dataversePath;
    }

    public String getFq0() {
        return fq0;
    }

    public void setFq0(String fq0) {
        this.fq0 = fq0;
    }

    public void init() {
        
        // FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Create Root Dataverse", " - To get started, you need to create your root dataverse."));  
        
        if (dataverse.getId() != null) { // view mode for a dataverse           
            dataverse = dataverseService.find(dataverse.getId());
            ownerId = dataverse.getOwner() != null ? dataverse.getOwner().getId() : null;
        } else if (ownerId != null) { // create mode for a new child dataverse
            editMode = true;
            dataverse.setOwner(dataverseService.find(ownerId));
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Create New Dataverse", " - Create a new dataverse that will be a child dataverse of the parent you clicked from."));
        } else { // view mode for root dataverse (or create root dataverse)
            try {
                dataverse = dataverseService.findRootDataverse();
            } catch (EJBException e) {
                if (e.getCause() instanceof NoResultException) {
                    editMode = true;
                    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Create Root Dataverse", " - To get started, you need to create your root dataverse."));
                } else {
                    throw e;
                }

            }
        }
    }

    public List getContents() {
        List contentsList = dataverseService.findByOwnerId(dataverse.getId());
        contentsList.addAll(datasetService.findByOwnerId(dataverse.getId()));
        return contentsList;
    }

    public void edit(ActionEvent e) {
        editMode = true;
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Edit Dataverse", " - Edit your dataverse and click Save."));
    }

    public void save(ActionEvent e) {
        dataverse.setOwner( ownerId != null ? dataverseService.find(ownerId) : null );
		
		CreateDataverseCommand cmd = new CreateDataverseCommand(dataverse, session.getUser());
		
		try {
			dataverse = commandEngine.submit(cmd);
			editMode = false;
		} catch (CommandException ex) {
			JH.addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage());
		}
	
    }

    public void cancel(ActionEvent e) {
        // reset values
        dataverse = dataverseService.find(dataverse.getId());
        ownerId = dataverse.getOwner() != null ? dataverse.getOwner().getId() : null;
        editMode = false;
    }

    public boolean isRootDataverse() {
        if (this.dataverse.equals(dataverseService.findRootDataverse())) {
            return true;
        } else {
            return false;
        }
    }
	
	public Dataverse getOwner() {
		return (ownerId!=null) ? dataverseService.find(ownerId) : null;
	}
}
