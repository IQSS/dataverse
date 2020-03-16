package edu.harvard.iq.dataverse.dataset.difference;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author skraffmiller
 */
public final class DatasetVersionDifference {

    private DatasetVersion newVersion;
    private DatasetVersion originalVersion;

    private List<List<DatasetFieldDiff>> detailDataByBlock = new ArrayList<>();

    private List<FileMetadata> addedFiles = new ArrayList<>();
    private List<FileMetadata> removedFiles = new ArrayList<>();
    private List<FileMetadataDiff> changedFileMetadata = new ArrayList<>();
    private List<TermsOfUseDiff> changedFileTerms = new ArrayList<>();
    private List<FileMetadataDiff> replacedFiles = new ArrayList<>();


    private List<DatasetFileDifferenceItem> datasetFilesDiffList = new ArrayList<>();
    private List<DatasetFileTermDifferenceItem> datasetFileTermsDiffList = new ArrayList<>();
    private List<DatasetReplaceFileItem> datasetFilesReplacementList = new ArrayList<>();


    private List<DatasetFieldChangeCounts> summaryDataForNote = new ArrayList<>();
    private List<MetadataBlockChangeCounts> blockDataForNote = new ArrayList<>();
    private String fileNote = StringUtils.EMPTY;

    // -------------------- CONSTRUCTORS --------------------

    public DatasetVersionDifference(DatasetVersion newVersion, DatasetVersion originalVersion) {
        this.originalVersion = originalVersion;
        this.newVersion = newVersion;
        //Compare Data

        // metadata field difference
        sortDatasetFields(newVersion.getDatasetFields());
        sortDatasetFields(originalVersion.getDatasetFields());

        Set<DatasetFieldType> originalDatasetFieldTypes = extractDatasetFieldTypes(originalVersion);
        Set<DatasetFieldType> newDatasetFieldTypes = extractDatasetFieldTypes(newVersion);

        for (DatasetFieldType inBothVersionsFieldType : SetUtils.intersection(originalDatasetFieldTypes,
                                                                              newDatasetFieldTypes)) {
            List<DatasetField> originalDatasetFields = extractFieldsWithType(originalVersion.getDatasetFields(),
                                                                             inBothVersionsFieldType);
            List<DatasetField> newDatasetFields = extractFieldsWithType(newVersion.getDatasetFields(),
                                                                        inBothVersionsFieldType);

            updateSameFieldTypesSummary(originalDatasetFields, newDatasetFields);
        }

        for (DatasetFieldType removedFieldType : SetUtils.difference(originalDatasetFieldTypes, newDatasetFieldTypes)) {
            List<DatasetField> originalDatasetField = extractFieldsWithType(originalVersion.getDatasetFields(),
                                                                            removedFieldType);
            if (originalDatasetField.stream().anyMatch(dsf -> !dsf.isEmpty())) {

                int valuesCount = originalDatasetField.stream()
                        .mapToInt(this::extractFieldValuesCount)
                        .sum();

                updateBlockSummary(originalDatasetField.get(0).getDatasetFieldType().getMetadataBlock(),
                                   0,
                                   valuesCount,
                                   0);

                addToSummary(originalDatasetField, Lists.newArrayList(),
                             originalDatasetField.get(0).getDatasetFieldType());
            }
        }

        for (DatasetFieldType addedFieldType : SetUtils.difference(newDatasetFieldTypes, originalDatasetFieldTypes)) {
            List<DatasetField> newDatasetField = extractFieldsWithType(newVersion.getDatasetFields(), addedFieldType);
            if (newDatasetField.stream().anyMatch(dsf -> !dsf.isEmpty())) {

                int valuesCount = newDatasetField.stream()
                        .mapToInt(this::extractFieldValuesCount)
                        .sum();

                updateBlockSummary(newDatasetField.get(0).getDatasetFieldType().getMetadataBlock(), valuesCount, 0, 0);
                addToSummary(Lists.newArrayList(), newDatasetField,
                             newDatasetField.get(0).getDatasetFieldType());
            }
        }

        //Sort within blocks by datasetfieldtype dispaly order then....
        //sort via metadatablock order - citation first...
        for (List<DatasetFieldDiff> blockList : detailDataByBlock) {
            Collections.sort(blockList,
                             Comparator.comparing(x -> x.getFieldType().getDisplayOrder()));
        }
        Collections.sort(detailDataByBlock,
                         Comparator.comparing(x -> x.get(0).getFieldType().getMetadataBlock().getId()));


        // files difference

        for (FileMetadata fmdo : originalVersion.getFileMetadatas()) {
            boolean deleted = true;
            for (FileMetadata fmdn : newVersion.getFileMetadatas()) {
                if (fmdo.getDataFile().equals(fmdn.getDataFile())) {
                    deleted = false;
                    if (!areFilesMetadataEqual(fmdo, fmdn)) {
                        changedFileMetadata.add(new FileMetadataDiff(fmdo, fmdn));
                    }
                    if (!areFileTermsEqual(fmdo.getTermsOfUse(), fmdn.getTermsOfUse())) {
                        changedFileTerms.add(new TermsOfUseDiff(fmdo.getTermsOfUse(), fmdn.getTermsOfUse()));
                    }

                    break;
                }
            }
            if (deleted) {
                removedFiles.add(fmdo);
            }
        }
        for (FileMetadata fmdn : newVersion.getFileMetadatas()) {
            boolean added = true;
            for (FileMetadata fmdo : originalVersion.getFileMetadatas()) {
                if (fmdo.getDataFile().equals(fmdn.getDataFile())) {
                    added = false;
                    break;
                }
            }
            if (added) {
                addedFiles.add(fmdn);
            }
        }
        findReplacedFilesAmongAddedAndRemoved();
        initDatasetFilesDifferencesList();

        fileNote = buildFileNote();
    }

