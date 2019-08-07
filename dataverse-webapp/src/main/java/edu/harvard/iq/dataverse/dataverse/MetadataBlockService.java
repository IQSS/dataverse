package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevelServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Stateless
public class MetadataBlockService {

    @Inject
    private DataverseServiceBean dataverseService;

    @EJB
    private DataverseFieldTypeInputLevelServiceBean dataverseFieldTypeInputLevelService;

    // -------------------- LOGIC --------------------

    /**
     * Extracts dataverse field type input levels to be saved, if field is optional it is not designed to be saved.
     * Only if it is required or hidden.
     */
    public List<DataverseFieldTypeInputLevel> getDataverseFieldTypeInputLevelsToBeSaved(Collection<MetadataBlock> metadataBlocks,
                                                                                        DataverseMetaBlockOptions mdbOptions,
                                                                                        Dataverse dataverse) {
        List<DataverseFieldTypeInputLevel> listDFTIL = new ArrayList<>();

        if (!mdbOptions.isInheritMetaBlocksFromParent()) {
            dataverse.getRootMetadataBlocks().clear();

            List<MetadataBlock> selectedMetadataBlocks = getSelectedMetadataBlocks(metadataBlocks, mdbOptions);
            dataverse.setMetadataBlocks(selectedMetadataBlocks);
            listDFTIL = getSelectedMetadataFields(selectedMetadataBlocks, dataverse, mdbOptions);
        }

        return listDFTIL;
    }

    /**
     * Prepares metadata blocks and dataset fields for edit/creation of dataverse in order to view all metadata blocks and their fields.
     */
    public Set<MetadataBlock> prepareMetaBlocksAndDatasetfields(Dataverse dataverse, DataverseMetaBlockOptions mdbOptions) {
        if (dataverse.isMetadataBlockRoot()) {
            mdbOptions.setInheritMetaBlocksFromParent(false);
        }

        Set<MetadataBlock> availableBlocks = prepareMetadataBlocks(dataverse);

        buildInitialMetadataBlockOptions(dataverse, mdbOptions, availableBlocks);

        Set<DatasetFieldType> datasetFieldTypes = retriveAllDatasetFieldsForMdb(availableBlocks);

        prepareDatasetFields(mdbOptions, dataverse, datasetFieldTypes);

        return availableBlocks;
    }

    /**
     * Changes metadata block view options in order to show editable dataset fields for given metadata block.
     */
    public MetadataBlockViewOptions prepareDatasetFieldsToBeEditable(DataverseMetaBlockOptions dataverseMetaBlockOptions, Long metadataBlockId) {
        return dataverseMetaBlockOptions.getMdbViewOptions().put(metadataBlockId,
                                                                 MetadataBlockViewOptions.newBuilder()
                                                                         .showDatasetFieldTypes(true)
                                                                         .editableDatasetFieldTypes(true)
                                                                         .selected(true)
                                                                         .build());
    }


    /**
     * Changes metadata block view options in order to show uneditable dataset fields for given metadata block.
     */
    public MetadataBlockViewOptions prepareDatasetFieldsToBeUnEditable(DataverseMetaBlockOptions dataverseMetaBlockOptions, Long metadataBlockId) {
        return dataverseMetaBlockOptions.getMdbViewOptions().put(metadataBlockId,
                                                                 MetadataBlockViewOptions.newBuilder()
                                                                         .showDatasetFieldTypes(true)
                                                                         .editableDatasetFieldTypes(false)
                                                                         .selected(dataverseMetaBlockOptions.isMetaBlockSelected(metadataBlockId))
                                                                         .build());
    }

    /**
     * Changes metadata block view options in order to hide all dataset fields for given metadata block.
     */
    public MetadataBlockViewOptions prepareDatasetFieldsToBeHidden(DataverseMetaBlockOptions dataverseMetaBlockOptions, Long metadataBlockId) {
        return dataverseMetaBlockOptions.getMdbViewOptions().put(metadataBlockId,
                                                                 MetadataBlockViewOptions.newBuilder()
                                                                         .showDatasetFieldTypes(false)
                                                                         .selected(dataverseMetaBlockOptions.isMetaBlockSelected(metadataBlockId))
                                                                         .build());
    }

    /**
     * Refreshes dataset fields view options for given dataset field.
     */
    public void refreshDatasetFieldsViewOptions(Long mdbId, long dsftId, Collection<MetadataBlock> allMetadataBlocks, DataverseMetaBlockOptions mdbOptions) {
        List<DatasetFieldType> childDSFT = new ArrayList<>();

        setViewOptionsForDatasetFields(mdbId, dsftId, allMetadataBlocks, mdbOptions, childDSFT);

        if (!childDSFT.isEmpty()) {
            setViewOptionsForDatasetFieldsChilds(mdbId, allMetadataBlocks, mdbOptions, childDSFT);
        }
    }

    // -------------------- PRIVATE --------------------

    private Set<MetadataBlock> retriveAllDataverseParentsMetaBlocks(Dataverse dataverse) {
        Set<MetadataBlock> metadataBlocks = new HashSet<>();
        for (Dataverse owner : dataverse.getOwners()) {
            metadataBlocks.addAll(owner.getMetadataBlocks());
        }

        return metadataBlocks;
    }

