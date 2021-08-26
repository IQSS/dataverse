package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseFacetServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.bannersandmessages.DataverseUtil;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.error.DataverseError;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.TransferEvent;
import org.primefaces.model.DualListModel;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ViewScoped
@Named("CreateEditDataversePage")
public class CreateEditDataversePage implements Serializable {

    @EJB
    private DataverseDao dataverseDao;

    @EJB
    private DatasetFieldServiceBean datasetFieldService;

    @EJB
    private MetadataBlockService metadataBlockService;

    @EJB
    private DataverseService metadataBlockSaveManager;

    @Inject
    private PermissionsWrapper permissionsWrapper;

    @Inject
    private DataverseFacetServiceBean dataverseFacetService;

    @Inject
    private DataverseSession session;

    @Inject
    private SettingsWrapper settingsWrapper;

    @Inject
    private SystemConfig systemConfig;

    private Long dataverseId;
    private Long ownerId;
    private String dataverseOwnerAlias;

    private Dataverse dataverse;
    private Long facetMetadataBlockId;
    private Set<MetadataBlock> allMetadataBlocks;
    private DualListModel<DatasetFieldType> facets = new DualListModel<>(new ArrayList<>(), new ArrayList<>());

    private DataverseMetaBlockOptions mdbOptions = new DataverseMetaBlockOptions();
    private DataverseMetaBlockOptions parentRootMdbOptions = new DataverseMetaBlockOptions();

    // -------------------- GETTERS --------------------

    public Dataverse getDataverse() {
        return dataverse;
    }

    public Long getDataverseId() {
        return dataverseId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public Long getFacetMetadataBlockId() {
        return facetMetadataBlockId;
    }

    public DualListModel<DatasetFieldType> getFacets() {
        return facets;
    }

    public Set<MetadataBlock> getAllMetadataBlocks() {
        return allMetadataBlocks;
    }

    public DataverseMetaBlockOptions getMdbOptions() {
        return mdbOptions;
    }

    public String getDataverseOwnerAlias() {
        return dataverseOwnerAlias;
    }

    // -------------------- LOGIC --------------------

    public String init() {

        if (dataverseId == null) {
            return setupViewForDataverseCreation();
        }

        return setupViewForDataverseEdit();
    }

    public void validateAlias(FacesContext context, UIComponent toValidate, Object value) {
        if (!StringUtils.isEmpty((String) value)) {
            String alias = (String) value;

            boolean aliasFound = false;
            Dataverse dv = dataverseDao.findByAlias(alias);

            if (dv != null && !dv.getId().equals(dataverse.getId())) {
                aliasFound = true;
            }
            if (aliasFound) {
                ((UIInput) toValidate).setValid(false);
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataverse.alias"), BundleUtil.getStringFromBundle("dataverse.alias.taken"));
                context.addMessage(toValidate.getClientId(context), message);
            }
        }
    }

    public void refreshDatasetFieldTypes(Long mdbId, boolean editable, boolean fieldsVisible) {
        metadataBlockService.refreshDatasetFieldTypes(mdbOptions, mdbId, editable, fieldsVisible);
    }

    
    /**
     * Changes metadata block view options in order to show editable dataset fields for given metadata block.
     */
    public void showEditableDatasetFieldTypes(Long mdbId) {
        metadataBlockService.prepareDatasetFieldsToBeEditable(mdbOptions, mdbId);
    }

    /**
     * Changes metadata block view options in order to show uneditable dataset fields for given metadata block.
     */
    public void showUnEditableDatasetFieldTypes(Long mdbId) {
        metadataBlockService.prepareDatasetFieldsToBeUnEditable(mdbOptions, mdbId);
    }

    /**
     * Changes metadata block view options in order to hide all dataset fields for given metadata block.
     */
    public void hideDatasetFieldTypes(Long mdbId) {
        metadataBlockService.prepareDatasetFieldsToBeHidden(mdbOptions, mdbId);
    }

    /**
     * Resets all view options for metadata blocks, dataset fields and sets metadata blocks to be inherited from parent.
     */
    public String resetToInherit() {
        mdbOptions = parentRootMdbOptions.deepCopy();
        mdbOptions.setInheritMetaBlocksFromParent(true);
        dataverse.setMetadataBlockRoot(false);

        return StringUtils.EMPTY;
    }

    public boolean isInheritFacetFromParent() {
        return !dataverse.isFacetRoot();
    }

    public void toggleFacetRoot() {
        if (!dataverse.isFacetRoot()) {
            initFacets();
        }
    }

    public boolean isUserCanChangeAllowMessageAndBanners() {
        return session.getUser().isSuperuser();
    }

    public String save() {
        List<DataverseFieldTypeInputLevel> dftilForSave =
                metadataBlockService.getDataverseFieldTypeInputLevelsToBeSaved(allMetadataBlocks, mdbOptions, dataverse);

        if (dataverse.getId() == null) {
            return saveNewDataverse(dftilForSave);
        }

        return saveEditedDataverse(dftilForSave);
    }

    public void refresh() {

    }

    public void changeFacetsMetadataBlock() {
        if (facetMetadataBlockId == null) {
            facets.setSource(datasetFieldService.findAllFacetableFieldTypes());
        } else {
            facets.setSource(datasetFieldService.findFacetableFieldTypesByMetadataBlock(facetMetadataBlockId));
        }

        facets.getSource().removeAll(facets.getTarget());
    }

    /**
     * Refreshes dataset fields view options for given dataset field.
     */
    public void refreshDatasetFieldsViewOptions(Long mdbId, long dsftId) {
        metadataBlockService.refreshDatasetFieldsViewOptions(mdbId, dsftId, allMetadataBlocks, mdbOptions);
    }

    /**
     * Makes metadata block selectable/editable since it is no longer inherited from parent dataverse.
     */
    public void makeMetadataBlocksSelectable() {
        mdbOptions.setInheritMetaBlocksFromParent(false);
        dataverse.setMetadataBlockRoot(true);
        allMetadataBlocks = metadataBlockService.prepareMetaBlocksAndDatasetfields(dataverse, mdbOptions);

    }

    /**
     * Returns to dataverse view.
     */
    public String cancel() {
        return returnRedirect();
    }

    public void onFacetTransfer(TransferEvent event) {
        for (Object item : event.getItems()) {
            DatasetFieldType facet = (DatasetFieldType) item;
            if (facetMetadataBlockId != null && !facetMetadataBlockId.equals(facet.getMetadataBlock().getId())) {
                facets.getSource().remove(facet);
            }
        }
    }

    public String returnRedirect() {
        return dataverse.getId() == null ? "/dataverse.xhtml?alias=" + dataverseOwnerAlias + "&faces-redirect=true" :
                "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
    }

    // -------------------- SETTERS --------------------

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public void setDataverseId(Long dataverseId) {
        this.dataverseId = dataverseId;
    }

    public void setFacetMetadataBlockId(Long facetMetadataBlockId) {
        this.facetMetadataBlockId = facetMetadataBlockId;
    }

    public void setAllMetadataBlocks(Set<MetadataBlock> allMetadataBlocks) {
        this.allMetadataBlocks = allMetadataBlocks;
    }

    public void setFacets(DualListModel<DatasetFieldType> facets) {
        this.facets = facets;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setInheritFacetFromParent(boolean inheritFacetFromParent) {
        dataverse.setFacetRoot(!inheritFacetFromParent);
    }

    public void setDataverseOwnerAlias(String dataverseOwnerAlias) {
        this.dataverseOwnerAlias = dataverseOwnerAlias;
    }

    // -------------------- PRIVATE --------------------

    private String saveEditedDataverse(List<DataverseFieldTypeInputLevel> dftilForSave) {
        Either<DataverseError, Dataverse> editResult = metadataBlockSaveManager.saveEditedDataverse(dftilForSave, dataverse, facets);

        if (editResult.isLeft()) {
            JsfHelper.addErrorMessage(editResult.getLeft().getErrorMsg(), "");
            return StringUtils.EMPTY;
        }

        JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.update.success"));
        return returnRedirect();
    }

    private String saveNewDataverse(List<DataverseFieldTypeInputLevel> dftilForSave) {
        Either<DataverseError, Dataverse> saveResult = metadataBlockSaveManager.saveNewDataverse(dftilForSave, dataverse, facets);

        if (saveResult.isLeft()) {
            JsfHelper.addErrorMessage(saveResult.getLeft().getErrorMsg(), "");
            return StringUtils.EMPTY;
        }

        showSuccessMessage();
        return "/dataverse.xhtml?alias=" + saveResult.get().getAlias() + "&faces-redirect=true";
    }

    private void showSuccessMessage() {
        JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.create.success",
                                                                        settingsWrapper.getGuidesBaseUrl(), systemConfig.getGuidesVersion()));
    }