    private void sortDatasetFields(List<DatasetField> fields) {
        fields.sort(Comparator.comparing(DatasetField::getDatasetFieldType)
        .thenComparing(DatasetField::getDisplayOrder));

        fields.forEach(datasetField -> datasetField.getDatasetFieldsChildren()
                .sort(Comparator.comparing(DatasetField::getDatasetFieldType)
                              .thenComparing(DatasetField::getDisplayOrder)));
    }

    // -------------------- GETTERS --------------------

    public DatasetVersion getNewVersion() {
        return newVersion;
    }

    public DatasetVersion getOriginalVersion() {
        return originalVersion;
    }

    /**
     * Returns dataset fields that have been changed
     * between two dataset versions (includes added and
     * removed fields).
     * Changes are grouped by metadata blocks.
     */
    public List<List<DatasetFieldDiff>> getDetailDataByBlock() {
        return detailDataByBlock;
    }

    /**
     * Returns file metadata of files that have
     * been added (that exists only in old version)
     */
    public List<FileMetadata> getAddedFiles() {
        return addedFiles;
    }

    /**
     * Returns file metadata of files that have
     * been removed (that exists only in new version)
     */
    public List<FileMetadata> getRemovedFiles() {
        return removedFiles;
    }

    /**
     * Returns files metadata that have been changed
     * between two dataset versions.
     */
    public List<FileMetadataDiff> getChangedFileMetadata() {
        return changedFileMetadata;
    }

    /**
     * Returns differences between files that have
     * changed between two dataset versions (includes added
     * and removed files).
     * <p>
     * Note that change in terms of use of files
     * is not listed here but in {@link #getDatasetFileTermsDiffList()}
     */
    public List<DatasetFileDifferenceItem> getDatasetFilesDiffList() {
        return datasetFilesDiffList;
    }

    /**
     * Returns files that have different terms of use
     * between two dataset versions
     */
    public List<DatasetFileTermDifferenceItem> getDatasetFileTermsDiffList() {
        return datasetFileTermsDiffList;
    }

    /**
     * Returns differences in files that have been
     * replaced between two dataset versions.
     */
    public List<DatasetReplaceFileItem> getDatasetFilesReplacementList() {
        return datasetFilesReplacementList;
    }

    /**
     * Returns statistical summary of changes between two versions
     * in dataset fields.
     * <p>
     * For example if element on returned list is:
     * <code>[[DatasetFieldType[author]], 3, 5, 1]</code>
     * that means that 3 authors were added; 5 was removed and in 1 author some metadata was changed.
     * <p>
     * Note that only fields with {@link DatasetFieldType#isDisplayOnCreate()} flag
     * are included in returned summary.
     */
    public List<DatasetFieldChangeCounts> getSummaryDataForNote() {
        return summaryDataForNote;
    }

