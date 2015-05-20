package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.UserNotification.Type;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateSavedSearchCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.LinkDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearch;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchFilterQuery;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
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

        CREATE, INFO, FEATURED
    }
    
    public enum LinkMode {

        SAVEDSEARCH,  LINKDATAVERSE
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
    SavedSearchServiceBean savedSearchService;
    @EJB
    SystemConfig systemConfig;
    @Inject
    SearchIncludeFragment searchIncludeFragment;

    @EJB
    DataverseLinkingServiceBean linkingService;

    private Dataverse dataverse = new Dataverse();
    private EditMode editMode;
    private LinkMode linkMode;

    private Long ownerId;
    private DualListModel<DatasetFieldType> facets = new DualListModel<>(new ArrayList<DatasetFieldType>(), new ArrayList<DatasetFieldType>());
    private DualListModel<Dataverse> featuredDataverses = new DualListModel<>(new ArrayList<Dataverse>(), new ArrayList<Dataverse>());
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

    private List<ControlledVocabularyValue> dataverseSubjectControlledVocabularyValues;

    public List<ControlledVocabularyValue> getDataverseSubjectControlledVocabularyValues() {
        return dataverseSubjectControlledVocabularyValues;
    }

    public void setDataverseSubjectControlledVocabularyValues(List<ControlledVocabularyValue> dataverseSubjectControlledVocabularyValues) {
        this.dataverseSubjectControlledVocabularyValues = dataverseSubjectControlledVocabularyValues;
    }

    private void updateDataverseSubjectSelectItems() {
        DatasetFieldType subjectDatasetField = datasetFieldService.findByName(DatasetFieldConstant.subject);
        setDataverseSubjectControlledVocabularyValues(controlledVocabularyValueServiceBean.findByDatasetFieldTypeId(subjectDatasetField.getId()));

    }
    
    
    public LinkMode getLinkMode() {
        return linkMode;
    }

    public void setLinkMode(LinkMode linkMode) {
        this.linkMode = linkMode;
    }
    
    
    public void setupLinkingPopup (String popupSetting){
        if (popupSetting.equals("link")){
            setLinkMode(LinkMode.LINKDATAVERSE);           
        } else {
            setLinkMode(LinkMode.SAVEDSEARCH); 
        }
        updateLinkableDataverses();
    }

    public void updateLinkableDataverses() {
        dataversesForLinking = new ArrayList();
        linkingDVSelectItems = new ArrayList();
        
        //Since only a super user function add all dvs
        dataversesForLinking = dataverseService.findAll();// permissionService.getDataversesUserHasPermissionOn(session.getUser(), Permission.PublishDataverse);
        
        //for linking - make sure the link hasn't occurred and its not int the tree
        if (this.linkMode.equals(LinkMode.LINKDATAVERSE)) {
        
            dataversesForLinking.remove(dataverseService.findRootDataverse());
            dataversesForLinking.remove(dataverse);
            
            if (dataverse.getOwner() != null ){
               Dataverse testDV = dataverse;
               while(testDV.getOwner() != null){
                   dataversesForLinking.remove(testDV.getOwner());
                   testDV = testDV.getOwner();
               }                
            }
            
            for (Dataverse removeLinked : linkingService.findLinkingDataverses(dataverse.getId())) {
                dataversesForLinking.remove(removeLinked);
            }
        } else{
            //for saved search add all

        }

        for (Dataverse selectDV : dataversesForLinking) {
            linkingDVSelectItems.add(new SelectItem(selectDV.getId(), selectDV.getDisplayName()));
        }

        if (!dataversesForLinking.isEmpty() && dataversesForLinking.size() == 1 && dataversesForLinking.get(0) != null) {
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
        if (dataverse.getAlias() != null || dataverse.getId() != null || ownerId == null) {// view mode for a dataverse
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
            setupForGeneralInfoEdit();
            // FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Create New Dataverse", " - Create a new dataverse that will be a child dataverse of the parent you clicked from. Asterisks indicate required fields."));
        }

        return null;
    }

    public void initFeaturedDataverses() {
        List<Dataverse> featuredSource = new ArrayList<>();
        List<Dataverse> featuredTarget = new ArrayList<>();
        featuredSource.addAll(dataverseService.findAllPublishedByOwnerId(dataverse.getId()));
        featuredSource.addAll(linkingService.findLinkedDataverses(dataverse.getId()));
        List<DataverseFeaturedDataverse> featuredList = featuredDataverseService.findByDataverseId(dataverse.getId());
        for (DataverseFeaturedDataverse dfd : featuredList) {
            Dataverse fd = dfd.getFeaturedDataverse();
            featuredTarget.add(fd);
            featuredSource.remove(fd);
        }
        featuredDataverses = new DualListModel<>(featuredSource, featuredTarget);

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

    private void setupForGeneralInfoEdit() {
        updateDataverseSubjectSelectItems();
        initFacets();
        refreshAllMetadataBlocks();
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
            setupForGeneralInfoEdit();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Edit Dataverse", " - Edit your dataverse and click Save. Asterisks indicate required fields."));
        } else if (editMode == EditMode.FEATURED) {
            initFeaturedDataverses();
        }

    }

    public void refresh() {

    }

    private boolean openMetadataBlock;

    public boolean isOpenMetadataBlock() {
        return openMetadataBlock;
    }

    public void setOpenMetadataBlock(boolean openMetadataBlock) {
        this.openMetadataBlock = openMetadataBlock;
    }

    private boolean editInputLevel;

    public boolean isEditInputLevel() {
        return editInputLevel;
    }

    public void setEditInputLevel(boolean editInputLevel) {
        this.editInputLevel = editInputLevel;
    }

    public void showDatasetFieldTypes(Long mdbId) {
        showDatasetFieldTypes(mdbId, true);
    }

    public void showDatasetFieldTypes(Long mdbId, boolean allowEdit) {
        for (MetadataBlock mdb : allMetadataBlocks) {
            if (mdb.getId().equals(mdbId)) {
                mdb.setShowDatasetFieldTypes(true);
                openMetadataBlock = true;
            }
        }
        setEditInputLevel(allowEdit);
    }

    public void hideDatasetFieldTypes(Long mdbId) {
        for (MetadataBlock mdb : allMetadataBlocks) {
            if (mdb.getId().equals(mdbId)) {
                mdb.setShowDatasetFieldTypes(false);
                openMetadataBlock = false;
            }
        }
        setEditInputLevel(false);
    }

    public void updateInclude(Long mdbId, long dsftId) {
        List<DatasetFieldType> childDSFT = new ArrayList();

        for (MetadataBlock mdb : allMetadataBlocks) {
            if (mdb.getId().equals(mdbId)) {
                for (DatasetFieldType dsftTest : mdb.getDatasetFieldTypes()) {
                    if (dsftTest.getId().equals(dsftId)) {
                        dsftTest.setOptionSelectItems(resetSelectItems(dsftTest));
                        if ((dsftTest.isHasParent() && !dsftTest.getParentDatasetFieldType().isInclude()) || (!dsftTest.isHasParent() && !dsftTest.isInclude())) {
                            dsftTest.setRequiredDV(false);
                        }
                        if (dsftTest.isHasChildren()) {
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

    public List<SelectItem> resetSelectItems(DatasetFieldType typeIn) {
        List retList = new ArrayList();
        if ((typeIn.isHasParent() && typeIn.getParentDatasetFieldType().isInclude()) || (!typeIn.isHasParent() && typeIn.isInclude())) {
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

    public void updateRequiredDatasetFieldTypes(Long mdbId, Long dsftId, boolean inVal) {
        for (MetadataBlock mdb : allMetadataBlocks) {
            if (mdb.getId().equals(mdbId)) {
                for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()) {
                    if (dsft.getId().equals(dsftId)) {
                        dsft.setRequiredDV(!inVal);
                    }
                }
            }
        }
    }

    public void updateOptionsRadio(Long mdbId, Long dsftId) {

        for (MetadataBlock mdb : allMetadataBlocks) {
            if (mdb.getId().equals(mdbId)) {
                for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()) {
                    if (dsft.getId().equals(dsftId)) {
                        dsft.setOptionSelectItems(resetSelectItems(dsft));
                    }
                }
            }
        }
    }


    public String save() {
        List<DataverseFieldTypeInputLevel> listDFTIL = new ArrayList();
        if (editMode != null && editMode.equals(EditMode.INFO)) {

            List<MetadataBlock> selectedBlocks = new ArrayList();
            if (dataverse.isMetadataBlockRoot()) {
                dataverse.getMetadataBlocks().clear();
            }

            for (MetadataBlock mdb : this.allMetadataBlocks) {
                if (dataverse.isMetadataBlockRoot() && (mdb.isSelected() || mdb.isRequired())) {
                    selectedBlocks.add(mdb);
                    for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()) {
                        if (dsft.isRequiredDV() && !dsft.isRequired()
                                && ((!dsft.isHasParent() && dsft.isInclude())
                                || (dsft.isHasParent() && dsft.getParentDatasetFieldType().isInclude()))) {
                            DataverseFieldTypeInputLevel dftil = new DataverseFieldTypeInputLevel();
                            dftil.setDatasetFieldType(dsft);
                            dftil.setDataverse(dataverse);
                            dftil.setRequired(true);
                            dftil.setInclude(true);
                            listDFTIL.add(dftil);
                        }
                        if ((!dsft.isHasParent() && !dsft.isInclude())
                                || (dsft.isHasParent() && !dsft.getParentDatasetFieldType().isInclude())) {
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

            if (!dataverse.isFacetRoot()) {
                facets.getTarget().clear();
            }

        }

        Command<Dataverse> cmd = null;
        //TODO change to Create - for now the page is expecting INFO instead.
        Boolean create;
        if (dataverse.getId() == null) {
            if (session.getUser().isAuthenticated()) {
                dataverse.setOwner(ownerId != null ? dataverseService.find(ownerId) : null);
                create = Boolean.TRUE;
                cmd = new CreateDataverseCommand(dataverse, (AuthenticatedUser) session.getUser(), facets.getTarget(), listDFTIL);
            } else {
                JH.addMessage(FacesMessage.SEVERITY_FATAL, JH.localize("dataverse.create.authenticatedUsersOnly"));
                return null;
            }
        } else {
            create = Boolean.FALSE;
            if (editMode != null && editMode.equals(editMode.FEATURED)) {
                cmd = new UpdateDataverseCommand(dataverse, null, featuredDataverses.getTarget(), session.getUser(), null);
            } else {
                cmd = new UpdateDataverseCommand(dataverse, facets.getTarget(), null, session.getUser(), listDFTIL);                
            }
        }

        try {
            dataverse = commandEngine.submit(cmd);
            if (session.getUser() instanceof AuthenticatedUser) {
                if (create) {
                    userNotificationService.sendNotification((AuthenticatedUser) session.getUser(), dataverse.getCreateDate(), Type.CREATEDV, dataverse.getId());
                }
            }
        
            String message = "";
            if (editMode != null && editMode.equals(editMode.FEATURED)) {
                message = "The featured dataverses for this dataverse have been updated.";
            } else {
                message = (create) ? BundleUtil.getStringFromBundle("dataverse.create.success", Arrays.asList(systemConfig.getGuidesBaseUrl(), systemConfig.getVersion())) : JH.localize("dataverse.update.success");
            }
            JsfHelper.addSuccessMessage(message);
            
            editMode = null;
            return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";            
            

        } catch (CommandException ex) {
            logger.log(Level.SEVERE, "Unexpected Exception calling dataverse command", ex);
            String errMsg = create ? JH.localize("dataverse.create.failure") : JH.localize("dataverse.update.failure");
            JH.addMessage(FacesMessage.SEVERITY_FATAL, errMsg);
            return null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected Exception calling dataverse command", e);
            String errMsg = create ? JH.localize("dataverse.create.failure") : JH.localize("dataverse.update.failure");
            JH.addMessage(FacesMessage.SEVERITY_FATAL, errMsg);
            return null;
        }
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

    public void editMetadataBlocks(boolean checkVal) {
        setInheritMetadataBlockFromParent(checkVal);
        if (!dataverse.isMetadataBlockRoot()) {
            refreshAllMetadataBlocks();
        }
    }

    public void cancelMetadataBlocks() {
        setInheritMetadataBlockFromParent(false);
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

    public String saveLinkedDataverse() {

        if (linkingDataverseId == null) {
            JsfHelper.addSuccessMessage("You must select a linking dataverse.");
            return "";
        }

        AuthenticatedUser savedSearchCreator = getAuthenticatedUser();
        if (savedSearchCreator == null) {
            String msg = "Only authenticated users can link a dataverse.";
            logger.severe(msg);
            JsfHelper.addErrorMessage(msg);
            return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
        }

        linkingDataverse = dataverseService.find(linkingDataverseId);

        LinkDataverseCommand cmd = new LinkDataverseCommand(session.getUser(), linkingDataverse, dataverse);
        try {
            DataverseLinkingDataverse linkedDataverse = commandEngine.submit(cmd);
        } catch (CommandException ex) {
            String msg = "Unable to link " + dataverse.getDisplayName() + " to " + linkingDataverse.getDisplayName() + ". An internal error occurred.";
            logger.severe(msg + " " + ex);
            JsfHelper.addErrorMessage(msg);
            return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
        }

        SavedSearch savedSearchOfChildren = createSavedSearchForChildren(savedSearchCreator);

        boolean createLinksAndIndexRightNow = false;
        if (createLinksAndIndexRightNow) {
            try {
                // create links (does indexing) right now (might be expensive)
                boolean debug = false;
                savedSearchService.makeLinksForSingleSavedSearch(savedSearchOfChildren, debug);
                JsfHelper.addSuccessMessage(dataverse.getDisplayName() + " has been successfully linked to " + linkingDataverse.getDisplayName());
                return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
            } catch (SearchException | CommandException ex) {
                // error: solr is down, etc. can't link children right now
                String msg = dataverse.getDisplayName() + " has been successfully linked to " + linkingDataverse.getDisplayName() + " but contents will not appear until an internal error has been fixed.";
                logger.severe(msg + " " + ex);
                JsfHelper.addErrorMessage(msg);
                return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
            }
        } else {
            // defer: please wait for the next timer/cron job
            JsfHelper.addSuccessMessage(dataverse.getDisplayName() + " has been successfully linked to " + linkingDataverse.getDisplayName() + ". Please wait for its contents to appear.");
            return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
        }
    }

    @Deprecated
    private SavedSearch createSavedOfCurrentDataverse(AuthenticatedUser savedSearchCreator) {
        /**
         * Please note that we are relying on the fact that the Solr ID of a
         * dataverse never changes, unlike datasets and files, which will change
         * from "dataset_10_draft" to "dataset_10" when published, for example.
         */
        String queryForCurrentDataverse = SearchFields.ID + ":" + IndexServiceBean.solrDocIdentifierDataverse + dataverse.getId();
        SavedSearch savedSearchToPersist = new SavedSearch(queryForCurrentDataverse, linkingDataverse, savedSearchCreator);
        SavedSearch savedSearchCreated = savedSearchService.add(savedSearchToPersist);
        return savedSearchCreated;
    }

    private SavedSearch createSavedSearchForChildren(AuthenticatedUser savedSearchCreator) {
        String wildcardQuery = "*";
        SavedSearch savedSearchToPersist = new SavedSearch(wildcardQuery, linkingDataverse, savedSearchCreator);
        String dataversePath = dataverseService.determineDataversePath(dataverse);
        String filterDownToSubtree = SearchFields.SUBTREE + ":\"" + dataversePath + "\"";
        SavedSearchFilterQuery filterDownToSubtreeFilterQuery = new SavedSearchFilterQuery(filterDownToSubtree, savedSearchToPersist);
        savedSearchToPersist.setSavedSearchFilterQueries(Arrays.asList(filterDownToSubtreeFilterQuery));
        SavedSearch savedSearchCreated = savedSearchService.add(savedSearchToPersist);
        return savedSearchCreated;
    }

    public String saveSavedSearch() {
        if (linkingDataverseId == null) {
            JsfHelper.addSuccessMessage("You must select a linking dataverse.");
            return "";
        }
        linkingDataverse = dataverseService.find(linkingDataverseId);

        AuthenticatedUser savedSearchCreator = getAuthenticatedUser();
        if (savedSearchCreator == null) {
            String msg = "Only authenticated users can save a search.";
            logger.severe(msg);
            JsfHelper.addErrorMessage(msg);
            return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
        }

        SavedSearch savedSearch = new SavedSearch(searchIncludeFragment.getQuery(), linkingDataverse, savedSearchCreator);
        savedSearch.setSavedSearchFilterQueries(new ArrayList());
        for (String filterQuery : searchIncludeFragment.getFilterQueriesDebug()) {
            /**
             * @todo Why are there null's here anyway? Turn on debug and figure
             * this out.
             */
            if (filterQuery != null && !filterQuery.isEmpty()) {
                SavedSearchFilterQuery ssfq = new SavedSearchFilterQuery();
                ssfq.setSavedSearch(savedSearch);
                ssfq.setFilterQuery(filterQuery);
                savedSearch.getSavedSearchFilterQueries().add(ssfq);
            }
        }
        CreateSavedSearchCommand cmd = new CreateSavedSearchCommand(session.getUser(), linkingDataverse, savedSearch);
        try {
            commandEngine.submit(cmd);
            JsfHelper.addSuccessMessage("This search is now linked to " + linkingDataverse.getDisplayName());
            //return "";
            return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
        } catch (CommandException ex) {
            String msg = "There was a problem linking this search to yours: " + ex;
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

    private AuthenticatedUser getAuthenticatedUser() {
        User user = session.getUser();
        if (user.isAuthenticated()) {
            return (AuthenticatedUser) user;
        } else {
            return null;
        }
    }

    public String releaseDataverse() {
        if (session.getUser() instanceof AuthenticatedUser) {
            PublishDataverseCommand cmd = new PublishDataverseCommand((AuthenticatedUser) session.getUser(), dataverse);
            try {
                commandEngine.submit(cmd);
                JsfHelper.addSuccessMessage(JH.localize("dataverse.publish.success"));

            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Unexpected Exception calling  publish dataverse command", ex);
                JsfHelper.addErrorMessage(JH.localize("dataverse.publish.failure"));

            }
        } else {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DataverseNotReleased", "Only authenticated users can release a dataverse.");
            FacesContext.getCurrentInstance().addMessage(null, message);
        }
        return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";

    }

    public String deleteDataverse() {
        DeleteDataverseCommand cmd = new DeleteDataverseCommand(session.getUser(), dataverse);
        try {
            commandEngine.submit(cmd);
            JsfHelper.addSuccessMessage(JH.localize("dataverse.delete.success"));
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unexpected Exception calling  delete dataverse command", ex);
            JsfHelper.addErrorMessage(JH.localize("dataverse.delete.failure"));
        }
        return "/dataverse.xhtml?alias=" + dataverse.getOwner().getAlias() + "&faces-redirect=true";
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

        List<MetadataBlock> availableBlocks = new ArrayList();
        //Add System level blocks
        availableBlocks.addAll(dataverseService.findSystemMetadataBlocks());

        Dataverse testDV = dataverse;
        //Add blocks associated with DV
        availableBlocks.addAll(dataverseService.findMetadataBlocksByDataverseId(dataverse.getId()));

        //Add blocks associated with dv going up inheritance tree
        while (testDV.getOwner() != null) {
            availableBlocks.addAll(dataverseService.findMetadataBlocksByDataverseId(testDV.getOwner().getId()));
            testDV = testDV.getOwner();
        }

        for (MetadataBlock mdb : availableBlocks) {
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
                if (!dsft.isChild()) {
                    DataverseFieldTypeInputLevel dsfIl = dataverseFieldTypeInputLevelService.findByDataverseIdDatasetFieldTypeId(dataverseIdForInputLevel, dsft.getId());
                    if (dsfIl != null) {
                        dsft.setRequiredDV(dsfIl.isRequired());
                        dsft.setInclude(dsfIl.isInclude());
                    } else {
                        dsft.setRequiredDV(dsft.isRequired());
                        dsft.setInclude(true);
                    }
                    dsft.setOptionSelectItems(resetSelectItems(dsft));
                    if (dsft.isHasChildren()) {
                        for (DatasetFieldType child : dsft.getChildDatasetFieldTypes()) {
                            DataverseFieldTypeInputLevel dsfIlChild = dataverseFieldTypeInputLevelService.findByDataverseIdDatasetFieldTypeId(dataverseIdForInputLevel, child.getId());
                            if (dsfIlChild != null) {
                                child.setRequiredDV(dsfIlChild.isRequired());
                                child.setInclude(dsfIlChild.isInclude());
                            } else {
                                child.setRequiredDV(child.isRequired());
                                child.setInclude(true);
                            }
                            child.setOptionSelectItems(resetSelectItems(child));
                        }
                    }
                }
            }            
            retList.add(mdb);
        }
        setAllMetadataBlocks(retList);
    }

    public void validateAlias(FacesContext context, UIComponent toValidate, Object value) {
        if (!StringUtils.isEmpty((String) value)) {
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