    private void setViewOptionsForDatasetFieldsChilds(Long mdbId, Collection<MetadataBlock> allMetadataBlocks, DataverseMetaBlockOptions mdbOptions, List<DatasetFieldType> childDSFT) {
        for (DatasetFieldType dsftUpdate : childDSFT) {
            for (MetadataBlock mdb : allMetadataBlocks) {
                if (mdb.getId().equals(mdbId)) {
                    for (DatasetFieldType dsftTest : mdb.getDatasetFieldTypes()) {
                        if (dsftTest.getId().equals(dsftUpdate.getId())) {
                            DatasetFieldViewOptions dsftViewOptions = mdbOptions.getDatasetFieldViewOptions().get(dsftTest.getId());
                            dsftViewOptions.setSelectedDatasetFields(setViewOptionForDatasetField(mdbOptions, dsftTest));
                        }
                    }
                    break;
                }
            }
        }
    }

    private void setViewOptionsForDatasetFields(Long mdbId, long dsftId, Collection<MetadataBlock> allMetadataBlocks, DataverseMetaBlockOptions mdbOptions, List<DatasetFieldType> childDSFT) {
        for (MetadataBlock mdb : allMetadataBlocks) {
            if (mdb.getId().equals(mdbId)) {
                for (DatasetFieldType dsftTest : mdb.getDatasetFieldTypes()) {
                    if (dsftTest.getId().equals(dsftId)) {
                        DatasetFieldViewOptions dsftViewOptions = mdbOptions.getDatasetFieldViewOptions().get(dsftTest.getId());

                        dsftViewOptions.setSelectedDatasetFields(setViewOptionForDatasetField(mdbOptions, dsftTest));

                        if ((dsftTest.isHasParent() && !mdbOptions.isDsftIncludedField(dsftTest.getParentDatasetFieldType().getId()))
                                || (!dsftTest.isHasParent() && !dsftViewOptions.isIncluded())) {
                            dsftViewOptions.setRequiredField(false);
                        }
                        if (dsftTest.isHasChildren()) {
                            childDSFT.addAll(dsftTest.getChildDatasetFieldTypes());
                        }
                        break;
                    }
                }
                break;
            }
        }
    }

    private void prepareDatasetFields(DataverseMetaBlockOptions mdbOptions, Dataverse freshDataverse, Set<DatasetFieldType> datasetFieldTypes) {
        for (DatasetFieldType rootDatasetFieldType : datasetFieldTypes) {
            setViewOptionsForDatasetFieldTypes(freshDataverse.getMetadataRootId(), rootDatasetFieldType, mdbOptions);

            if (rootDatasetFieldType.isHasChildren()) {
                for (DatasetFieldType childDatasetFieldType : rootDatasetFieldType.getChildDatasetFieldTypes()) {
                    setViewOptionsForDatasetFieldTypes(freshDataverse.getMetadataRootId(), childDatasetFieldType, mdbOptions);
                }

            }
        }
    }

    private Set<MetadataBlock> prepareMetadataBlocks(Dataverse dataverse) {
        Set<MetadataBlock> availableBlocks = new HashSet<>(dataverseService.findSystemMetadataBlocks());
        Set<MetadataBlock> metadataBlocks = retriveAllDataverseParentsMetaBlocks(dataverse);
        availableBlocks.addAll(metadataBlocks);

        return availableBlocks;
    }

    private Map<Long, MetadataBlockViewOptions> buildInitialMetadataBlockOptions(Dataverse dataverse, DataverseMetaBlockOptions mdbOptions, Set<MetadataBlock> availableBlocks) {
        for (MetadataBlock mdb : availableBlocks) {

            MetadataBlockViewOptions.Builder blockOptionsBuilder = MetadataBlockViewOptions.newBuilder()
                    .selected(false)
                    .showDatasetFieldTypes(false);

            if (dataverse.getOwner() != null) {
                if (dataverse.getOwner().getRootMetadataBlocks().contains(mdb)) {
                    blockOptionsBuilder.selected(true);
                }

                if (dataverse.getOwner().getMetadataBlocks().contains(mdb)) {
                    blockOptionsBuilder.selected(true);
                }
            }

            if (dataverse.getId() != null && dataverse.getMetadataBlocks().contains(mdb)) {
                blockOptionsBuilder.selected(true);
            }

            mdbOptions.getMdbViewOptions().put(mdb.getId(), blockOptionsBuilder.build());
        }

        return mdbOptions.getMdbViewOptions();
    }