    private String setupViewForDataverseEdit() {
        dataverse = dataverseDao.find(dataverseId);
        if (!permissionsWrapper.canIssueUpdateDataverseCommand(dataverse)) {
            return permissionsWrapper.notAuthorized();
        }

        ownerId = dataverse.getOwner() != null ? dataverse.getOwner().getId() : null;
        setupForGeneralInfoEdit();

        return StringUtils.EMPTY;
    }

    private String setupViewForDataverseCreation() {
        dataverse = new Dataverse();
        dataverse.setOwner(dataverseDao.find(ownerId));

        if (dataverse.getOwner() == null) {
            return permissionsWrapper.notFound();
        } else if (!permissionsWrapper.canIssueCreateDataverseCommand(dataverse.getOwner())) {
            return permissionsWrapper.notAuthorized();
        }

        dataverse.getDataverseContacts().add(new DataverseContact(dataverse, session.getUser().getDisplayInfo().getEmailAddress()));
        dataverse.setAffiliation(session.getUser().getDisplayInfo().getAffiliation());
        setupForGeneralInfoEdit();

        dataverse.setName(DataverseUtil.getSuggestedDataverseNameOnCreate(session.getUser()));

        return StringUtils.EMPTY;
    }

    private void setupForGeneralInfoEdit() {
        initFacets();
        allMetadataBlocks = metadataBlockService.prepareMetaBlocksAndDatasetfields(dataverse, mdbOptions);

        metadataBlockService.prepareMetaBlocksAndDatasetfields(Option.of(dataverse.getOwner()).getOrElse(dataverse),
                                                                        parentRootMdbOptions);
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
}