    /**
     * Returns statistical summary of changes between two versions
     * in dataset fields inside metadata block.
     * <p>
     * For example if element on returned list is:
     * <code>[[MetadataBlock[citation]], addedCount=3, removedCount=5, changedCount=1]</code>
     * that means that 3 field values were added to fields within
     * citation block; 5 was removed and 1 field was changed.
     * <p>
     * Note that only fields without {@link DatasetFieldType#isDisplayOnCreate()} flag
     * are included in returned summary.
     */
    public List<MetadataBlockChangeCounts> getBlockDataForNote() {
        return blockDataForNote;
    }

    /**
     * Returns statistical summary of dataset files that changed
     * between two versions in form of formatted and localized string.
     */
    public String getFileNote() {
        return fileNote;
    }

    // -------------------- LOGIC --------------------

    public String getEditSummaryForLog() {

        String retVal = "";

        retVal = System.lineSeparator() + this.newVersion.getTitle() + " (" + this.originalVersion.getDataset().getIdentifier() + ") was updated " + new Date();

        String valueString = "";
        String groupString = "";

        //Metadata differences displayed by Metdata block
        if (!this.detailDataByBlock.isEmpty()) {
            for (List<DatasetFieldDiff> blocks : detailDataByBlock) {
                groupString = System.lineSeparator() + " Metadata Block";

                String blockDisplay = " " + blocks.get(0).getFieldType().getMetadataBlock().getName() + ": " + System.lineSeparator();
                groupString += blockDisplay;
                for (DatasetFieldDiff dsfArray : blocks) {
                    valueString = " Field: ";
                    String title = dsfArray.getFieldType().getName();
                    valueString += title;
                    String oldValue = " Changed From: ";

                    String oldDsfValues = DatasetFieldUtil.joinAllValues(dsfArray.getOldValue());

                    oldValue += oldDsfValues;
                    valueString += oldValue;

                    String newValue = " To: ";

                    String freshDsfValues = DatasetFieldUtil.joinAllValues(dsfArray.getNewValue());

                    newValue += freshDsfValues;
                    valueString += newValue;
                    groupString += valueString + System.lineSeparator();
                }
                retVal += groupString + System.lineSeparator();
            }
        }

        // File Differences
        String fileDiff = System.lineSeparator() + "Files: " + System.lineSeparator();
        if (!this.getDatasetFilesDiffList().isEmpty()) {

            String itemDiff;

            for (DatasetFileDifferenceItem item : this.getDatasetFilesDiffList()) {
                FileMetadataDifferenceItem metadataDiff = item.getDifference();
                itemDiff = "File ID: " + item.getFileSummary().getFileId();

                itemDiff += buildValuesDiffString("Name", metadataDiff.getFileName1(), metadataDiff.getFileName2());
                itemDiff += buildValuesDiffString("Type", metadataDiff.getFileType1(), metadataDiff.getFileType2());
                itemDiff += buildValuesDiffString("Size", metadataDiff.getFileSize1(), metadataDiff.getFileSize2());
                itemDiff += buildValuesDiffString("Tag(s)", metadataDiff.getFileCat1(), metadataDiff.getFileCat2());
                itemDiff += buildValuesDiffString("Description",
                                                  metadataDiff.getFileDesc1(),
                                                  metadataDiff.getFileDesc1());
                itemDiff += buildValuesDiffString("Provenance Description",
                                                  metadataDiff.getFileProvFree1(),
                                                  metadataDiff.getFileProvFree2());

                fileDiff += itemDiff;
            }

            retVal += fileDiff;
        }

        String fileReplaced = System.lineSeparator() + "File(s) Replaced: " + System.lineSeparator();
        if (!this.getDatasetFilesReplacementList().isEmpty()) {
            String itemDiff;
            for (DatasetReplaceFileItem item : this.getDatasetFilesReplacementList()) {
                FileMetadataDifferenceItem metadataDiff = item.getMetadataDifference();
                itemDiff = "";
                itemDiff += buildValuesDiffString("Name", metadataDiff.getFileName1(), metadataDiff.getFileName2());
                itemDiff += buildValuesDiffString("Type", metadataDiff.getFileType1(), metadataDiff.getFileType2());
                itemDiff += buildValuesDiffString("Size", metadataDiff.getFileSize1(), metadataDiff.getFileSize2());
                itemDiff += buildValuesDiffString("Tag(s)", metadataDiff.getFileCat1(), metadataDiff.getFileCat2());
                itemDiff += buildValuesDiffString("Description",
                                                  metadataDiff.getFileDesc1(),
                                                  metadataDiff.getFileDesc2());
                itemDiff += buildValuesDiffString("Provenance Description",
                                                  metadataDiff.getFileProvFree1(),
                                                  metadataDiff.getFileProvFree2());
                fileReplaced += itemDiff;
            }
            retVal += fileReplaced;
        }

        return retVal;
    }

