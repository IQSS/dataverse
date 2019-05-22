package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.UserNotification.Type;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.bannersandmessages.DataverseUtil;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateSavedSearchCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.LinkDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.search.SearchIncludeFragment;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearch;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchFilterQuery;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.primefaces.event.TransferEvent;
import org.primefaces.model.DualListModel;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;


/**
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
        SAVEDSEARCH, LINKDATAVERSE
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
    SystemConfig systemConfig;
    @Inject
    SearchIncludeFragment searchIncludeFragment;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    SettingsWrapper settingsWrapper;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    DataverseLinkingServiceBean linkingService;
    @Inject
    PermissionsWrapper permissionsWrapper;

    private Dataverse dataverse = new Dataverse();
    private EditMode editMode;
    private LinkMode linkMode;

    private Long ownerId;
    private DualListModel<DatasetFieldType> facets = new DualListModel<>(new ArrayList<>(), new ArrayList<>());
    private DualListModel<Dataverse> featuredDataverses = new DualListModel<>(new ArrayList<>(), new ArrayList<>());
    private List<Dataverse> dataversesForLinking;
    private Long linkingDataverseId;
    private List<SelectItem> linkingDVSelectItems;
    private List<ControlledVocabularyValue> dataverseSubjectControlledVocabularyValues;
    private Dataverse linkingDataverse;
    private Long facetMetadataBlockId;
    private boolean openMetadataBlock;
    private boolean editInputLevel;
    private List<Dataverse> carouselFeaturedDataverses = null;
    private List<MetadataBlock> allMetadataBlocks;

    public Dataverse getLinkingDataverse() {
        return linkingDataverse;
    }

    public List<SelectItem> getLinkingDVSelectItems() {
        return linkingDVSelectItems;
    }

    public Long getLinkingDataverseId() {
        return linkingDataverseId;
    }

    public List<Dataverse> getDataversesForLinking() {
        return dataversesForLinking;
    }

    public List<ControlledVocabularyValue> getDataverseSubjectControlledVocabularyValues() {
        return dataverseSubjectControlledVocabularyValues;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public LinkMode getLinkMode() {
        return linkMode;
    }

    public EditMode getEditMode() {
        return editMode;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public Long getFacetMetadataBlockId() {
        return facetMetadataBlockId;
    }

    public boolean isOpenMetadataBlock() {
        return openMetadataBlock;
    }

    public boolean isEditInputLevel() {
        return editInputLevel;
    }

    public boolean isRootDataverse() {
        return dataverse.getOwner() == null;
    }

    public Dataverse getOwner() {
        return (ownerId != null) ? dataverseService.find(ownerId) : null;
    }

    public List<MetadataBlock> getAllMetadataBlocks() {
        return this.allMetadataBlocks;
    }

    public DualListModel<DatasetFieldType> getFacets() {
        return facets;
    }

    public DualListModel<Dataverse> getFeaturedDataverses() {
        return featuredDataverses;
    }

    // -------------------- LOGIC --------------------

    public String init() {
        //System.out.println("_YE_OLDE_QUERY_COUNTER_");  // for debug purposes

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
                return permissionsWrapper.notFound();
            }
            if (!dataverse.isReleased() && !permissionService.on(dataverse).has(Permission.ViewUnpublishedDataverse)) {
                return permissionsWrapper.notAuthorized();
            }

            ownerId = dataverse.getOwner() != null ? dataverse.getOwner().getId() : null;
        } else { // ownerId != null; create mode for a new child dataverse
            editMode = EditMode.CREATE;
            dataverse.setOwner(dataverseService.find(ownerId));
            if (dataverse.getOwner() == null) {
                return permissionsWrapper.notFound();
            } else if (!permissionService.on(dataverse.getOwner()).has(Permission.AddDataverse)) {
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
        if (carouselFeaturedDataverses != null) {
            return carouselFeaturedDataverses;
        }
        carouselFeaturedDataverses = featuredDataverseService.findByDataverseIdQuick(dataverse.getId());

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

    public String save() {
        List<DataverseFieldTypeInputLevel> listDFTIL = new ArrayList<>();
        if (editMode != null && (editMode.equals(EditMode.INFO) || editMode.equals(EditMode.CREATE))) {

            if (dataverse.isMetadataBlockRoot()) {
                dataverse.getMetadataBlocks().clear();

                List<MetadataBlock> selectedMetadataBlocks = getSelectedMetadataBlocks();
                dataverse.setMetadataBlocks(selectedMetadataBlocks);
                listDFTIL = getSelectedMetadataFields(selectedMetadataBlocks);
            }

            if (!dataverse.isFacetRoot()) {
                facets.getTarget().clear();
            }
        }

        Command<Dataverse> cmd;
        //TODO change to Create - for now the page is expecting INFO instead.
        Boolean create;
        if (dataverse.getId() == null) {
            if (session.getUser().isAuthenticated()) {
                dataverse.setOwner(ownerId != null ? dataverseService.find(ownerId) : null);
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
                message = (create) ? BundleUtil.getStringFromBundle("dataverse.create.success", Arrays.asList(settingsWrapper.getGuidesBaseUrl(), systemConfig.getGuidesVersion())) : BundleUtil.getStringFromBundle("dataverse.update.success");
            }
            JsfHelper.addFlashSuccessMessage(message);

            editMode = null;
            return returnRedirect();


        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unexpected Exception calling dataverse command", ex);
            String errMsg = create ? BundleUtil.getStringFromBundle("dataverse.create.failure") : BundleUtil.getStringFromBundle("dataverse.update.failure");
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
        if (editMode.equals(DataversePage.EditMode.CREATE)) {
            refreshAllMetadataBlocks();
            return null;
        } else {
            return save();
        }
    }

    public String saveLinkedDataverse() {

        if (linkingDataverseId == null) {
            JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.link.select"));
            return "";
        }

        AuthenticatedUser savedSearchCreator = getAuthenticatedUser();
        if (savedSearchCreator == null) {
            String msg = BundleUtil.getStringFromBundle("dataverse.link.user");
            logger.severe(msg);
            JsfHelper.addFlashErrorMessage(msg);
            return returnRedirect();
        }

        linkingDataverse = dataverseService.find(linkingDataverseId);

        LinkDataverseCommand cmd = new LinkDataverseCommand(dvRequestService.getDataverseRequest(), linkingDataverse, dataverse);
        //LinkDvObjectCommand cmd = new LinkDvObjectCommand (session.getUser(), linkingDataverse, dataverse);
        try {
            commandEngine.submit(cmd);
        } catch (CommandException ex) {
            List<String> args = Arrays.asList(dataverse.getDisplayName(), linkingDataverse.getDisplayName());
            String msg = BundleUtil.getStringFromBundle("dataverse.link.error", args);
            logger.log(Level.SEVERE, "{0} {1}", new Object[]{msg, ex});
            JsfHelper.addFlashErrorMessage(msg);
            return returnRedirect();
        }

        JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.linked.success.wait", getSuccessMessageArguments()));
        return returnRedirect();
    }

    public void setupLinkingPopup(String popupSetting) {
        if (popupSetting.equals("link")) {
            setLinkMode(LinkMode.LINKDATAVERSE);
        } else {
            setLinkMode(LinkMode.SAVEDSEARCH);
        }
        updateLinkableDataverses();
    }

    public String saveSavedSearch() {
        if (linkingDataverseId == null) {
            JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.link.select"));
            return "";
        }
        linkingDataverse = dataverseService.find(linkingDataverseId);

        AuthenticatedUser savedSearchCreator = getAuthenticatedUser();
        if (savedSearchCreator == null) {
            String msg = BundleUtil.getStringFromBundle("dataverse.search.user");
            logger.severe(msg);
            JsfHelper.addFlashErrorMessage(msg);
            return returnRedirect();
        }

        SavedSearch savedSearch = new SavedSearch(searchIncludeFragment.getQuery(), linkingDataverse, savedSearchCreator);
        savedSearch.setSavedSearchFilterQueries(new ArrayList<>());
        for (String filterQuery : searchIncludeFragment.getFilterQueriesDebug()) {
            if (filterQuery != null && !filterQuery.isEmpty()) {
                SavedSearchFilterQuery ssfq = new SavedSearchFilterQuery(filterQuery, savedSearch);
                savedSearch.getSavedSearchFilterQueries().add(ssfq);
            }
        }
        CreateSavedSearchCommand cmd = new CreateSavedSearchCommand(dvRequestService.getDataverseRequest(), linkingDataverse, savedSearch);
        try {
            commandEngine.submit(cmd);

            List<String> arguments = new ArrayList<>();
            String linkString = "<a href=\"/dataverse/" + linkingDataverse.getAlias() + "\">" + StringEscapeUtils.escapeHtml(linkingDataverse.getDisplayName()) + "</a>";
            arguments.add(linkString);
            String successMessageString = BundleUtil.getStringFromBundle("dataverse.saved.search.success", arguments);
            JsfHelper.addFlashSuccessMessage(successMessageString);
            return returnRedirect();
        } catch (CommandException ex) {
            String msg = "There was a problem linking this search to yours: " + ex;
            logger.severe(msg);
            JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.saved.search.failure") + " " + ex);
            return returnRedirect();
        }
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
    }

    public String releaseDataverse() {
        if (session.getUser() instanceof AuthenticatedUser) {
            PublishDataverseCommand cmd = new PublishDataverseCommand(dvRequestService.getDataverseRequest(), dataverse);
            try {
                commandEngine.submit(cmd);
                JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.publish.success"));

            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Unexpected Exception calling  publish dataverse command", ex);
                JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.publish.failure"));

            }
        } else {
            JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.publish.not.authorized"));
        }
        return returnRedirect();

    }

    public String deleteDataverse() {
        DeleteDataverseCommand cmd = new DeleteDataverseCommand(dvRequestService.getDataverseRequest(), dataverse);
        try {
            commandEngine.submit(cmd);
            JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.delete.success"));
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unexpected Exception calling  delete dataverse command", ex);
            JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.delete.failure"));
        }
        return "/dataverse.xhtml?alias=" + dataverse.getOwner().getAlias() + "&faces-redirect=true";
    }

    public Boolean isEmptyDataverse() {
        return !dataverseService.hasData(dataverse);
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

    public boolean isUserCanChangeAllowMessageAndBanners() {
        return session.getUser().isSuperuser();
    }

    public boolean isUserAdminForCurrentDataverse() {
        return permissionService.isUserAdminForDataverse(session.getUser(), this.dataverse);
    }

    public boolean isInheritMetadataBlockFromParent() {
        return !dataverse.isMetadataBlockRoot();
    }

    public boolean isInheritFacetFromParent() {
        return !dataverse.isFacetRoot();
    }

    // -------------------- PRIVATE --------------------

    private void updateDataverseSubjectSelectItems() {
        DatasetFieldType subjectDatasetField = datasetFieldService.findByName(DatasetFieldConstant.subject);
        setDataverseSubjectControlledVocabularyValues(controlledVocabularyValueServiceBean.findByDatasetFieldTypeId(subjectDatasetField.getId()));
    }

    private void setupForGeneralInfoEdit() {
        updateDataverseSubjectSelectItems();
        initFacets();
        refreshAllMetadataBlocks();
    }

    private List<DataverseFieldTypeInputLevel> getSelectedMetadataFields(List<MetadataBlock> selectedMetadataBlocks) {
        List<DataverseFieldTypeInputLevel> listDFTIL = new ArrayList<>();

        for (MetadataBlock selectedMetadataBlock : selectedMetadataBlocks) {
            for (DatasetFieldType dsft : selectedMetadataBlock.getDatasetFieldTypes()) {

                if (isDatasetFieldChildOrParentRequired(dsft)) {
                    listDFTIL.add(createDataverseFieldTypeInputLevel(dsft, dataverse, true, true));
                }

                if (isDatasetFieldChildOrParentNotIncluded(dsft)) {
                    listDFTIL.add(createDataverseFieldTypeInputLevel(dsft, dataverse, false, false));
                }
            }

        }

        return listDFTIL;
    }

    private List<MetadataBlock> getSelectedMetadataBlocks() {
        List<MetadataBlock> selectedBlocks = new ArrayList<>();

        for (MetadataBlock mdb : this.allMetadataBlocks) {
            if (dataverse.isMetadataBlockRoot() && (mdb.isSelected() || mdb.isRequired())) {
                selectedBlocks.add(mdb);
            }
        }

        return selectedBlocks;
    }

    private DataverseFieldTypeInputLevel createDataverseFieldTypeInputLevel(DatasetFieldType dsft,
                                                                            Dataverse dataverse,
                                                                            boolean isRequired,
                                                                            boolean isIncluded) {
        DataverseFieldTypeInputLevel dftil = new DataverseFieldTypeInputLevel();
        dftil.setDatasetFieldType(dsft);
        dftil.setDataverse(dataverse);
        dftil.setRequired(isRequired);
        dftil.setInclude(isIncluded);
        return dftil;
    }

    private boolean isDatasetFieldChildOrParentNotIncluded(DatasetFieldType dsft) {
        return (!dsft.isHasParent() && !dsft.isInclude())
                || (dsft.isHasParent() && !dsft.getParentDatasetFieldType().isInclude());
    }

    private boolean isDatasetFieldChildOrParentRequired(DatasetFieldType dsft) {
        return dsft.isRequiredDV() && !dsft.isRequired()
                && ((!dsft.isHasParent() && dsft.isInclude())
                || (dsft.isHasParent() && dsft.getParentDatasetFieldType().isInclude()));
    }

    private List<String> getSuccessMessageArguments() {
        List<String> arguments = new ArrayList<>();
        arguments.add(StringEscapeUtils.escapeHtml(dataverse.getDisplayName()));
        String linkString = "<a href=\"/dataverse/" + linkingDataverse.getAlias() + "\">" + StringEscapeUtils.escapeHtml(linkingDataverse.getDisplayName()) + "</a>";
        arguments.add(linkString);
        return arguments;
    }

    private AuthenticatedUser getAuthenticatedUser() {
        User user = session.getUser();
        if (user.isAuthenticated()) {
            return (AuthenticatedUser) user;
        } else {
            return null;
        }
    }

    private void refreshAllMetadataBlocks() {
        Long dataverseIdForInputLevel = dataverse.getId();
        List<MetadataBlock> retList = new ArrayList<>();

        //Add System level blocks
        List<MetadataBlock> availableBlocks = new ArrayList<>(dataverseService.findSystemMetadataBlocks());

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

    private void initFeaturedDataverses() {
        List<Dataverse> featuredSource = new ArrayList<>();
        List<Dataverse> featuredTarget = new ArrayList<>();
        featuredSource.addAll(dataverseService.findAllPublishedByOwnerId(dataverse.getId()));
        featuredSource.addAll(linkingService.findLinkingDataverses(dataverse.getId()));
        List<DataverseFeaturedDataverse> featuredList = featuredDataverseService.findByDataverseId(dataverse.getId());
        for (DataverseFeaturedDataverse dfd : featuredList) {
            Dataverse fd = dfd.getFeaturedDataverse();
            featuredTarget.add(fd);
            featuredSource.remove(fd);
        }
        featuredDataverses = new DualListModel<>(featuredSource, featuredTarget);

    }

    private void updateLinkableDataverses() {
        dataversesForLinking = new ArrayList<>();
        linkingDVSelectItems = new ArrayList<>();

        //Since only a super user function add all dvs
        dataversesForLinking = dataverseService.findAll();// permissionService.getDataversesUserHasPermissionOn(session.getUser(), Permission.PublishDataverse);


        //for linking - make sure the link hasn't occurred and its not int the tree
        if (this.linkMode.equals(LinkMode.LINKDATAVERSE)) {

            // remove this and it's parent tree
            dataversesForLinking.remove(dataverse);
            Dataverse testDV = dataverse;
            while (testDV.getOwner() != null) {
                dataversesForLinking.remove(testDV.getOwner());
                testDV = testDV.getOwner();
            }

            for (Dataverse removeLinked : linkingService.findLinkingDataverses(dataverse.getId())) {
                dataversesForLinking.remove(removeLinked);
            }
        }


        for (Dataverse selectDV : dataversesForLinking) {
            linkingDVSelectItems.add(new SelectItem(selectDV.getId(), selectDV.getDisplayName()));
        }

        if (dataversesForLinking.size() == 1 && dataversesForLinking.get(0) != null) {
            linkingDataverse = dataversesForLinking.get(0);
            linkingDataverseId = linkingDataverse.getId();
        }
    }

    private void initFacets() {
        List<DatasetFieldType> facetsTarget = new ArrayList<>();
        List<DatasetFieldType> facetsSource = new ArrayList<>(datasetFieldService.findAllFacetableFieldTypes());
        List<DataverseFacet> facetsList = dataverseFacetService.findByDataverseId(dataverse.getFacetRootId());
        for (DataverseFacet dvFacet : facetsList) {
            DatasetFieldType dsfType = dvFacet.getDatasetFieldType();
            facetsTarget.add(dsfType);
            facetsSource.remove(dsfType);
        }
        facets = new DualListModel<>(facetsSource, facetsTarget);
        facetMetadataBlockId = null;
    }

    private List<SelectItem> resetSelectItems(DatasetFieldType typeIn) {
        List<SelectItem> retList = new ArrayList<>();
        if ((typeIn.isHasParent() && typeIn.getParentDatasetFieldType().isInclude()) || (!typeIn.isHasParent() && typeIn.isInclude())) {
            SelectItem requiredItem = new SelectItem();
            requiredItem.setLabel(BundleUtil.getStringFromBundle("dataverse.item.required"));
            requiredItem.setValue(true);
            retList.add(requiredItem);
            SelectItem optional = new SelectItem();
            optional.setLabel(BundleUtil.getStringFromBundle("dataverse.item.optional"));
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

    private String returnRedirect() {
        return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
    }

    // -------------------- SETTERS --------------------

    public void setLinkingDataverse(Dataverse linkingDataverse) {
        this.linkingDataverse = linkingDataverse;
    }

    public void setLinkingDVSelectItems(List<SelectItem> linkingDVSelectItems) {
        this.linkingDVSelectItems = linkingDVSelectItems;
    }

    public void setLinkingDataverseId(Long linkingDataverseId) {
        this.linkingDataverseId = linkingDataverseId;
    }

    public void setDataversesForLinking(List<Dataverse> dataversesForLinking) {

        this.dataversesForLinking = dataversesForLinking;
    }

    public void setDataverseSubjectControlledVocabularyValues(List<ControlledVocabularyValue> dataverseSubjectControlledVocabularyValues) {
        this.dataverseSubjectControlledVocabularyValues = dataverseSubjectControlledVocabularyValues;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public void setEditMode(EditMode editMode) {
        this.editMode = editMode;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setFacetMetadataBlockId(Long facetMetadataBlockId) {
        this.facetMetadataBlockId = facetMetadataBlockId;
    }

    public void setLinkMode(LinkMode linkMode) {
        this.linkMode = linkMode;
    }

    public void setOpenMetadataBlock(boolean openMetadataBlock) {
        this.openMetadataBlock = openMetadataBlock;
    }

    public void setEditInputLevel(boolean editInputLevel) {
        this.editInputLevel = editInputLevel;
    }

    public void setInheritMetadataBlockFromParent(boolean inheritMetadataBlockFromParent) {
        dataverse.setMetadataBlockRoot(!inheritMetadataBlockFromParent);
    }


    public void setFeaturedDataverses(DualListModel<Dataverse> featuredDataverses) {
        this.featuredDataverses = featuredDataverses;
    }

    public void setFacets(DualListModel<DatasetFieldType> facets) {
        this.facets = facets;
    }


    public void setInheritFacetFromParent(boolean inheritFacetFromParent) {
        dataverse.setFacetRoot(!inheritFacetFromParent);
    }

    public void setAllMetadataBlocks(List<MetadataBlock> inBlocks) {
        this.allMetadataBlocks = inBlocks;
    }
}
