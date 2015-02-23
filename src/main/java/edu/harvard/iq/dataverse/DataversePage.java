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
import edu.harvard.iq.dataverse.engine.command.impl.LinkDataverseCommand;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import org.primefaces.model.DualListModel;
import javax.ejb.EJBException;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import org.apache.commons.lang.StringUtils;
import org.primefaces.event.TransferEvent;

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
    @EJB
    ControlledVocabularyValueServiceBean controlledVocabularyValueServiceBean;
    
    @EJB
    DataverseLinkingServiceBean linkingService;

    private Dataverse dataverse = new Dataverse();
    private EditMode editMode;
    private Long ownerId;
    private DualListModel<DatasetFieldType> facets;
    private DualListModel<Dataverse> featuredDataverses;
    private List<Dataverse> dataversesForLinking;
    private Long linkingDataverseId;
    private List<SelectItem> linkingDVSelectItems;
    private Dataverse linkingDataverse;
    private List<ControlledVocabularyValue> selectedSubjects;

    public List<ControlledVocabularyValue> getSelectedSubjects() {
        return selectedSubjects;
    }

    public void setSelectedSubjects(List<ControlledVocabularyValue> selectedSubjects) {
        this.selectedSubjects = selectedSubjects;
    }

    public Dataverse getLinkingDataverse() {
        return linkingDataverse;
    }

    public void setLinkingDataverse(Dataverse linkingDataverse) {
        this.linkingDataverse = linkingDataverse;
    }

    public List<SelectItem> getLinkingDVSelectItems() {
        return linkingDVSelectItems;
    }

    public void setLinkingDVSelectItems(List<SelectItem> linkingDVSelectItems) {
        this.linkingDVSelectItems = linkingDVSelectItems;
    }

    public Long getLinkingDataverseId() {
        return linkingDataverseId;
    }

    public void setLinkingDataverseId(Long linkingDataverseId) {
        this.linkingDataverseId = linkingDataverseId;
    }

    public List<Dataverse> getDataversesForLinking() {
        return dataversesForLinking;
    }

    public void setDataversesForLinking(List<Dataverse> dataversesForLinking) {
        
        this.dataversesForLinking = dataversesForLinking;
    }
    
    private List <ControlledVocabularyValue> dataverseSubjectControlledVocabularyValues;

    public List<ControlledVocabularyValue> getDataverseSubjectControlledVocabularyValues() {
        return dataverseSubjectControlledVocabularyValues;
    }

    public void setDataverseSubjectControlledVocabularyValues(List<ControlledVocabularyValue> dataverseSubjectControlledVocabularyValues) {
        this.dataverseSubjectControlledVocabularyValues = dataverseSubjectControlledVocabularyValues;
    }
    



    
    private void updateDataverseSubjectSelectItems(){
        DatasetFieldType subjectDatasetField = datasetFieldService.findByName(DatasetFieldConstant.subject);
        setDataverseSubjectControlledVocabularyValues(controlledVocabularyValueServiceBean.findByDatasetFieldTypeId(subjectDatasetField.getId())); 
        
    }

    public void updateLinkableDataverses(){
        dataversesForLinking = new ArrayList();
        linkingDVSelectItems = new ArrayList();
        List<Dataverse> testingDataverses = permissionService.getDataversesUserHasPermissionOn(session.getUser(), Permission.PublishDataverse);
        for (Dataverse testDV: testingDataverses ){
            Dataverse rootDV = dataverseService.findRootDataverse();
            if(!testDV.equals(rootDV) && !testDV.equals(dataverse) 
                    && !testDV.getOwner().equals(dataverse) 
                    && !dataverse.getOwner().equals(testDV) // && testDV.isReleased() remove released as requirement for linking dv
                    ){               
                dataversesForLinking.add(testDV);
            } 
        }
        for (Dataverse removeLinked: linkingService.findLinkingDataverses(dataverse.getId())){
            dataversesForLinking.remove(removeLinked);
        }
        
        for(Dataverse selectDV : dataversesForLinking){
            linkingDVSelectItems.add(new SelectItem(selectDV.getId(), selectDV.getDisplayName()));
        }
        
        if (!dataversesForLinking.isEmpty() && dataversesForLinking.size() == 1  && dataversesForLinking.get(0) != null){
            linkingDataverse = dataversesForLinking.get(0);
            linkingDataverseId = linkingDataverse.getId();
        }
    }
    
    public void updateSelectedLinkingDV(ValueChangeEvent event) {
        linkingDataverseId = (Long) event.getNewValue();
    }
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
            
            // set defaults - contact e-mail and affiliation from user
            dataverse.getDataverseContacts().add(new DataverseContact(dataverse, session.getUser().getDisplayInfo().getEmailAddress()));
            dataverse.setAffiliation(session.getUser().getDisplayInfo().getAffiliation());
            
            // FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Create New Dataverse", " - Create a new dataverse that will be a child dataverse of the parent you clicked from. Asterisks indicate required fields."));
        }
        
        updateDataverseSubjectSelectItems();

        initFacets();

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
        
    public void initFacets() {        
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
        facetMetadataBlockId = null;
    }    
    
    private Long facetMetadataBlockId;

    public Long getFacetMetadataBlockId() {
        return facetMetadataBlockId;
    }

    public void setFacetMetadataBlockId(Long facetMetadataBlockId) {
        this.facetMetadataBlockId = facetMetadataBlockId;
    }
    
    public void changeFacetsMetadataBlock() {
        if (facetMetadataBlockId == null) {
            facets.setSource(datasetFieldService.findAllFacetableFieldTypes());
        } else {
            facets.setSource(datasetFieldService.findFacetableFieldTypesByMetadataBlock(facetMetadataBlockId));
        }
        
        facets.getSource().removeAll(facets.getTarget());
    }

    public void toggleFacetRoot() {
        if (!dataverse.isFacetRoot()) {
            initFacets();
        }
    }
    
    public void onFacetTransfer(TransferEvent event) {
        for (Object item : event.getItems()) {
            DatasetFieldType facet = (DatasetFieldType) item;
            if (facetMetadataBlockId != null && !facetMetadataBlockId.equals(facet.getMetadataBlock().getId())) {
                facets.getSource().remove(facet);
            }
        }
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
    public String saveFeature() {
        String successMessage = "The featured dataverses for this dataverse have been updated.";
        return save(successMessage);
    } 
    public String save() {
        return save(null);
    }
    
    public String save(String message) {
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
            logger.log(Level.SEVERE, "Unexpected Exception calling dataverse command",ex);
            String errMsg = create ? JH.localize("dataverse.create.failure"): JH.localize("dataverse.update.failure");
            JH.addMessage(FacesMessage.SEVERITY_FATAL, errMsg);
            return null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected Exception calling dataverse command",e);
            JH.addMessage(FacesMessage.SEVERITY_FATAL, JH.localize("dataverse.create.failure"));
            return null;
        }
        if (message==null) {
             message = (create)? JH.localize("dataverse.create.success") : JH.localize("dataverse.update.success");
        }
        JsfHelper.addFlashMessage(message);
        
        return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
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
        if (!dataverse.isMetadataBlockRoot()) {
            refreshAllMetadataBlocks();
        }        
    }

    public boolean isInheritFacetFromParent() {
        return !dataverse.isFacetRoot();
    }

    public void setInheritFacetFromParent(boolean inheritFacetFromParent) {
        dataverse.setFacetRoot(!inheritFacetFromParent);
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
    
    public String saveLinkedDataverse(){
        
        if (linkingDataverseId == null){
           JsfHelper.addFlashMessage( "You must select a linking dataverse."); 
           return "";
        }  
        linkingDataverse = dataverseService.find(linkingDataverseId);
        LinkDataverseCommand cmd = new LinkDataverseCommand(session.getUser(), linkingDataverse, dataverse );
        try {
            commandEngine.submit(cmd);          
            JsfHelper.addFlashMessage( "This dataverse is now linked to " + linkingDataverse.getDisplayName() );
            //return "";
             return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
        } catch (CommandException ex) {
            String msg = "There was a problem linking this dataverse to yours: " + ex;
            logger.severe(msg);
            /**
             * @todo how do we get this message to show up in the GUI?
             */
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DataverseNotLinked", msg);
            FacesContext.getCurrentInstance().addMessage(null, message);
            //return "";
            return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
        }
    }

    public String releaseDataverse() {
        if ( session.getUser() instanceof AuthenticatedUser ) {
            PublishDataverseCommand cmd = new PublishDataverseCommand((AuthenticatedUser) session.getUser(), dataverse);
            try {
                commandEngine.submit(cmd);
                JsfHelper.addFlashMessage( "Your dataverse is now public.");
                return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
            } catch (CommandException ex) {
                String msg = "There was a problem publishing your dataverse: " + ex;
                logger.severe(msg);
                /**
                 * @todo how do we get this message to show up in the GUI?
                 */
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DataverseNotReleased", msg);
                FacesContext.getCurrentInstance().addMessage(null, message);
                return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
            }
        } else {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DataverseNotReleased", "Only authenticated users can release a dataverse.");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
        }
    }

    public String deleteDataverse() {
        DeleteDataverseCommand cmd = new DeleteDataverseCommand(session.getUser(), dataverse);
        try {
            commandEngine.submit(cmd);
           JsfHelper.addFlashMessage( "Your dataverse has been deleted.");
          return "/dataverse.xhtml?alias=" + dataverse.getOwner().getAlias() + "&faces-redirect=true";
        } catch (CommandException ex) {
            String msg = "There was a problem deleting your dataverse: " + ex;
            logger.severe(msg);
            /**
             * @todo how do we get this message to show up in the GUI?
             */
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DataverseNotDeleted", msg);
            FacesContext.getCurrentInstance().addMessage(null, message);
            return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
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
