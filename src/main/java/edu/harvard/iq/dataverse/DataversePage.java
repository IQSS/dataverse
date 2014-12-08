/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.UserNotification.Type;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import org.primefaces.model.DualListModel;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.ejb.EJBException;
import javax.faces.model.SelectItem;
import org.apache.commons.lang.StringUtils;

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
    @EJB
    DataverseFieldTypeInputLevelServiceBean dataverseFieldTypeInputLevelService; 
    @EJB
    PermissionServiceBean permissionService;

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
    public String init() {
        if (dataverse.getAlias() != null || dataverse.getId() != null || ownerId == null  ){// view mode for a dataverse
            if (dataverse.getAlias() != null) {
                dataverse = dataverseService.findByAlias(dataverse.getAlias());
            } else if (dataverse.getId() != null) {        
                dataverse = dataverseService.find(dataverse.getId());
            } else {
                try {
                    dataverse = dataverseService.findRootDataverse(); 
                } catch (EJBException e) {
                    // @todo handle case with no root dataverse (a fresh installation) with message about using API to create the root 
                    dataverse = null;
                }
            }

            // check if dv exists and user has permission
            if (dataverse == null) {
                return "/404.xhtml";
            }
            if (!dataverse.isReleased() && !permissionService.on(dataverse).has(Permission.ViewUnpublishedDataverse)) {
                return "/loginpage.xhtml" + DataverseHeaderFragment.getRedirectPage();
            } 
            
            ownerId = dataverse.getOwner() != null ? dataverse.getOwner().getId() : null;
        } else { // ownerId != null; create mode for a new child dataverse
            editMode = EditMode.INFO;
            dataverse.setOwner(dataverseService.find(ownerId));
            if (dataverse.getOwner() == null) {
                return "/404.xhtml";
            } else if (!permissionService.on(dataverse.getOwner()).has(Permission.AddDataverse)) {
                return "/loginpage.xhtml" + DataverseHeaderFragment.getRedirectPage();
            }               
            dataverse.setContactEmail(session.getUser().getDisplayInfo().getEmailAddress());
            dataverse.setAffiliation(session.getUser().getDisplayInfo().getAffiliation());
            dataverse.setFacetRoot(false);
            // FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Create New Dataverse", " - Create a new dataverse that will be a child dataverse of the parent you clicked from. Asterisks indicate required fields."));
        }

        List<DatasetFieldType> facetsSource = new ArrayList<>();
        List<DatasetFieldType> facetsTarget = new ArrayList<>();

        facetsSource.addAll(datasetFieldService.findAllFacetableFieldTypes());
        

        List<DataverseFacet> facetsList = dataverseFacetService.findByDataverseId(dataverse.getFacetRootId());
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
        refreshAllMetadataBlocks();
        
        return null;
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
    
    public void refresh(){
        
    }
    
    private boolean openMetadataBlock;

    public boolean isOpenMetadataBlock() {
        return openMetadataBlock;
    }

    public void setOpenMetadataBlock(boolean openMetadataBlock) {
        this.openMetadataBlock = openMetadataBlock;
    }
    
    public void showDatasetFieldTypes(Long  mdbId) {
        for (MetadataBlock mdb : allMetadataBlocks){
            if(mdb.getId().equals(mdbId)){
                mdb.setShowDatasetFieldTypes(true);
                openMetadataBlock = true;
            }
        }
    }
    
    public void hideDatasetFieldTypes(Long  mdbId) {
        for (MetadataBlock mdb : allMetadataBlocks){
            if(mdb.getId().equals(mdbId)){
                mdb.setShowDatasetFieldTypes(false);
                openMetadataBlock = false;
            }
        }
    }
    public void updateInclude(Long mdbId, long dsftId) {
        List<DatasetFieldType> childDSFT = new ArrayList();
        
        for (MetadataBlock mdb : allMetadataBlocks) {
            if (mdb.getId().equals(mdbId)) {
                for (DatasetFieldType dsftTest : mdb.getDatasetFieldTypes()) {
                    if (dsftTest.getId().equals(dsftId)) {
                        dsftTest.setOptionSelectItems(resetSelectItems(dsftTest));
                        if ((dsftTest.isHasParent() && !dsftTest.getParentDatasetFieldType().isInclude()) || (!dsftTest.isHasParent() && !dsftTest.isInclude())){                         
                            dsftTest.setRequiredDV(false);
                        }
                        if (dsftTest.isHasChildren() && !dsftTest.isInclude()) {
                            childDSFT.addAll(dsftTest.getChildDatasetFieldTypes());
                        }
                    }
                }
            }
        }
        if (!childDSFT.isEmpty()) {
            for (DatasetFieldType dsftUpdate : childDSFT) {
                for (MetadataBlock mdb : allMetadataBlocks) {
                    if (mdb.getId().equals(mdbId)) {
                        for (DatasetFieldType dsftTest : mdb.getDatasetFieldTypes()) {
                            if (dsftTest.getId().equals(dsftUpdate.getId())) {                                
                                dsftTest.setOptionSelectItems(resetSelectItems(dsftTest));
                            }
                        }
                    }
                }                
            }            
        }
    }
    
    public List<SelectItem> resetSelectItems(DatasetFieldType typeIn){
        List retList = new ArrayList();
        if ((typeIn.isHasParent() && typeIn.getParentDatasetFieldType().isInclude()) || (!typeIn.isHasParent() && typeIn.isInclude())){
             SelectItem requiredItem = new SelectItem();
             requiredItem.setLabel("Required");
             requiredItem.setValue(true);
             retList.add(requiredItem);
             SelectItem optional = new SelectItem();
             optional.setLabel("Optional");
             optional.setValue(false);
             retList.add(optional);                         
        } else {
             SelectItem hidden = new SelectItem();
             hidden.setLabel("Hidden");
             hidden.setValue(false);
             hidden.setDisabled(true);
             retList.add(hidden);  
        }
        return retList;
    }
    
    public void updateRequiredDatasetFieldTypes(Long  mdbId, Long dsftId, boolean inVal) {
        for (MetadataBlock mdb : allMetadataBlocks){
            if(mdb.getId().equals(mdbId)){
               for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()){
                   if (dsft.getId().equals(dsftId)){
                       dsft.setRequiredDV(!inVal);
                   }
               }
            }
        }
    }
    
     public void updateOptionsRadio(Long  mdbId, Long dsftId) {

        for (MetadataBlock mdb : allMetadataBlocks){
            if(mdb.getId().equals(mdbId)){
               for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()){
                   if (dsft.getId().equals(dsftId)){                     
                       dsft.setOptionSelectItems(resetSelectItems(dsft));
                   }
               }
            }
        }
    } 

    public String save() {
        List<DataverseFieldTypeInputLevel> listDFTIL = new ArrayList();
        List<MetadataBlock> selectedBlocks = new ArrayList();
        if (dataverse.isMetadataBlockRoot()) {
            dataverse.getMetadataBlocks().clear();
        }

        for (MetadataBlock mdb : this.allMetadataBlocks) {
            if (dataverse.isMetadataBlockRoot() && (mdb.isSelected() || mdb.isRequired())) {
                
                selectedBlocks.add(mdb);
                for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()) {
                    if (dsft.isRequiredDV() && !dsft.isRequired() 
                            && ((!dsft.isHasParent() &&  dsft.isInclude()) || 
                            (dsft.isHasParent() &&  dsft.getParentDatasetFieldType().isInclude()))) {
                        DataverseFieldTypeInputLevel dftil = new DataverseFieldTypeInputLevel();                       
                        dftil.setDatasetFieldType(dsft);
                        dftil.setDataverse(dataverse);
                        dftil.setRequired(true);
                        dftil.setInclude(true);
                        listDFTIL.add(dftil);
                    }
                    if ( (!dsft.isHasParent() && !dsft.isInclude())  ||
                            (dsft.isHasParent() &&  !dsft.getParentDatasetFieldType().isInclude())) {
                        DataverseFieldTypeInputLevel dftil = new DataverseFieldTypeInputLevel();
                        dftil.setDatasetFieldType(dsft);
                        dftil.setDataverse(dataverse);
                        dftil.setRequired(false);
                        dftil.setInclude(false);
                        listDFTIL.add(dftil);
                    }
                }
            }
        }
        
        if (!selectedBlocks.isEmpty()) {
            dataverse.setMetadataBlocks(selectedBlocks);
        }
        
        if(!dataverse.isFacetRoot()){
            facets.getTarget().clear();
        }
        
        Command<Dataverse> cmd = null;
        //TODO change to Create - for now the page is expecting INFO instead.
        Boolean create;
        if (dataverse.getId() == null) {
            dataverse.setOwner(ownerId != null ? dataverseService.find(ownerId) : null);
            create = Boolean.TRUE;
            cmd = new CreateDataverseCommand(dataverse, session.getUser(), facets.getTarget(), listDFTIL);
        } else {
            create=Boolean.FALSE;
            cmd = new UpdateDataverseCommand(dataverse, facets.getTarget(), featuredDataverses.getTarget(), session.getUser(), listDFTIL);
        }

        try {
            dataverse = commandEngine.submit(cmd);
            if (session.getUser() instanceof AuthenticatedUser) {
                userNotificationService.sendNotification((AuthenticatedUser) session.getUser(), dataverse.getCreateDate(), Type.CREATEDV, dataverse.getId());
            }
            editMode = null;
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage());
            return null;
        }
        String msg = (create)? "You have successfully created your dataverse.": "You have successfully updated your dataverse.";
        JsfHelper.addSuccessMessage(msg);
        
        return "/dataverse/" + dataverse.getAlias() + "&faces-redirect=true";
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
        refreshAllMetadataBlocks();
    }

    public boolean isInheritFacetFromParent() {
        return !dataverse.isFacetRoot();
    }

    public void setInheritFacetFromParent(boolean inheritFacetFromParent) {
        dataverse.setFacetRoot(!inheritFacetFromParent);
    }

    public void editFacets() {

        List<DatasetFieldType> facetsSource = new ArrayList<>();
        List<DatasetFieldType> facetsTarget = new ArrayList<>();
        facetsSource.addAll(datasetFieldService.findAllFacetableFieldTypes());
        List<DataverseFacet> facetsList = dataverseFacetService.findByDataverseId(dataverse.getFacetRootId());
        for (DataverseFacet dvFacet : facetsList) {
            DatasetFieldType dsfType = dvFacet.getDatasetFieldType();
            facetsTarget.add(dsfType);
            facetsSource.remove(dsfType);
        }
        facets = new DualListModel<>(facetsSource, facetsTarget);
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
            return "/dataverse/" + dataverse.getAlias() + "&faces-redirect=true";
        } catch (CommandException ex) {
            String msg = "There was a problem publishing your dataverse: " + ex;
            logger.severe(msg);
            /**
             * @todo how do we get this message to show up in the GUI?
             */
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DataverseNotReleased", msg);
            FacesContext.getCurrentInstance().addMessage(null, message);
            return "/dataverse/" + dataverse.getAlias() + "&faces-redirect=true";
        }
    }

    public String deleteDataverse() {
        DeleteDataverseCommand cmd = new DeleteDataverseCommand(session.getUser(), dataverse);
        try {
            commandEngine.submit(cmd);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DataverseDeleted", "Your dataverse ihas been deleted.");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return "/dataverse/" + dataverse.getOwner().getAlias() + "&faces-redirect=true";
        } catch (CommandException ex) {
            String msg = "There was a problem deleting your dataverse: " + ex;
            logger.severe(msg);
            /**
             * @todo how do we get this message to show up in the GUI?
             */
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DataverseNotDeleted", msg);
            FacesContext.getCurrentInstance().addMessage(null, message);
            return "/dataverse/=" + dataverse.getAlias() + "&faces-redirect=true";
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
       private List<MetadataBlock> allMetadataBlocks;

    public List<MetadataBlock> getAllMetadataBlocks() {
        return this.allMetadataBlocks;
    }

    public void setAllMetadataBlocks(List<MetadataBlock> inBlocks) {
        this.allMetadataBlocks = inBlocks;
    }

    private void refreshAllMetadataBlocks() {
        Long dataverseIdForInputLevel = dataverse.getId();
        List<MetadataBlock> retList = new ArrayList();
        for (MetadataBlock mdb : dataverseService.findAllMetadataBlocks()) {
            mdb.setSelected(false);
            mdb.setShowDatasetFieldTypes(false);
            if (!dataverse.isMetadataBlockRoot() && dataverse.getOwner() != null) {
                dataverseIdForInputLevel = dataverse.getMetadataRootId();
                for (MetadataBlock mdbTest : dataverse.getOwner().getMetadataBlocks()) {
                    if (mdb.equals(mdbTest)) {
                        mdb.setSelected(true);
                    }
                }
            } else {
                for (MetadataBlock mdbTest : dataverse.getMetadataBlocks(true)) {
                    if (mdb.equals(mdbTest)) {
                        mdb.setSelected(true);
                    }
                }
            }

            for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()) {
                DataverseFieldTypeInputLevel dsfIl = dataverseFieldTypeInputLevelService.findByDataverseIdDatasetFieldTypeId(dataverseIdForInputLevel, dsft.getId());
                if (dsfIl != null) {
                    dsft.setRequiredDV(dsfIl.isRequired());
                    dsft.setInclude(dsfIl.isInclude());
                } else {
                    dsft.setRequiredDV(dsft.isRequired());
                    dsft.setInclude(true);
                }
                dsft.setOptionSelectItems(resetSelectItems(dsft));
            }
            retList.add(mdb);
        }
        setAllMetadataBlocks(retList);
    }


    public void validateAlias(FacesContext context, UIComponent toValidate, Object value) {
        if (!StringUtils.isEmpty((String)value)) {
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
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "alias", "This Alias is already taken.");
                context.addMessage(toValidate.getClientId(context), message);
            }
        }
    }

}
