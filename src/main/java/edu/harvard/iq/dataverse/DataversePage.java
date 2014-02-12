/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

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
import java.util.logging.Logger;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named("DataversePage")
public class DataversePage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DataversePage.class.getCanonicalName());

    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
	@Inject
	DataverseSession session;
	@EJB
	EjbDataverseEngine commandEngine;
    @EJB
    SearchServiceBean searchService;

    private Dataverse dataverse = new Dataverse();
    private boolean editMode = false;
    private Long ownerId;
//    private TreeNode treeWidgetRootNode = new DefaultTreeNode("Root", null);

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

//    public TreeNode getTreeWidgetRootNode() {
//        return treeWidgetRootNode;
//    }
//
//    public void setTreeWidgetRootNode(TreeNode treeWidgetRootNode) {
//        this.treeWidgetRootNode = treeWidgetRootNode;
//    }

    public void init() {
        
        // FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Create Root Dataverse", " - To get started, you need to create your root dataverse."));  
        
        if (dataverse.getId() != null) { // view mode for a dataverse           
            dataverse = dataverseService.find(dataverse.getId());
            ownerId = dataverse.getOwner() != null ? dataverse.getOwner().getId() : null;
        } else if (ownerId != null) { // create mode for a new child dataverse
            editMode = true;
            dataverse.setOwner(dataverseService.find(ownerId));
            dataverse.setContactEmail(session.getUser().getEmail());
            dataverse.setAffiliation(session.getUser().getAffiliation());
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Create New Dataverse", " - Create a new dataverse that will be a child dataverse of the parent you clicked from. Asterisks indicate required fields."));
        } else { // view mode for root dataverse (or create root dataverse)
            try {
                dataverse = dataverseService.findRootDataverse();
            } catch (EJBException e) {
                if (e.getCause() instanceof NoResultException) {
                    editMode = true;
                    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Create Root Dataverse", " - To get started, you need to create your root dataverse. Asterisks indicate required fields."));
                } else {
                    throw e;
                }

            }
        }

//        populateTreeWidget(treeWidgetRootNode);
    }

    public List getContents() {
        List contentsList = dataverseService.findByOwnerId(dataverse.getId());
        contentsList.addAll(datasetService.findByOwnerId(dataverse.getId()));
        return contentsList;
    }

    public void edit(ActionEvent e) {
        editMode = true;
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Edit Dataverse", " - Edit your dataverse and click Save. Asterisks indicate required fields."));
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

        public TreeNode populateTreeWidget(TreeNode root) {
        TreeNode firstNode = new DefaultTreeNode();
        if (dataverse.getOwner() != null) {
            TreeNode parentDataverseNode = new DefaultTreeNode(dataverse.getOwner(), root);
            firstNode = parentDataverseNode;
        } else {
            firstNode = root;
        }
        firstNode.setExpanded(true);
        TreeNode currentDataverseNode = new DefaultTreeNode(dataverse, firstNode);
        currentDataverseNode.setExpanded(true);
        currentDataverseNode.setSelectable(false);
        List<Dataverse> childDataversesOfCurrentDataverse = dataverseService.findByOwnerId(dataverse.getId());
        /**
         * @todo: support arbitrary depth of dataverse heirarchy
         */
        for (Dataverse child1 : childDataversesOfCurrentDataverse) {
            TreeNode treeNode1 = new DefaultTreeNode(child1, currentDataverseNode);
            List<Dataverse> childDataversesOfLevel1Dataverse = dataverseService.findByOwnerId(child1.getId());
            for (Dataverse child2 : childDataversesOfLevel1Dataverse) {
                TreeNode treeNode2 = new DefaultTreeNode(child2, treeNode1);
                List<Dataverse> childDataversesOfLevel2Dataverse = dataverseService.findByOwnerId(child2.getId());
                for (Dataverse child3 : childDataversesOfLevel2Dataverse) {
                    TreeNode treeNode3 = new DefaultTreeNode(child3, treeNode2);
                    List<Dataverse> childDataversesOfLevel3Dataverse = dataverseService.findByOwnerId(child3.getId());
                    for (Dataverse child4 : childDataversesOfLevel3Dataverse) {
                        TreeNode treeNode4 = new DefaultTreeNode(child4, treeNode3);
                    }
                }
            }
        }
        return root;
    }

}
