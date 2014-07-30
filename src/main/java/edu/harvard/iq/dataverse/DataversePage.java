/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.UserNotification.Type;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
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
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import org.primefaces.model.DualListModel;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named("DataversePage")
public class DataversePage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DataversePage.class.getCanonicalName());

    public enum EditMode {

        CREATE, INFO, PERMISSIONS, SETUP, THEME
    }

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
    @EJB
    DatasetFieldServiceBean datasetFieldService;
    @EJB
    DataverseFacetServiceBean dataverseFacetService;
    @EJB
    UserNotificationServiceBean userNotificationService;
    @EJB
    FeaturedDataverseServiceBean featuredDataverseService;

    private Dataverse dataverse = new Dataverse();
    private EditMode editMode;
    private Long ownerId;
    private DualListModel<DatasetFieldType> facets;
    private DualListModel<Dataverse> featuredDataverses;

//    private TreeNode treeWidgetRootNode = new DefaultTreeNode("Root", null);
    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public EditMode getEditMode() {
        return editMode;
    }

    public void setEditMode(EditMode editMode) {
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
            editMode = EditMode.INFO;
            dataverse.setOwner(dataverseService.find(ownerId));
            dataverse.setContactEmail(session.getUser().getEmail());
            dataverse.setAffiliation(session.getUser().getAffiliation());
            // FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Create New Dataverse", " - Create a new dataverse that will be a child dataverse of the parent you clicked from. Asterisks indicate required fields."));
        } else { // view mode for root dataverse (or create root dataverse)
            try {
                dataverse = dataverseService.findRootDataverse();
            } catch (EJBException e) {
                if (e.getCause() instanceof NoResultException) {
                    editMode = EditMode.INFO;
                    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Create Root Dataverse", " - To get started, you need to create your root dataverse. Asterisks indicate required fields."));
                } else {
                    throw e;
                }
            }
        }

        List<DatasetFieldType> facetsSource = new ArrayList<>();
        List<DatasetFieldType> facetsTarget = new ArrayList<>();

        facetsSource.addAll(datasetFieldService.findAllFacetableFieldTypes());

        List<DataverseFacet> facetsList = dataverseFacetService.findByDataverseId(dataverse.getId());
        for (DataverseFacet dvFacet : facetsList) {
            DatasetFieldType dsfType = dvFacet.getDatasetFieldType();
            facetsTarget.add(dsfType);
            facetsSource.remove(dsfType);
        }
        facets = new DualListModel<>(facetsSource, facetsTarget);

        List<Dataverse> featuredSource = new ArrayList<>();
        List<Dataverse> featuredTarget = new ArrayList<>();
        featuredSource.addAll(dataverseService.findAllPublishedByOwnerId(dataverse.getId()));
        List<DataverseFeaturedDataverse> featuredList = featuredDataverseService.findByDataverseId(dataverse.getId());
        for (DataverseFeaturedDataverse dfd : featuredList) {
            Dataverse fd = dfd.getFeaturedDataverse();
            featuredTarget.add(fd);
            featuredSource.remove(fd);
        }
        featuredDataverses = new DualListModel<>(featuredSource, featuredTarget);
    }

    public List<Dataverse> getCarouselFeaturedDataverses() {
        List<Dataverse> retList = new ArrayList();
        List<DataverseFeaturedDataverse> featuredList = featuredDataverseService.findByDataverseId(dataverse.getId());
        for (DataverseFeaturedDataverse dfd : featuredList) {
            Dataverse fd = dfd.getFeaturedDataverse();
            retList.add(fd);
        }
        return retList;
    }

    public List getContents() {
        List contentsList = dataverseService.findByOwnerId(dataverse.getId());
        contentsList.addAll(datasetService.findByOwnerId(dataverse.getId()));
        return contentsList;
    }

    public void edit(EditMode editMode) {
        this.editMode = editMode;
        if (editMode == EditMode.INFO) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Edit Dataverse", " - Edit your dataverse and click Save. Asterisks indicate required fields."));
        } else if (editMode == EditMode.SETUP) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Edit Dataverse Setup", " - Edit the Metadata Blocks and Facets you want to associate with your dataverse. Note: facets will appear in the order shown on the list."));
        }
    }

    public String save() {
        Command<Dataverse> cmd = null;
        //TODO change to Create - for now the page is expecting INFO instead.
        if (dataverse.getId() == null) {
            dataverse.setOwner(ownerId != null ? dataverseService.find(ownerId) : null);
            cmd = new CreateDataverseCommand(dataverse, session.getUser());
        } else {
            cmd = new UpdateDataverseCommand(dataverse, facets.getTarget(), featuredDataverses.getTarget(), session.getUser());
        }

        try {
            dataverse = commandEngine.submit(cmd);
            userNotificationService.sendNotification(session.getUser(), dataverse.getCreateDate(), Type.CREATEDV, dataverse.getId());
            editMode = null;
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage());
            return null;
        }

        return "/dataverse.xhtml?id=" + dataverse.getId() + "&faces-redirect=true";
    }

    public void cancel(ActionEvent e) {
        // reset values
        dataverse = dataverseService.find(dataverse.getId());
        ownerId = dataverse.getOwner() != null ? dataverse.getOwner().getId() : null;
        editMode = null;
    }

    public boolean isRootDataverse() {
        return dataverse.getOwner() == null;
    }

    public Dataverse getOwner() {
        return (ownerId != null) ? dataverseService.find(ownerId) : null;
    }

    // METHODS for Dataverse Setup
    public boolean isInheritMetadataBlockFromParent() {
        return !dataverse.isMetadataBlockRoot();
    }

    public void setInheritMetadataBlockFromParent(boolean inheritMetadataBlockFromParent) {
        dataverse.setMetadataBlockRoot(!inheritMetadataBlockFromParent);
    }

    public void editMetadataBlocks() {
        if (dataverse.isMetadataBlockRoot()) {
            dataverse.getMetadataBlocks().addAll(dataverse.getOwner().getMetadataBlocks());
        } else {
            dataverse.getMetadataBlocks(true).clear();
        }
    }

    public boolean isInheritFacetFromParent() {
        return !dataverse.isFacetRoot();
    }

    public void setInheritFacetFromParent(boolean inheritFacetFromParent) {
        dataverse.setFacetRoot(!inheritFacetFromParent);
    }

    public void editFacets() {
        if (dataverse.isFacetRoot()) {
            dataverse.getDataverseFacets().addAll(dataverse.getOwner().getDataverseFacets());
        } else {
            dataverse.getDataverseFacets(true).clear();
        }
    }

    public DualListModel<DatasetFieldType> getFacets() {
        return facets;
    }

    public void setFacets(DualListModel<DatasetFieldType> facets) {
        this.facets = facets;
    }

    public DualListModel<Dataverse> getFeaturedDataverses() {
        return featuredDataverses;
    }

    public void setFeaturedDataverses(DualListModel<Dataverse> featuredDataverses) {
        this.featuredDataverses = featuredDataverses;
    }

    public String releaseDataverse() {
        PublishDataverseCommand cmd = new PublishDataverseCommand(session.getUser(), dataverse);
        try {
            commandEngine.submit(cmd);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DataverseReleased", "Your dataverse is now public.");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return "/dataverse.xhtml?id=" + dataverse.getId() + "&faces-redirect=true";
        } catch (CommandException ex) {
            String msg = "There was a problem publishing your dataverse: " + ex;
            logger.severe(msg);
            /**
             * @todo how do we get this message to show up in the GUI?
             */
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DataverseNotReleased", msg);
            FacesContext.getCurrentInstance().addMessage(null, message);
            return "/dataverse.xhtml?id=" + dataverse.getId() + "&faces-redirect=true";
        }
    }

    public String deleteDataverse() {
        DeleteDataverseCommand cmd = new DeleteDataverseCommand(session.getUser(), dataverse);
        try {
            commandEngine.submit(cmd);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DataverseDeleted", "Your dataverse ihas been deleted.");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return "/dataverse.xhtml?id=" + dataverse.getOwner().getId() + "&faces-redirect=true";
        } catch (CommandException ex) {
            String msg = "There was a problem deleting your dataverse: " + ex;
            logger.severe(msg);
            /**
             * @todo how do we get this message to show up in the GUI?
             */
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DataverseNotDeleted", msg);
            FacesContext.getCurrentInstance().addMessage(null, message);
            return "/dataverse.xhtml?id=" + dataverse.getId() + "&faces-redirect=true";
        }
    }

    public String getMetadataBlockPreview(MetadataBlock mdb, int numberOfItems) {
        /// for beta, we will just preview the first n fields
        StringBuilder mdbPreview = new StringBuilder();
        int count = 0;
        for (DatasetFieldType dsfType : mdb.getDatasetFieldTypes()) {
            if (!dsfType.isChild()) {
                if (count != 0) {
                    mdbPreview.append(", ");
                    if (count == numberOfItems) {
                        mdbPreview.append("etc.");
                        break;
                    }
                }

                mdbPreview.append(dsfType.getDisplayName());
                count++;
            }
        }

        return mdbPreview.toString();
    }

    public Boolean isEmptyDataverse() {
        return !dataverseService.hasData(dataverse);
    }

    public void validateAlias(FacesContext context, UIComponent toValidate, Object value) {
        String alias = (String) value;
        boolean aliasFound = false;
        Dataverse dv = dataverseService.findByAlias(alias);
        if (editMode == DataversePage.EditMode.CREATE) {
            if (dv != null) {
                aliasFound = true;
            }
        } else {
            if (dv != null && !dv.getId().equals(dataverse.getId())) {
                aliasFound = true;
            }
        }
        if (aliasFound) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage("This Alias is already taken.");
            context.addMessage(toValidate.getClientId(context), message);
        }
    }
}
