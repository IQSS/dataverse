package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.UserNotification.Type;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.dataverse.DataverseUtil;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CheckRateLimitForCollectionPageCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateSavedSearchCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.LinkDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidProviderFactoryBean;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.search.FacetCategory;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SearchIncludeFragment;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearch;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchFilterQuery;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.List;

import edu.harvard.iq.dataverse.util.cache.CacheFactoryBean;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import org.primefaces.model.DualListModel;
import jakarta.ejb.EJBException;
import jakarta.faces.event.ValueChangeEvent;
import jakarta.faces.model.SelectItem;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
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
    @EJB DataverseRoleServiceBean dataverseRoleServiceBean;
    @Inject
    SearchIncludeFragment searchIncludeFragment;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    SettingsWrapper settingsWrapper; 
    @EJB
    DataverseLinkingServiceBean linkingService;
    @Inject PermissionsWrapper permissionsWrapper;
    @Inject 
    NavigationWrapper navigationWrapper;
    @Inject DataverseHeaderFragment dataverseHeaderFragment;
    @EJB
    PidProviderFactoryBean pidProviderFactoryBean;
    @EJB
    CacheFactoryBean cacheFactory;

    private Dataverse dataverse = new Dataverse();  

    /**
     * View parameters
     */
    private Long id = null;
    private String alias = null;
    private Long ownerId = null;    
    private EditMode editMode;
    private LinkMode linkMode;

    private DualListModel<DatasetFieldType> facets = new DualListModel<>(new ArrayList<>(), new ArrayList<>());
    private DualListModel<Dataverse> featuredDataverses = new DualListModel<>(new ArrayList<>(), new ArrayList<>());
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
    
    public boolean showLinkingPopup() {
        String testquery = "";
        if (session.getUser() == null) {
            return false;
        }
        if (dataverse == null) {
            return false;
        }
        if (query != null) {
            testquery = query;
        }

        return (session.getUser().isSuperuser() && (dataverse.getOwner() != null || !testquery.isEmpty()));
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
        dataversesForLinking = new ArrayList<>();
        linkingDVSelectItems = new ArrayList<>();
        
        //Since only a super user function add all dvs
        dataversesForLinking = dataverseService.findAll();// permissionService.getDataversesUserHasPermissionOn(session.getUser(), Permission.PublishDataverse);
        
        /*
        List<DataverseRole> roles = dataverseRoleServiceBean.getDataverseRolesByPermission(Permission.PublishDataverse, dataverse.getId());
        List<String> types = new ArrayList();
        types.add("Dataverse");
        for (Long dvIdAsInt : permissionService.getDvObjectIdsUserHasRoleOn(session.getUser(), roles, types, false)) {
            dataversesForLinking.add(dataverseService.find(dvIdAsInt));
        }*/
        
        //for linking - make sure the link hasn't occurred and its not int the tree
        if (this.linkMode.equals(LinkMode.LINKDATAVERSE)) {
        
            // remove this and it's parent tree
            dataversesForLinking.remove(dataverse);
            Dataverse testDV = dataverse;
            while(testDV.getOwner() != null){
                dataversesForLinking.remove(testDV.getOwner());
                testDV = testDV.getOwner();
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

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }
    
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    public String getAlias() { return this.alias; }
    public void setAlias(String alias) { this.alias = alias; }    

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

    public void updateOwnerDataverse() {
        if (dataverse.getOwner() != null && dataverse.getOwner().getId() != null) {
            ownerId = dataverse.getOwner().getId();
            logger.info("New host dataverse id: " + ownerId);
            // discard the dataverse already created:
            dataverse = new Dataverse();
            // initialize a new new dataverse:
            init();
            dataverseHeaderFragment.initBreadcrumbs(dataverse);
        }
    }
    
    public String init() {
        //System.out.println("_YE_OLDE_QUERY_COUNTER_");  // for debug purposes
        // Check for rate limit exceeded. Must be done before anything else to prevent unnecessary processing.
        if (!cacheFactory.checkRate(session.getUser(), new CheckRateLimitForCollectionPageCommand(null,null))) {
            return navigationWrapper.tooManyRequests();
        }
        if (this.getAlias() != null || this.getId() != null || this.getOwnerId() == null) {// view mode for a dataverse
            if (this.getAlias() != null) {
                dataverse = dataverseService.findByAlias(this.getAlias());
            } else if (this.getId() != null) {
                dataverse = dataverseService.find(this.getId());
            } else {
                try {
                    dataverse = settingsWrapper.getRootDataverse();
                } catch (EJBException e) {
                    // @todo handle case with no root dataverse (a fresh installation) with message about using API to create the root 
                    dataverse = null;
                }
            }

            // check if dv exists and user has permission
            if (dataverse == null) {
                return permissionsWrapper.notFound();
            }
            if (!dataverse.isReleased() && !permissionService.on(dataverse).has(Permission.ViewUnpublishedDataverse)) {
                // the permission lookup above should probably be moved into the permissionsWrapper -- L.A. 5.7
                return permissionsWrapper.notAuthorized();
            }

            ownerId = dataverse.getOwner() != null ? dataverse.getOwner().getId() : null;
        } else { // ownerId != null; create mode for a new child dataverse
            editMode = EditMode.CREATE;
            dataverse.setOwner(dataverseService.find( this.getOwnerId()));
            if (dataverse.getOwner() == null) {
                return  permissionsWrapper.notFound();
            } else if (!permissionService.on(dataverse.getOwner()).has(Permission.AddDataverse)) {
                // the permission lookup above should probably be moved into the permissionsWrapper -- L.A. 5.7
                return permissionsWrapper.notAuthorized();            
            }

            // set defaults - contact e-mail and affiliation from user
            dataverse.getDataverseContacts().add(new DataverseContact(dataverse, session.getUser().getDisplayInfo().getEmailAddress()));
            dataverse.setAffiliation(session.getUser().getDisplayInfo().getAffiliation());
            setupForGeneralInfoEdit();
            // FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Create New Dataverse", " - Create a new dataverse that will be a child dataverse of the parent you clicked from. Asterisks indicate required fields."));
            if (dataverse.getName() == null) {
                dataverse.setName(DataverseUtil.getSuggestedDataverseNameOnCreate(session.getUser()));
            }
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

    private List<Dataverse> carouselFeaturedDataverses = null;
    
    public List<Dataverse> getCarouselFeaturedDataverses() {
        if (carouselFeaturedDataverses != null) {
            return carouselFeaturedDataverses;
        }
        carouselFeaturedDataverses = featuredDataverseService.findByDataverseIdQuick(dataverse.getId());/*new ArrayList();
        
        List<DataverseFeaturedDataverse> featuredList = featuredDataverseService.findByDataverseId(dataverse.getId());
        for (DataverseFeaturedDataverse dfd : featuredList) {
            Dataverse fd = dfd.getFeaturedDataverse();
            retList.add(fd);
        }*/
        
        return carouselFeaturedDataverses;
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
            JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataverse.edit.msg"), BundleUtil.getStringFromBundle("dataverse.edit.detailmsg"));
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
        List<DatasetFieldType> childDSFT = new ArrayList<>();

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
        PrimeFaces.current().executeScript("scrollAfterUpdate();");
    }

    public List<SelectItem> resetSelectItems(DatasetFieldType typeIn) {
        List<SelectItem> retList = new ArrayList<>();
        if ((typeIn.isHasParent() && typeIn.getParentDatasetFieldType().isInclude()) || (!typeIn.isHasParent() && typeIn.isInclude())) {
                SelectItem requiredItem = new SelectItem();
                requiredItem.setLabel(BundleUtil.getStringFromBundle("dataverse.item.required"));
                requiredItem.setValue(true);
                retList.add(requiredItem);
                SelectItem optional = new SelectItem();
                // When parent field is not required and child is; default level is "Conditionally Required"
                if (typeIn.isRequired() && typeIn.isHasParent() && !typeIn.getParentDatasetFieldType().isRequired()) {
                    optional.setLabel(BundleUtil.getStringFromBundle("dataverse.item.required.conditional"));
                } else {
                    optional.setLabel(BundleUtil.getStringFromBundle("dataverse.item.optional"));                    
                }
                optional.setValue(false);
                retList.add(optional);
        } else {
            SelectItem hidden = new SelectItem();
            hidden.setLabel(BundleUtil.getStringFromBundle("dataverse.item.hidden"));
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
        List<DataverseFieldTypeInputLevel> listDFTIL = new ArrayList<>();
        if (editMode != null && ( editMode.equals(EditMode.INFO) || editMode.equals(EditMode.CREATE))) {

            List<MetadataBlock> selectedBlocks = new ArrayList<>();
            if (dataverse.isMetadataBlockRoot()) {
                dataverse.getMetadataBlocks().clear();
            }

            for (MetadataBlock mdb : this.allMetadataBlocks) {
                if (dataverse.isMetadataBlockRoot() && (mdb.isSelected() || mdb.isRequired())) {
                    selectedBlocks.add(mdb);
                    for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()) {
                        // currently we don't allow input levels for setting an optional field as conditionally required
                        // so we skip looking at parents (which get set automatically with their children)
                        if (!dsft.isHasChildren() && dsft.isRequiredDV()) {
                            boolean addRequiredInputLevels = false;
                            boolean parentAlreadyAdded = false;
                            
                            if (!dsft.isHasParent() && dsft.isInclude()) {
                                addRequiredInputLevels = !dsft.isRequired();
                            }
                            if (dsft.isHasParent() && dsft.getParentDatasetFieldType().isInclude()) {
                                addRequiredInputLevels = !dsft.isRequired() || !dsft.getParentDatasetFieldType().isRequired();
                            }
                            
                            if (addRequiredInputLevels) {
                                listDFTIL.add(new DataverseFieldTypeInputLevel(dsft, dataverse,true, true));
                            
                                //also add the parent as required (if it hasn't been added already)
                                // todo: review needed .equals() methods, then change this to use a Set, in order to simplify code
                                if (dsft.isHasParent()) {
                                    DataverseFieldTypeInputLevel parentToAdd = new DataverseFieldTypeInputLevel(dsft.getParentDatasetFieldType(), dataverse, true, true);
                                    for (DataverseFieldTypeInputLevel dataverseFieldTypeInputLevel : listDFTIL) {
                                        if (dataverseFieldTypeInputLevel.getDatasetFieldType().getId() == parentToAdd.getDatasetFieldType().getId()) {
                                            parentAlreadyAdded = true;
                                            break;
                                        }
                                    }
                                    if (!parentAlreadyAdded) {
                                        // Only add the parent once. There's a UNIQUE (dataverse_id, datasetfieldtype_id)
                                        // constraint on the dataversefieldtypeinputlevel table we need to avoid.
                                        listDFTIL.add(parentToAdd);
                                    }
                                }      
                            }
                        }
                        if ((!dsft.isHasParent() && !dsft.isInclude())
                                || (dsft.isHasParent() && !dsft.getParentDatasetFieldType().isInclude())) {
                            listDFTIL.add(new DataverseFieldTypeInputLevel(dsft, dataverse,false, false));                        
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
                if (dataverse.getOwner() == null || dataverse.getOwner().getId() == null) {
                    dataverse.setOwner(ownerId != null ? dataverseService.find(ownerId) : null);
                }
                create = Boolean.TRUE;
                cmd = new CreateDataverseCommand(dataverse, dvRequestService.getDataverseRequest(), facets.getTarget(), listDFTIL);
            } else {
                JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("dataverse.create.authenticatedUsersOnly"));
                return null;
            }
        } else {
            create = Boolean.FALSE;
            if (editMode != null && editMode.equals(EditMode.FEATURED)) {
                cmd = new UpdateDataverseCommand(dataverse, null, featuredDataverses.getTarget(), dvRequestService.getDataverseRequest(), null);
            } else {
                cmd = new UpdateDataverseCommand(dataverse, facets.getTarget(), null, dvRequestService.getDataverseRequest(), listDFTIL);                
            }
        }

        try {
            dataverse = commandEngine.submit(cmd);
            if (session.getUser() instanceof AuthenticatedUser) {
                if (create) {
                    userNotificationService.sendNotification((AuthenticatedUser) session.getUser(), dataverse.getCreateDate(), Type.CREATEDV, dataverse.getId());
                }
            }
        
            String message;
            if (editMode != null && editMode.equals(EditMode.FEATURED)) {
                message = BundleUtil.getStringFromBundle("dataverse.feature.update");
            } else {
                message = (create) ? BundleUtil.getStringFromBundle("dataverse.create.success", Arrays.asList(settingsWrapper.getGuidesBaseUrl(), settingsWrapper.getGuidesVersion())) : BundleUtil.getStringFromBundle("dataverse.update.success");
            }
            JsfHelper.addSuccessMessage(message);
            
            editMode = null;
            return returnRedirect();            
            

        } /*catch (CommandException ex) {
            logger.log(Level.SEVERE, "Unexpected Exception calling dataverse command", ex);
            String errMsg = create ? BundleUtil.getStringFromBundle("dataverse.create.failure") : BundleUtil.getStringFromBundle("dataverse.update.failure");
            JH.addMessage(FacesMessage.SEVERITY_FATAL, errMsg);
            return null;
        }*/ catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected Exception calling dataverse command", e);
            String errMsg = create ? BundleUtil.getStringFromBundle("dataverse.create.failure") : BundleUtil.getStringFromBundle("dataverse.update.failure");
            
            String failureMessage = e.getMessage() == null 
                        ? errMsg
                        : e.getMessage();
            JsfHelper.addErrorMessage(failureMessage);
            
            return null;
        }
    }
    
    public String cancel() {
        // reset values
        dataverse = dataverseService.find(dataverse.getId());
        ownerId = dataverse.getOwner() != null ? dataverse.getOwner().getId() : null;
        editMode = null;
        return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
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
    
    public String resetToInherit() {

        setInheritMetadataBlockFromParent(true);
        refreshAllMetadataBlocks();
        return null;
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
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataverse.link.select"));
            return "";
        }

        linkingDataverse = dataverseService.find(linkingDataverseId);

        LinkDataverseCommand cmd = new LinkDataverseCommand(dvRequestService.getDataverseRequest(), linkingDataverse, dataverse);
        try {
            commandEngine.submit(cmd);
        } catch (CommandException ex) {
            List<String> args = Arrays.asList(dataverse.getDisplayName(),linkingDataverse.getDisplayName());
            String msg = BundleUtil.getStringFromBundle("dataverse.link.error", args);
            logger.log(Level.SEVERE, "{0} {1}", new Object[]{msg, ex});
            JsfHelper.addErrorMessage(msg);
            return returnRedirect();
        }
        
        JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataverse.linked.success.wait", getSuccessMessageArguments()));
        return returnRedirect();        
    }
    
    private List<String> getSuccessMessageArguments() {
        List<String> arguments = new ArrayList<>();
        arguments.add(StringEscapeUtils.escapeHtml4(dataverse.getDisplayName()));
        String linkString = "<a href=\"/dataverse/" + linkingDataverse.getAlias() + "\">" + StringEscapeUtils.escapeHtml4(linkingDataverse.getDisplayName()) + "</a>";
        arguments.add(linkString);
        return arguments;
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
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataverse.link.select"));
            return "";
        }
        linkingDataverse = dataverseService.find(linkingDataverseId);

        AuthenticatedUser savedSearchCreator = getAuthenticatedUser();
        if (savedSearchCreator == null) {
            String msg = BundleUtil.getStringFromBundle("dataverse.search.user");
            logger.severe(msg);
            JsfHelper.addErrorMessage(msg);
            return returnRedirect();
        }

        SavedSearch savedSearch = new SavedSearch(query, linkingDataverse, savedSearchCreator);
        savedSearch.setSavedSearchFilterQueries(new ArrayList<>());
        for (String filterQuery : filterQueries) {
            /**
             * @todo Why are there null's here anyway? Turn on debug and figure
             * this out.
             */
            if (filterQuery != null && !filterQuery.isEmpty()) {
                SavedSearchFilterQuery ssfq = new SavedSearchFilterQuery(filterQuery,savedSearch);
                savedSearch.getSavedSearchFilterQueries().add(ssfq);
            }
        }
        CreateSavedSearchCommand cmd = new CreateSavedSearchCommand(dvRequestService.getDataverseRequest(), linkingDataverse, savedSearch);
        try {
            commandEngine.submit(cmd);

            List<String> arguments = new ArrayList<>();           
            String linkString = "<a href=\"/dataverse/" + linkingDataverse.getAlias() + "\">" + StringEscapeUtils.escapeHtml4(linkingDataverse.getDisplayName()) + "</a>";
            arguments.add(linkString);
            String successMessageString = BundleUtil.getStringFromBundle("dataverse.saved.search.success", arguments);
            JsfHelper.addSuccessMessage(successMessageString);
            return returnRedirect();
        } catch (CommandException ex) {
            String msg = "There was a problem linking this search to yours: " + ex;
            logger.severe(msg);
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataverse.saved.search.failure") + " " +  ex);
            return returnRedirect();
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
            PublishDataverseCommand cmd = new PublishDataverseCommand(dvRequestService.getDataverseRequest(), dataverse);
            try {
                commandEngine.submit(cmd);
                JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataverse.publish.success"));

            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Unexpected Exception calling  publish dataverse command", ex);
                String failureMessage = ex.getMessage() == null 
                        ? BundleUtil.getStringFromBundle("dataverse.publish.failure")
                        : ex.getMessage();
                JsfHelper.addErrorMessage(failureMessage);

            }
        } else {
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataverse.release.authenticatedUsersOnly"));
        }
        return returnRedirect();

    }

    public String deleteDataverse() {
        DeleteDataverseCommand cmd = new DeleteDataverseCommand(dvRequestService.getDataverseRequest(), dataverse);
        try {
            commandEngine.submit(cmd);
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataverse.delete.success"));
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unexpected Exception calling  delete dataverse command", ex);
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataverse.delete.failure"));
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
        List<MetadataBlock> retList = new ArrayList<>();

        List<MetadataBlock> availableBlocks = new ArrayList<>();
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
                                // in the case of conditionally required (child = true, parent = false)
                                // we set this to false; i.e this is the default "don't override" value
                                child.setRequiredDV(child.isRequired() && dsft.isRequired());
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
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataverse.alias"), BundleUtil.getStringFromBundle("dataverse.alias.taken"));
                context.addMessage(toValidate.getClientId(context), message);
            }
        }
    }
    
    private String returnRedirect(){
        return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";  
    }
    
    private Map<String, Integer> numberOfFacets = new HashMap<>();
    
    public int getNumberOfFacets(String name, int defaultValue) {
        Integer numFacets = numberOfFacets.get(name);
        if (numFacets == null) {
            numberOfFacets.put(name, defaultValue);
            numFacets = defaultValue;
        }
        return numFacets;
    }
    
    public void incrementFacets(String name, int incrementNum) {
        Integer numFacets = numberOfFacets.get(name);
        if (numFacets == null) {
            numFacets = incrementNum;
        }
        numberOfFacets.put(name, numFacets + incrementNum);
    }
    
    private String query;
    private List<String> filterQueries = new ArrayList<>();
    private List<FacetCategory> facetCategoryList = new ArrayList<>();
    private String selectedTypesString;
    private String sortField;
    private SearchIncludeFragment.SortOrder sortOrder;
    private String searchFieldType = SearchFields.TYPE;
    private String searchFieldSubtree = SearchFields.SUBTREE;
    
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getFilterQueries() {
        return filterQueries;
    }

    public void setFilterQueries(List<String> filterQueries) {
        this.filterQueries = filterQueries;
    }

    public List<FacetCategory> getFacetCategoryList() {
        return facetCategoryList;
    }

    public void setFacetCategoryList(List<FacetCategory> facetCategoryList) {
        this.facetCategoryList = facetCategoryList;
    }
    
    private int searchResultsCount = 0;
    
    public int getSearchResultsCount() {
        return searchResultsCount;
    }

    public void setSearchResultsCount(int searchResultsCount) {
        this.searchResultsCount = searchResultsCount;
    }

    public String getSelectedTypesString() {
        return selectedTypesString;
    }

    public void setSelectedTypesString(String selectedTypesString) {
        this.selectedTypesString = selectedTypesString;
    }
    
    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        if (sortOrder != null) {
            return sortOrder.toString();
        } else {
            return null;
        }
    }

    /**
     * Allow only valid values to be set.
     *
     * Rather than passing in a String and converting it to an enum in this
     * method we could write a converter:
     * http://stackoverflow.com/questions/8609378/jsf-2-0-view-parameters-to-pass-objects
     */
    public void setSortOrder(String sortOrderSupplied) {
        if (sortOrderSupplied != null) {
            if (sortOrderSupplied.equals(SearchIncludeFragment.SortOrder.asc.toString())) {
                this.sortOrder = SearchIncludeFragment.SortOrder.asc;
            }
            if (sortOrderSupplied.equals(SearchIncludeFragment.SortOrder.desc.toString())) {
                this.sortOrder = SearchIncludeFragment.SortOrder.desc;
            }
        }
    }
    
    public String getSearchFieldType() {
        return searchFieldType;
    }

    public void setSearchFieldType(String searchFieldType) {
        this.searchFieldType = searchFieldType;
    }

    public String getSearchFieldSubtree() {
        return searchFieldSubtree;
    }

    public void setSearchFieldSubtree(String searchFieldSubtree) {
        this.searchFieldSubtree = searchFieldSubtree;
    }
    
    public List<Dataverse> completeHostDataverseMenuList(String query) {
        if (session.getUser().isAuthenticated()) {
            return dataverseService.filterDataversesForHosting(query, dvRequestService.getDataverseRequest());
        } else {
            return null;
        }
    }
    
    public Set<Entry<String, String>> getStorageDriverOptions() {
    	HashMap<String, String> drivers =new HashMap<String, String>();
    	drivers.putAll(DataAccess.getStorageDriverLabels());
    	//Add an entry for the default (inherited from an ancestor or the system default)
    	drivers.put(getDefaultStorageDriverLabel(), DataAccess.UNDEFINED_STORAGE_DRIVER_IDENTIFIER);
    	return drivers.entrySet();
    }
    
    public String getDefaultStorageDriverLabel() {
    	String storageDriverId = DataAccess.DEFAULT_STORAGE_DRIVER_IDENTIFIER;
    	Dataverse parent = dataverse.getOwner();
    	boolean fromAncestor=false;
    	if(parent != null) {
    		storageDriverId = parent.getEffectiveStorageDriverId();
    		//recurse dataverse chain to root and if any have a storagedriver set, fromAncestor is true
    	    while(parent!=null) {
    	    	if(!parent.getStorageDriverId().equals(DataAccess.UNDEFINED_STORAGE_DRIVER_IDENTIFIER)) {
    	    		fromAncestor=true;
    	    		break;
    	    	}
    	    	parent=parent.getOwner();
    	    }
    	}
   		String label = DataAccess.getStorageDriverLabelFor(storageDriverId);
   		if(fromAncestor) {
   			label = label + " " + BundleUtil.getStringFromBundle("dataverse.inherited");
   		} else {
   			label = label + " " + BundleUtil.getStringFromBundle("dataverse.default");
   		}
   		return label;
    }
    
    public Set<Entry<String, String>> getMetadataLanguages() {
        return settingsWrapper.getMetadataLanguages(this.dataverse).entrySet();
    }
    
    private Set<Entry<String, String>> curationLabelSetOptions = null; 
    
    public Set<Entry<String, String>> getCurationLabelSetOptions() {
        if (curationLabelSetOptions == null) {
            HashMap<String, String> setNames = new HashMap<String, String>();
            Set<String> allowedSetNames = systemConfig.getCurationLabels().keySet();
            if (allowedSetNames.size() > 0) {
                // Add an entry for the default (inherited from an ancestor or the system
                // default)
                String inheritedLabelSet = getCurationLabelSetNameLabel();
                if (!StringUtils.isBlank(inheritedLabelSet)) {
                    setNames.put(inheritedLabelSet, SystemConfig.DEFAULTCURATIONLABELSET);
                }
                // Add an entry for disabled
                setNames.put(BundleUtil.getStringFromBundle("dataverse.curationLabels.disabled"), SystemConfig.CURATIONLABELSDISABLED);

                allowedSetNames.forEach(name -> {
                    String localizedName = DatasetUtil.getLocaleExternalStatus(name) ;
                    setNames.put(localizedName,name);
                });
            }
            curationLabelSetOptions = setNames.entrySet();
        }
        return curationLabelSetOptions;
    }

    public String getCurationLabelSetNameLabel() {
        Dataverse parent = dataverse.getOwner();
        String setName = null;
        boolean fromAncestor = false;
        if (parent != null) {
            setName = parent.getEffectiveCurationLabelSetName();
            // recurse dataverse chain to root and if any have a curation label set name set,
            // fromAncestor is true
            while (parent != null) {
                if (!parent.getCurationLabelSetName().equals(SystemConfig.DEFAULTCURATIONLABELSET)) {
                    fromAncestor = true;
                    break;
                }
                parent = parent.getOwner();
            }
        }
        if (setName != null) {
            if (fromAncestor) {
                setName = setName + " " + BundleUtil.getStringFromBundle("dataverse.inherited");
            } else {
                setName = setName + " " + BundleUtil.getStringFromBundle("dataverse.default");
            }
        }
        return setName;
    }

    public Set<Entry<String, String>> getGuestbookEntryOptions() {
        return settingsWrapper.getGuestbookEntryOptions(this.dataverse).entrySet();
    }

    public Set<Entry<String, String>> getPidProviderOptions() {
        PidProvider defaultPidProvider = pidProviderFactoryBean.getDefaultPidGenerator();
        Set<String> providerIds = PidUtil.getManagedProviderIds();
        Set<Entry<String, String>> options = new HashSet<Entry<String, String>>();
        if (providerIds.size() > 1) {

            String label = null;
            if (this.dataverse.getOwner() != null && this.dataverse.getOwner().getEffectivePidGenerator()!= null) {
                PidProvider inheritedPidProvider = this.dataverse.getOwner().getEffectivePidGenerator();
                label = inheritedPidProvider.getLabel() + " " + BundleUtil.getStringFromBundle("dataverse.inherited") + ": "
                        + inheritedPidProvider.getProtocol() + ":" + inheritedPidProvider.getAuthority()
                        + inheritedPidProvider.getSeparator() + inheritedPidProvider.getShoulder();
            } else {
                label = defaultPidProvider.getLabel() +  " " + BundleUtil.getStringFromBundle("dataverse.default") + ": "
                        + defaultPidProvider.getProtocol() + ":" + defaultPidProvider.getAuthority()
                        + defaultPidProvider.getSeparator() + defaultPidProvider.getShoulder();
            }
            Entry<String, String> option = new AbstractMap.SimpleEntry<String, String>("default", label);
            options.add(option);
        }
        for (String providerId : providerIds) {
            PidProvider pidProvider = PidUtil.getPidProvider(providerId);
            String label = pidProvider.getLabel() + ": " + pidProvider.getProtocol() + ":" + pidProvider.getAuthority()
                    + pidProvider.getSeparator() + pidProvider.getShoulder();
            Entry<String, String> option = new AbstractMap.SimpleEntry<String, String>(providerId, label);
            options.add(option);
        }
        return options;
    }
}