    public boolean isEmpty() {
        return (detailDataByBlock.size() + addedFiles.size() + removedFiles.size() +
                replacedFiles.size() + changedFileMetadata.size() + changedFileTerms.size()) == 0;
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    // -------------------- PRIVATE --------------------

    private Set<DatasetFieldType> extractDatasetFieldTypes(DatasetVersion datasetVersion) {
        Set<DatasetFieldType> datasetFieldTypes = new HashSet<>();
        for (DatasetField dsfo : datasetVersion.getDatasetFields()) {
            datasetFieldTypes.add(dsfo.getDatasetFieldType());
        }
        return datasetFieldTypes;
    }

    private List<DatasetField> extractFieldsWithType(List<DatasetField> datasetFields, DatasetFieldType datasetFieldType) {
        return datasetFields.stream()
                .filter(datasetField -> datasetField.getDatasetFieldType().equals(datasetFieldType))
                .collect(Collectors.toList());
    }

    /**
     *
     */
    private int extractFieldValuesCount(DatasetField datasetField) {
        if (datasetField.getDatasetFieldType().isControlledVocabulary()) {
            return datasetField.getControlledVocabularyValues().size();
        }

        return datasetField.getFieldValue().isDefined() || !datasetField.getDatasetFieldsChildren().isEmpty() ? 1 : 0;
    }


    private void findReplacedFilesAmongAddedAndRemoved() {
        if (addedFiles.isEmpty() || removedFiles.isEmpty()) {
            return;
        }
        for (FileMetadata added : addedFiles) {
            DataFile addedDF = added.getDataFile();
            Long replacedId = addedDF.getPreviousDataFileId();
            for (FileMetadata removed : removedFiles) {
                DataFile test = removed.getDataFile();
                if (test.getId().equals(replacedId)) {
                    replacedFiles.add(new FileMetadataDiff(removed, added));
                }
            }
        }
        replacedFiles.stream().forEach(replaced -> {
            removedFiles.remove(replaced.getOldValue());
            addedFiles.remove(replaced.getNewValue());
        });
    }


    private void addToSummary(DatasetField dsfo, DatasetField dsfn) {
        if (dsfo == null) {
            dsfo = new DatasetField();
            dsfo.setDatasetFieldType(dsfn.getDatasetFieldType());
        }
        if (dsfn == null) {
            dsfn = new DatasetField();
            dsfn.setDatasetFieldType(dsfo.getDatasetFieldType());
        }
        MetadataBlock blockToUpdate = dsfo.getDatasetFieldType().getMetadataBlock();
        List<DatasetFieldDiff> blockListDiffToUpdate = extractOrCreateDiffForBlock(blockToUpdate);

        blockListDiffToUpdate.add(new DatasetFieldDiff(Lists.newArrayList(dsfo),
                                                       Lists.newArrayList(dsfn),
                                                       dsfo.getDatasetFieldType()));
    }

    private void addToSummary(List<DatasetField> dsfo, List<DatasetField> dsfn, DatasetFieldType fieldType) {

        List<DatasetFieldDiff> blockListDiffToUpdate = extractOrCreateDiffForBlock(fieldType.getMetadataBlock());

        blockListDiffToUpdate.add(new DatasetFieldDiff(dsfo, dsfn, fieldType));
    }

    private List<DatasetFieldDiff> extractOrCreateDiffForBlock(MetadataBlock blockToUpdate) {
        for (List<DatasetFieldDiff> blockListDiff : detailDataByBlock) {
            MetadataBlock block = blockListDiff.get(0).getFieldType().getMetadataBlock();
            if (block.equals(blockToUpdate)) {
                return blockListDiff;
            }
        }
        List<DatasetFieldDiff> newBlockListDiff = new ArrayList<>();
        detailDataByBlock.add(newBlockListDiff);
        return newBlockListDiff;
    }

    private void updateBlockSummary(MetadataBlock metadataBlock, int added, int deleted, int changed) {

        for (int i = 0; i < blockDataForNote.size(); ++i) {
            MetadataBlock metadataBlockFromBlockData = blockDataForNote.get(i).getItem();
            if (metadataBlockFromBlockData.equals(metadataBlock)) {
                blockDataForNote.get(i).incrementAdded(added);
                blockDataForNote.get(i).incrementRemoved(deleted);
                blockDataForNote.get(i).incrementChanged(changed);
                return;
            }
        }
        MetadataBlockChangeCounts changeCounts = new MetadataBlockChangeCounts(metadataBlock);
        changeCounts.incrementAdded(added);
        changeCounts.incrementRemoved(deleted);
        changeCounts.incrementChanged(changed);
        blockDataForNote.add(changeCounts);
    }

    private void addToNoteSummary(DatasetFieldType dsft, int added, int deleted, int changed) {
        DatasetFieldChangeCounts counts = new DatasetFieldChangeCounts(dsft);
        counts.incrementAdded(added);
        counts.incrementRemoved(deleted);
        counts.incrementChanged(changed);
        summaryDataForNote.add(counts);
    }

    private boolean areFilesMetadataEqual(FileMetadata fmdo, FileMetadata fmdn) {

        if (!StringUtils.equals(fmdo.getDescription(), fmdn.getDescription())) {
            return false;
        }

        if (!StringUtils.equals(fmdo.getCategoriesByName().toString(), fmdn.getCategoriesByName().toString())) {
            return false;
        }

        if (!StringUtils.equals(fmdo.getLabel(), fmdn.getLabel())) {
            return false;
        }

        if (!StringUtils.equals(fmdo.getProvFreeForm(), fmdn.getProvFreeForm())) {
            return false;
        }

        return true;
    }

    private boolean areFileTermsEqual(FileTermsOfUse termsOriginal, FileTermsOfUse termsNew) {
        if (termsOriginal.getTermsOfUseType() != termsNew.getTermsOfUseType()) {
            return false;
        }
        if (termsOriginal.getTermsOfUseType() == TermsOfUseType.LICENSE_BASED) {
            return termsOriginal.getLicense().getId().equals(termsNew.getLicense().getId());
        }
        if (termsOriginal.getTermsOfUseType() == TermsOfUseType.RESTRICTED) {
            return termsOriginal.getRestrictType() == termsNew.getRestrictType() &&
                    StringUtils.equals(termsOriginal.getRestrictCustomText(), termsNew.getRestrictCustomText());
        }
        return true;
    }

    private void updateSameFieldTypesSummary(List<DatasetField> originalFields, List<DatasetField> newFields) {
        int totalAdded = 0;
        int totalDeleted = 0;
        int totalChanged = 0;

        List<String> originalValues = originalFields.stream()
                .map(this::extractValuesToCompare)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        List<String> newValues = newFields.stream()
                .map(this::extractValuesToCompare)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        DatasetFieldType datasetFieldType = originalFields.get(0).getDatasetFieldType();

        for (int i = 0; i < originalValues.size(); ++i) {
            String originalValue = originalValues.get(i);
            String newValue = (i < newValues.size()) ? newValues.get(i) : StringUtils.EMPTY;

            if (originalValue.isEmpty() && !newValue.isEmpty()) {
                ++totalAdded;
            } else if (!originalValue.isEmpty() && newValue.isEmpty()) {
                ++totalDeleted;
            } else if (!StringUtils.equals(newValue.trim(), originalValue.trim())) {
                ++totalChanged;
            }
        }
        if (newValues.size() > originalValues.size()) {
            totalAdded += (newValues.size() - originalValues.size());
        }

        if ((totalAdded + totalDeleted + totalChanged) > 0) {
            if (datasetFieldType.isDisplayOnCreate()) {
                addToNoteSummary(datasetFieldType, totalAdded, totalDeleted, totalChanged);
            } else {
                updateBlockSummary(datasetFieldType.getMetadataBlock(),
                                   totalAdded,
                                   totalDeleted,
                                   totalChanged);
            }
            addToSummary(originalFields, newFields, datasetFieldType);
        }
    }

    private List<String> extractValuesToCompare(DatasetField datasetField) {

        if (datasetField.getDatasetFieldType().isPrimitive()) {
            return datasetField.getValues();
        }

        return datasetField.getDatasetFieldsChildren().stream()
                .map(DatasetField::getDisplayValue)
                .collect(Collectors.toList());
    }

    private String buildFileNote() {

        List<String> fileChangeStrings = new ArrayList<>();

        if (addedFiles.size() > 0) {
            String addedString = BundleUtil.getStringFromBundle("dataset.version.file.added", addedFiles.size());
            fileChangeStrings.add(addedString);
        }

        if (removedFiles.size() > 0) {
            String removedString = BundleUtil.getStringFromBundle("dataset.version.file.removed", removedFiles.size());
            fileChangeStrings.add(removedString);
        }

        if (replacedFiles.size() > 0) {
            String replacedString = BundleUtil.getStringFromBundle("dataset.version.file.replaced",
                                                                   replacedFiles.size());
            fileChangeStrings.add(replacedString);
        }

        if (changedFileMetadata.size() > 0) {
            String changedFileMetadataString = BundleUtil.getStringFromBundle("dataset.version.file.changedMetadata",
                                                                              changedFileMetadata.size());
            fileChangeStrings.add(changedFileMetadataString);
        }

        if (changedFileTerms.size() > 0) {
            String changedFileTermString = BundleUtil.getStringFromBundle("dataset.version.file.changedTerms",
                                                                          changedFileTerms.size());
            fileChangeStrings.add(changedFileTermString);
        }

        if (fileChangeStrings.isEmpty()) {
            return StringUtils.EMPTY;
        }
        return "(" + StringUtils.join(fileChangeStrings, "; ") + ")";
    }


    private DatasetReplaceFileItem buildDatasetReplaceFileItem(FileMetadata replacedFile, FileMetadata newFile) {
        DataFile replacedDataFile = replacedFile.getDataFile();
        FileSummary replacedSummary = new FileSummary(
                replacedDataFile.getId().toString(),
                replacedDataFile.getChecksumType(),
                replacedDataFile.getChecksumValue());

        DataFile newDataFile = newFile.getDataFile();
        FileSummary newSummary = new FileSummary(
                newDataFile.getId().toString(),
                newDataFile.getChecksumType(),
                newDataFile.getChecksumValue());

        FileMetadataDifferenceItem metadataDiff = new FileMetadataDifferenceItem();
        fillFileMetadataDifference(metadataDiff, replacedFile, newFile);

        DatasetReplaceFileItem fdr = new DatasetReplaceFileItem(replacedSummary, newSummary, metadataDiff);
        return fdr;
    }

    private DatasetFileDifferenceItem buildDatasetFileDifferenceItem(FileMetadata fm1, FileMetadata fm2) {
        DataFile dataFileForDifference = fm1 != null ? fm1.getDataFile() : fm2.getDataFile();
        FileSummary dataFileSummary = new FileSummary(
                dataFileForDifference.getId().toString(),
                dataFileForDifference.getChecksumType(),
                dataFileForDifference.getChecksumValue());

        FileMetadataDifferenceItem metadataDiff = new FileMetadataDifferenceItem();
        fillFileMetadataDifference(metadataDiff, fm1, fm2);

        DatasetFileDifferenceItem fdi = new DatasetFileDifferenceItem(dataFileSummary, metadataDiff);

        return fdi;
    }

    private void initDatasetFilesDifferencesList() {

        replacedFiles.stream()
                .map((replacedPair) -> buildDatasetReplaceFileItem(replacedPair.getOldValue(),
                                                                   replacedPair.getNewValue()))
                .forEach(datasetFilesReplacementList::add);

        for (FileMetadata addedFile : addedFiles) {
            datasetFilesDiffList.add(buildDatasetFileDifferenceItem(null, addedFile));
        }
        for (FileMetadata removedFile : removedFiles) {
            datasetFilesDiffList.add(buildDatasetFileDifferenceItem(removedFile, null));
        }
        for (FileMetadataDiff changedPair : changedFileMetadata) {
            FileMetadata originalMetadata = changedPair.getOldValue();
            FileMetadata newMetadata = changedPair.getNewValue();
            datasetFilesDiffList.add(buildDatasetFileDifferenceItem(originalMetadata, newMetadata));
        }

        for (TermsOfUseDiff changedTermsPair : changedFileTerms) {
            FileTermsOfUse originalTerms = changedTermsPair.getOldValue();
            FileTermsOfUse newTerms = changedTermsPair.getNewValue();
            DataFile dataFile = originalTerms.getFileMetadata().getDataFile();

            datasetFileTermsDiffList.add(new DatasetFileTermDifferenceItem(
                    new FileSummary(dataFile.getId().toString(),
                                    dataFile.getChecksumType(),
                                    dataFile.getChecksumValue()),
                    originalTerms, newTerms));
        }
    }

    private void fillFileMetadataDifference(FileMetadataDifferenceItem fdi, FileMetadata fm1, FileMetadata fm2) {

        if (fm1 == null && fm2 == null) {
            return;
        }

        if (fm2 == null) {
            fdi.setFileName1(fm1.getLabel());
            fdi.setFileType1(fm1.getDataFile().getFriendlyType());
            //fdi.setFileSize1(FileUtil. (new File(fm1.getDataFile().getFileSystemLocation()).length()));

            // deprecated: fdi.setFileCat1(fm1.getCategory());
            fdi.setFileDesc1(fm1.getDescription());
            if (!fm1.getCategoriesByName().isEmpty()) {
                fdi.setFileCat1(fm1.getCategoriesByName().toString());
            }

            fdi.setFileProvFree1(fm1.getProvFreeForm());
            fdi.setFile2Empty(true);

        } else if (fm1 == null) {
            fdi.setFile1Empty(true);

            fdi.setFileName2(fm2.getLabel());
            fdi.setFileType2(fm2.getDataFile().getFriendlyType());

            //fdi.setFileSize2(FileUtil.byteCountToDisplaySize(new File(fm2.getStudyFile().getFileSystemLocation()).length()));
            // deprecated: fdi.setFileCat2(fm2.getCategory());
            fdi.setFileDesc2(fm2.getDescription());
            if (!fm2.getCategoriesByName().isEmpty()) {
                fdi.setFileCat2(fm2.getCategoriesByName().toString());
            }
            fdi.setFileProvFree2(fm2.getProvFreeForm());
        } else {
            // Both are non-null metadata objects.
            // We simply go through the 5 metadata fields, if any are
            // different between the 2 versions, we add them to the
            // difference object:

            String value1;
            String value2;

            // filename:
            value1 = fm1.getLabel();
            value2 = fm2.getLabel();

            value1 = StringUtils.trimToEmpty(value1);
            value2 = StringUtils.trimToEmpty(value2);

            if (!value1.equals(value2)) {

                fdi.setFileName1(value1);
                fdi.setFileName2(value2);
            }

            // NOTE:
            // fileType and fileSize will always be the same
            // for the same studyFile! -- so no need to check for differences in
            // these 2 items.
            // file category:
            value1 = fm1.getCategoriesByName().toString();
            value2 = fm2.getCategoriesByName().toString();

            value1 = StringUtils.trimToEmpty(value1);
            value2 = StringUtils.trimToEmpty(value2);

            if (!value1.equals(value2)) {
                fdi.setFileCat1(value1);
                fdi.setFileCat2(value2);
            }

            // file description:
            value1 = fm1.getDescription();
            value2 = fm2.getDescription();

            value1 = StringUtils.trimToEmpty(value1);
            value2 = StringUtils.trimToEmpty(value2);

            if (!value1.equals(value2)) {

                fdi.setFileDesc1(value1);
                fdi.setFileDesc2(value2);
            }

            // provenance freeform
            value1 = fm1.getProvFreeForm();
            value2 = fm2.getProvFreeForm();

            value1 = StringUtils.trimToEmpty(value1);
            value2 = StringUtils.trimToEmpty(value2);

            if (!value1.equals(value2)) {

                fdi.setFileProvFree1(value1);
                fdi.setFileProvFree2(value2);
            }
        }
    }

    private String buildValuesDiffString(String valueType, String val1, String val2) {
        if (val1 == null && val2 == null) {
            return StringUtils.EMPTY;
        }

        return System.lineSeparator() + " " + valueType + ": " +
                StringUtils.defaultString(val1, "N/A") + " : " + StringUtils.defaultString(val2, "N/A ");
    }

}