    private void setViewOptionsForDatasetFieldTypes(Long dataverseId, DatasetFieldType rootDatasetFieldType, DataverseMetaBlockOptions mdbOptions) {

        DataverseFieldTypeInputLevel dftil = dataverseFieldTypeInputLevelService.findByDataverseIdDatasetFieldTypeId(dataverseId, rootDatasetFieldType.getId());

        if (dftil != null) {
            setDsftilViewOptions(mdbOptions, rootDatasetFieldType.getId(), dftil.isRequired(), dftil.isInclude());
        } else {
            setDsftilViewOptions(mdbOptions, rootDatasetFieldType.getId(), rootDatasetFieldType.isRequired(), true);
        }

        mdbOptions.getDatasetFieldViewOptions()
                .get(rootDatasetFieldType.getId())
                .setSelectedDatasetFields(setViewOptionForDatasetField(mdbOptions, rootDatasetFieldType));
    }

    private List<SelectItem> setViewOptionForDatasetField(DataverseMetaBlockOptions mdbOptions, DatasetFieldType typeIn) {
        List<SelectItem> selectItems = new ArrayList<>();

        if ((typeIn.isHasParent() && mdbOptions.isDsftIncludedField(typeIn.getParentDatasetFieldType().getId())) ||
                (!typeIn.isHasParent() && mdbOptions.isDsftIncludedField(typeIn.getId()))) {
            selectItems.add(generateSelectedItem("dataverse.item.required", true, false));
            selectItems.add(generateSelectedItem("dataverse.item.optional", false, false));
        } else {
            selectItems.add(generateSelectedItem("dataverse.item.hidden", false, true));
        }
        return selectItems;
    }

    private Tuple2<Boolean, Boolean> setDsftilViewOptions(DataverseMetaBlockOptions mdbOptions, long datasetFieldTypeId, boolean requiredField, boolean included) {
        mdbOptions.getDatasetFieldViewOptions().put(datasetFieldTypeId, new DatasetFieldViewOptions(requiredField, included));
        return Tuple.of(requiredField, included);
    }

    private SelectItem generateSelectedItem(String label, boolean selected, boolean disabled) {
        SelectItem requiredItem = new SelectItem();
        requiredItem.setLabel(BundleUtil.getStringFromBundle(label));
        requiredItem.setValue(selected);
        requiredItem.setDisabled(disabled);
        return requiredItem;
    }

    private Set<DatasetFieldType> retriveAllDatasetFieldsForMdb(Collection<MetadataBlock> mdb) {
        Set<DatasetFieldType> allFields = new HashSet<>();

        for (MetadataBlock metadataBlock : mdb) {
            allFields.addAll(metadataBlock.getDatasetFieldTypes());
        }

        return allFields;
    }

    private List<DataverseFieldTypeInputLevel> getSelectedMetadataFields(List<MetadataBlock> selectedMetadataBlocks, Dataverse dataverse, DataverseMetaBlockOptions mdbOptions) {
        List<DataverseFieldTypeInputLevel> listDFTIL = new ArrayList<>();

        for (MetadataBlock selectedMetadataBlock : selectedMetadataBlocks) {
            for (DatasetFieldType dsft : selectedMetadataBlock.getDatasetFieldTypes()) {

                if (isDatasetFieldChildOrParentRequired(dsft, mdbOptions)) {
                    listDFTIL.add(createDataverseFieldTypeInputLevel(dsft, dataverse, true, true));
                }

                if (isDatasetFieldChildOrParentNotIncluded(dsft, mdbOptions)) {
                    listDFTIL.add(createDataverseFieldTypeInputLevel(dsft, dataverse, false, false));
                }
            }

        }

        return listDFTIL;
    }

    private boolean isDatasetFieldChildOrParentNotIncluded(DatasetFieldType dsft, DataverseMetaBlockOptions mdbOptions) {
        return (!dsft.isHasParent() && !mdbOptions.isDsftIncludedField(dsft.getId()))
                || (dsft.isHasParent() && !mdbOptions.isDsftIncludedField(dsft.getParentDatasetFieldType().getId()));
    }

    private boolean isDatasetFieldChildOrParentRequired(DatasetFieldType dsft, DataverseMetaBlockOptions mdbOptions) {
        DatasetFieldViewOptions dsftViewOptions = mdbOptions.getDatasetFieldViewOptions().get(dsft.getId());

        DatasetFieldViewOptions parentDsftViewOptions = null;

        if (dsft.isHasParent()) {
            parentDsftViewOptions = mdbOptions.getDatasetFieldViewOptions().get(dsft.getParentDatasetFieldType().getId());
        }

        boolean isParentIncluded = parentDsftViewOptions != null && parentDsftViewOptions.isIncluded();

        return dsftViewOptions.isRequiredField() && !dsft.isRequired()
                && ((!dsft.isHasParent() && dsftViewOptions.isIncluded())
                || (dsft.isHasParent() && isParentIncluded));
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

    private List<MetadataBlock> getSelectedMetadataBlocks(Collection<MetadataBlock> allMetadataBlocks, DataverseMetaBlockOptions mdbOptions) {
        List<MetadataBlock> selectedBlocks = new ArrayList<>();

        for (MetadataBlock mdb : allMetadataBlocks) {
            if (!mdbOptions.isInheritMetaBlocksFromParent() && (mdbOptions.isMetaBlockSelected(mdb.getId()) || mdb.isCitationMetaBlock())) {
                selectedBlocks.add(mdb);
            }
        }

        return selectedBlocks;
    }

}
