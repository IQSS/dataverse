package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang.StringUtils;

import edu.harvard.iq.dataverse.util.BundleUtil;
import io.vavr.Tuple2;
import io.vavr.Tuple4;

/**
 * @author skraffmiller
 */
public final class DatasetVersionDifference {

    private DatasetVersion newVersion;
    private DatasetVersion originalVersion;
    
    private List<List<Tuple2<DatasetField,DatasetField>>> detailDataByBlock = new ArrayList<>();
    
    private List<FileMetadata> addedFiles = new ArrayList<>();
    private List<FileMetadata> removedFiles = new ArrayList<>();
    private List<Tuple2<FileMetadata, FileMetadata>> changedFileMetadata = new ArrayList<>();
    private List<Tuple2<FileMetadata, FileMetadata>> replacedFiles = new ArrayList<>();
    
    
    private List<DatasetFileDifferenceItem> datasetFilesDiffList;
    private List<DatasetReplaceFileItem> datasetFilesReplacementList;
    
    
    private List<Tuple4<DatasetFieldType, Integer, Integer, Integer>> summaryDataForNote = new ArrayList<>();
    private List<Tuple4<MetadataBlock, Integer, Integer, Integer>> blockDataForNote = new ArrayList<>();
    private String fileNote = StringUtils.EMPTY;
    
    
    // -------------------- CONSTRUCTORS --------------------
    
    public DatasetVersionDifference(DatasetVersion newVersion, DatasetVersion originalVersion) {
        this.originalVersion = originalVersion;
        this.newVersion = newVersion;
        //Compare Data
        
        // metadata field difference
        
        Set<DatasetFieldType> originalDatasetFieldTypes = extractDatasetFieldTypes(originalVersion);
        Set<DatasetFieldType> newDatasetFieldTypes = extractDatasetFieldTypes(newVersion);
        
        for (DatasetFieldType inBothVersionsFieldType: SetUtils.intersection(originalDatasetFieldTypes, newDatasetFieldTypes)) {
            DatasetField originalDatasetField = extractFieldWithType(originalVersion.getDatasetFields(), inBothVersionsFieldType);
            DatasetField newDatasetField = extractFieldWithType(newVersion.getDatasetFields(), inBothVersionsFieldType);
            
            updateSameFieldTypeSummary(originalDatasetField, newDatasetField);
        }
        for (DatasetFieldType removedFieldType: SetUtils.difference(originalDatasetFieldTypes, newDatasetFieldTypes)) {
            DatasetField originalDatasetField = extractFieldWithType(originalVersion.getDatasetFields(), removedFieldType);
            if (!originalDatasetField.isEmpty()) {
                int valuesCount = extractFieldValuesCount(originalDatasetField);
                updateBlockSummary(originalDatasetField.getDatasetFieldType().getMetadataBlock(), 0, valuesCount, 0);
                addToSummary(originalDatasetField, null);
            }
        }
        for (DatasetFieldType addedFieldType: SetUtils.difference(newDatasetFieldTypes, originalDatasetFieldTypes)) {
            DatasetField newDatasetField = extractFieldWithType(newVersion.getDatasetFields(), addedFieldType);
            if (!newDatasetField.isEmpty()) {
                int valuesCount = extractFieldValuesCount(newDatasetField);
                updateBlockSummary(newDatasetField.getDatasetFieldType().getMetadataBlock(), valuesCount, 0, 0);
                addToSummary(null, newDatasetField);
            }
        }
        
        //Sort within blocks by datasetfieldtype dispaly order then....
        //sort via metadatablock order - citation first...
        for (List<Tuple2<DatasetField, DatasetField>> blockList : detailDataByBlock) {
            Collections.sort(blockList, Comparator.comparing(x -> x._1().getDatasetFieldType().getDisplayOrder()));
        }
        Collections.sort(detailDataByBlock, Comparator.comparing(x -> x.get(0)._1().getDatasetFieldType().getMetadataBlock().getId()));
        
        
        // files difference
        
        for (FileMetadata fmdo : originalVersion.getFileMetadatas()) {
            boolean deleted = true;
            for (FileMetadata fmdn : newVersion.getFileMetadatas()) {
                if (fmdo.getDataFile().equals(fmdn.getDataFile())) {
                    deleted = false;
                    if (!areFilesMetadataEqual(fmdo, fmdn)) {
                        changedFileMetadata.add(new Tuple2<FileMetadata, FileMetadata>(fmdo, fmdn));
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
     * First element in tuple points to old dataset field and
     * Second element points to new dataset field.
     */
    public List<List<Tuple2<DatasetField, DatasetField>>> getDetailDataByBlock() {
        return detailDataByBlock;
    }

    /**
     * Returns file metadata of files that have
     * been added (exist only in old version)
     */
    public List<FileMetadata> getAddedFiles() {
        return addedFiles;
    }

    /**
     * Returns file metadata of files that have
     * been removed (exist only in new version)
     */
    public List<FileMetadata> getRemovedFiles() {
        return removedFiles;
    }
    
    /**
     * Returns files metadata that have been changed
     * between two dataset versions.
     * First element in tuple points to old file metadata and
     * second element points to new file metadata.
     */
    public List<Tuple2<FileMetadata, FileMetadata>> getChangedFileMetadata() {
        return changedFileMetadata;
    }
    
    /**
     * Returns differences between files that have
     * changed between two dataset versions (includes added
     * and removed files).
     */
    public List<DatasetFileDifferenceItem> getDatasetFilesDiffList() {
        return datasetFilesDiffList;
    }
    
    /**
     * Returns differences in files that have been
     * replaced between two dataset versions.
     */
    public List<DatasetReplaceFileItem> getDatasetFilesReplacementList() {
        return datasetFilesReplacementList;
    }

    /**
     * Returns statistical summary of dataset fields that changed
     * between two versions.
     * Every item in returned list contains tuple with number of
     * changes on field with type equal to first element in tuple.
     * <p>
     * For example if tuple is:
     * <code>([DatasetFieldType[author]], 3, 5, 1)</code>
     * that means that 3 authors were added; 5 was removed and in 1 author some metadata was changed.
     * <p>
     * Note that only fields with {@link DatasetFieldType#isDisplayOnCreate()} flag
     * are included in returned summary.
     */
    public List<Tuple4<DatasetFieldType, Integer, Integer, Integer>> getSummaryDataForNote() {
        return summaryDataForNote;
    }

    /**
     * Returns statistical summary of dataset fields that changed
     * between two versions.
     * Every item in returned list contains tuple with number
     * of changes in all fields within metadata block equal to
     * first element in tuple.
     * <p>
     * For example if tuple is:
     * <code>([MetadataBlock[citation]], 3, 5, 1)</code>
     * that means that 3 field values were added to fields within
     * citation block; 5 was removed and 1 field was changed.
     * <p>
     * Note that only fields without {@link DatasetFieldType#isDisplayOnCreate()} flag
     * are included in returned summary.
     */
    public List<Tuple4<MetadataBlock, Integer, Integer, Integer>> getBlockDataForNote() {
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
            for (List<Tuple2<DatasetField, DatasetField>> blocks : detailDataByBlock) {
                groupString = System.lineSeparator() + " Metadata Block";
                
                String blockDisplay = " " + blocks.get(0)._1().getDatasetFieldType().getMetadataBlock().getName() + ": " + System.lineSeparator();
                groupString += blockDisplay;
                for (Tuple2<DatasetField, DatasetField> dsfArray : blocks) {
                    valueString = " Field: ";
                    String title = dsfArray._1().getDatasetFieldType().getName();
                    valueString += title;
                    String oldValue = " Changed From: ";

                    if (!dsfArray._1().isEmpty()) {
                        if (dsfArray._1().getDatasetFieldType().isPrimitive()) {
                            oldValue += dsfArray._1().getRawValue();
                        } else {
                            oldValue += dsfArray._1().getCompoundRawValue();
                        }
                    }
                    valueString += oldValue;

                    String newValue = " To: ";
                    if (!dsfArray._2().isEmpty()) {
                        if (dsfArray._2().getDatasetFieldType().isPrimitive()) {
                            newValue += dsfArray._2().getRawValue();
                        } else {
                            newValue += dsfArray._2().getCompoundRawValue();
                        }

                    }
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
                itemDiff = "File ID: " + item.fileId;

                itemDiff += buildValuesDiffString("Name", item.fileName1, item.fileName2);
                itemDiff += buildValuesDiffString("Type", item.fileType1, item.fileType2);
                itemDiff += buildValuesDiffString("Size", item.fileSize1, item.fileSize2);
                itemDiff += buildValuesDiffString("Tag(s)", item.fileCat1, item.fileCat2);
                itemDiff += buildValuesDiffString("Description", item.fileDesc1, item.fileDesc1);
                itemDiff += buildValuesDiffString("Provenance Description", item.fileProvFree1, item.fileProvFree2);

                fileDiff += itemDiff;
            }

            retVal += fileDiff;
        }

        String fileReplaced = System.lineSeparator() + "File(s) Replaced: " + System.lineSeparator();
        if (!this.getDatasetFilesReplacementList().isEmpty()) {
            String itemDiff;
            for (DatasetReplaceFileItem item : this.getDatasetFilesReplacementList()) {
                itemDiff = "";
                itemDiff += buildValuesDiffString("Name", item.fdi.fileName1, item.fdi.fileName2);
                itemDiff += buildValuesDiffString("Type", item.fdi.fileType1, item.fdi.fileType2);
                itemDiff += buildValuesDiffString("Size", item.fdi.fileSize1, item.fdi.fileSize2);
                itemDiff += buildValuesDiffString("Tag(s)", item.fdi.fileCat1, item.fdi.fileCat2);
                itemDiff += buildValuesDiffString("Description", item.fdi.fileDesc1, item.fdi.fileDesc2);
                itemDiff += buildValuesDiffString("Provenance Description", item.fdi.fileProvFree1, item.fdi.fileProvFree2);
                fileReplaced += itemDiff;
            }
            retVal += fileReplaced;
        }

        return retVal;
    }
    
    // -------------------- PRIVATE --------------------
    
    private Set<DatasetFieldType> extractDatasetFieldTypes(DatasetVersion datasetVersion) {
        Set<DatasetFieldType> datasetFieldTypes = new HashSet<DatasetFieldType>();
        for (DatasetField dsfo : datasetVersion.getDatasetFields()) {
            datasetFieldTypes.add(dsfo.getDatasetFieldType());
        }
        return datasetFieldTypes;
    }
    
    private DatasetField extractFieldWithType(List<DatasetField> datasetFields, DatasetFieldType datasetFieldType) {
        for (DatasetField dsf : datasetFields) {
            if (dsf.getDatasetFieldType().equals(datasetFieldType)) {
                return dsf;
            }
        }
        return null;
    }
    
    private int extractFieldValuesCount(DatasetField datasetField) {
        if (datasetField.getDatasetFieldType().isPrimitive()) {
            if (datasetField.getDatasetFieldType().isControlledVocabulary()) {
                return datasetField.getControlledVocabularyValues().size();
            } else {
                return datasetField.getDatasetFieldValues().size();
            }
        }
        return datasetField.getDatasetFieldCompoundValues().size();
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
                    replacedFiles.add(new Tuple2<FileMetadata, FileMetadata>(removed, added));
                }
            }
        }
        replacedFiles.stream().forEach(replaced -> {
            removedFiles.remove(replaced._1());
            addedFiles.remove(replaced._2());
        });
    }


    private void addToList(List<Tuple2<DatasetField, DatasetField>> listIn, DatasetField dsfo, DatasetField dsfn) {
        listIn.add(new Tuple2<>(dsfo, dsfn));
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
        boolean addedToAll = false;
        for (List<Tuple2<DatasetField, DatasetField>> blockList : detailDataByBlock) {
            Tuple2<DatasetField, DatasetField> dsft = blockList.get(0);
            if (dsft._1().getDatasetFieldType().getMetadataBlock().equals(dsfo.getDatasetFieldType().getMetadataBlock())) {
                addToList(blockList, dsfo, dsfn);
                addedToAll = true;
            }
        }
        if (!addedToAll) {
            List<Tuple2<DatasetField, DatasetField>> newList = new ArrayList<>();
            addToList(newList, dsfo, dsfn);
            detailDataByBlock.add(newList);
        }
    }

    private void updateBlockSummary(MetadataBlock metadataBlock, int added, int deleted, int changed) {
        int indexToUpdate = -1;
        
        for (int i=0; i<blockDataForNote.size(); ++i) {
            MetadataBlock metadataBlockFromBlockData = blockDataForNote.get(i)._1();
            if (metadataBlockFromBlockData.equals(metadataBlock)) {
                indexToUpdate = i;
                break;
            }
        }
        
        if (indexToUpdate != -1) {
            Tuple4<MetadataBlock, Integer, Integer, Integer> blockList = blockDataForNote.remove(indexToUpdate);
            blockDataForNote.add(new Tuple4<>(blockList._1(), blockList._2() + added, blockList._3() + deleted, blockList._4() + changed));
            return;
        }
        blockDataForNote.add(new Tuple4<>(metadataBlock, added, deleted, changed));

    }

    private void addToNoteSummary(DatasetFieldType dsft, int added, int deleted, int changed) {
        summaryDataForNote.add(new Tuple4<>(dsft, added, deleted, changed));
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

    private List<String> extractValuesToCompare(DatasetField datasetField) {
        
        if (datasetField.getDatasetFieldType().isPrimitive()) {
            return datasetField.getValues();
        }
        
        List<String> values = new ArrayList<String>();
        
        for (DatasetFieldCompoundValue datasetFieldCompoundValueOriginal : datasetField.getDatasetFieldCompoundValues()) {
            String originalValue = "";
            for (DatasetField dsfo : datasetFieldCompoundValueOriginal.getChildDatasetFields()) {
                if (!dsfo.getDisplayValue().isEmpty()) {
                    originalValue += dsfo.getDisplayValue() + ", ";
                }
            }
            values.add(originalValue);
        }
        return values;
    }
    
    private void updateSameFieldTypeSummary(DatasetField originalField, DatasetField newField) {
        int totalAdded = 0;
        int totalDeleted = 0;
        int totalChanged = 0;

        List<String> originalValues = extractValuesToCompare(originalField);
        List<String> newValues = extractValuesToCompare(newField);
        
        for (int i=0; i<originalValues.size(); ++i) {
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
            if (originalField.getDatasetFieldType().isDisplayOnCreate()) {
                addToNoteSummary(originalField.getDatasetFieldType(), totalAdded, totalDeleted, totalChanged);
            } else {
                updateBlockSummary(originalField.getDatasetFieldType().getMetadataBlock(), totalAdded, totalDeleted, totalChanged);
            }
            addToSummary(originalField, newField);
        }
    }

    private String buildFileNote() {
        
        List<String> fileChangeStrings = new ArrayList<>();

        if (addedFiles.size() > 0) {
            String addedString = BundleUtil.getStringFromBundle("dataset.version.file.added", Arrays.asList(addedFiles.size() + ""));
            fileChangeStrings.add(addedString);
        }

        if (removedFiles.size() > 0) {
            String removedString = BundleUtil.getStringFromBundle("dataset.version.file.removed", Arrays.asList(removedFiles.size() + ""));
            fileChangeStrings.add(removedString);
        }

        if (replacedFiles.size() > 0) {
            String replacedString = BundleUtil.getStringFromBundle("dataset.version.file.replaced", Arrays.asList(replacedFiles.size() + ""));
            fileChangeStrings.add(replacedString);
        }

        if (changedFileMetadata.size() > 0) {
            String changedFileMetadataString = BundleUtil.getStringFromBundle("dataset.version.file.changed", Arrays.asList(changedFileMetadata.size() + ""));
            fileChangeStrings.add(changedFileMetadataString);
        }

        if (fileChangeStrings.isEmpty()) {
            return StringUtils.EMPTY;
        }
        return BundleUtil.getStringFromBundle("dataset.version.file.label") + " (" + StringUtils.join(fileChangeStrings, ';') + ")";
    }

    
    private DatasetReplaceFileItem buildDatasetReplaceFileItem(FileMetadata replacedFile, FileMetadata newFile) {
        DatasetFileDifferenceItem fdi = new DatasetFileDifferenceItem();
        fillFileMetadataDifference(fdi, replacedFile, newFile);
        
        DatasetReplaceFileItem fdr = new DatasetReplaceFileItem();
        fdr.setFdi(fdi);
        fdr.setFile1Id(replacedFile.getDataFile().getId().toString());
        if (newFile.getDataFile().getId() != null) {
            fdr.setFile2Id(newFile.getDataFile().getId().toString());
        }
        fdr.setFile1ChecksumType(replacedFile.getDataFile().getChecksumType());
        fdr.setFile2ChecksumType(newFile.getDataFile().getChecksumType());
        fdr.setFile1ChecksumValue(replacedFile.getDataFile().getChecksumValue());
        fdr.setFile2ChecksumValue(newFile.getDataFile().getChecksumValue());
        return fdr;
    }
    
    private DatasetFileDifferenceItem buildDatasetFileDifferenceItem(FileMetadata fm1, FileMetadata fm2) {
        DatasetFileDifferenceItem fdi = new DatasetFileDifferenceItem();
        fillFileMetadataDifference(fdi, fm1, fm2);
        
        DataFile dataFileForDifference = fm1 != null ? fm1.getDataFile() : fm2.getDataFile();
        
        fdi.setFileId(dataFileForDifference.getId().toString());
        fdi.setFileChecksumType(dataFileForDifference.getChecksumType());
        fdi.setFileChecksumValue(dataFileForDifference.getChecksumValue());
        
        return fdi;
    }
    
    private void initDatasetFilesDifferencesList() {
        datasetFilesDiffList = new ArrayList<>();
        datasetFilesReplacementList = new ArrayList<>();

        
        replacedFiles.stream()
            .map((replacedPair) -> buildDatasetReplaceFileItem(replacedPair._1(), replacedPair._2()))
            .forEach(datasetFilesReplacementList::add);
        
        for (FileMetadata addedFile: addedFiles) {
            datasetFilesDiffList.add(buildDatasetFileDifferenceItem(null, addedFile));
        }
        for (FileMetadata removedFile: removedFiles) {
            datasetFilesDiffList.add(buildDatasetFileDifferenceItem(removedFile, null));
        }
        for (Tuple2<FileMetadata, FileMetadata> changedPair: changedFileMetadata) {
            FileMetadata originalMetadata = changedPair._1();
            FileMetadata newMetadata = changedPair._2();
            datasetFilesDiffList.add(buildDatasetFileDifferenceItem(originalMetadata, newMetadata));
        }
    }
    

    private void fillFileMetadataDifference(DatasetFileDifferenceItem fdi, FileMetadata fm1, FileMetadata fm2) {

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
    
    
    // -------------------- INNER CLASSES --------------------
    
    public class DifferenceSummaryItem {
        private String displayName;
        private int changed;
        private int added;
        private int deleted;
        private int replaced;
        private boolean multiple;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public int getChanged() {
            return changed;
        }

        public void setChanged(int changed) {
            this.changed = changed;
        }

        public int getAdded() {
            return added;
        }

        public void setAdded(int added) {
            this.added = added;
        }

        public int getDeleted() {
            return deleted;
        }

        public void setDeleted(int deleted) {
            this.deleted = deleted;
        }

        public int getReplaced() {
            return replaced;
        }

        public void setReplaced(int replaced) {
            this.replaced = replaced;
        }

        public boolean isMultiple() {
            return multiple;
        }

        public void setMultiple(boolean multiple) {
            this.multiple = multiple;
        }


    }

    public class DatasetReplaceFileItem {

        public DatasetFileDifferenceItem getFdi() {
            return fdi;
        }

        public void setFdi(DatasetFileDifferenceItem fdi) {
            this.fdi = fdi;
        }

        public String getFile1Id() {
            return file1Id;
        }

        public void setFile1Id(String file1Id) {
            this.file1Id = file1Id;
        }

        public String getFile2Id() {
            return file2Id;
        }

        public void setFile2Id(String file2Id) {
            this.file2Id = file2Id;
        }

        public DataFile.ChecksumType getFile1ChecksumType() {
            return file1ChecksumType;
        }

        public void setFile1ChecksumType(DataFile.ChecksumType file1ChecksumType) {
            this.file1ChecksumType = file1ChecksumType;
        }

        public DataFile.ChecksumType getFile2ChecksumType() {
            return file2ChecksumType;
        }

        public void setFile2ChecksumType(DataFile.ChecksumType file2ChecksumType) {
            this.file2ChecksumType = file2ChecksumType;
        }

        public String getFile1ChecksumValue() {
            return file1ChecksumValue;
        }

        public void setFile1ChecksumValue(String file1ChecksumValue) {
            this.file1ChecksumValue = file1ChecksumValue;
        }

        public String getFile2ChecksumValue() {
            return file2ChecksumValue;
        }

        public void setFile2ChecksumValue(String file2ChecksumValue) {
            this.file2ChecksumValue = file2ChecksumValue;
        }

        private DatasetFileDifferenceItem fdi;
        private String file1Id;
        private String file2Id;
        private DataFile.ChecksumType file1ChecksumType;
        private DataFile.ChecksumType file2ChecksumType;
        private String file1ChecksumValue;
        private String file2ChecksumValue;
    }

    public class DatasetFileDifferenceItem {

        public DatasetFileDifferenceItem() {
        }

        private String fileId;
        private DataFile.ChecksumType fileChecksumType;
        private String fileChecksumValue;

        private String fileName1;
        private String fileType1;
        private String fileSize1;
        private String fileCat1;
        private String fileDesc1;
        private String fileProvFree1;

        private String fileName2;
        private String fileType2;
        private String fileSize2;
        private String fileCat2;
        private String fileDesc2;
        private String fileProvFree2;

        public String getFileProvFree1() {
            return fileProvFree1;
        }

        public void setFileProvFree1(String fileProvFree1) {
            this.fileProvFree1 = fileProvFree1;
        }

        public String getFileProvFree2() {
            return fileProvFree2;
        }

        public void setFileProvFree2(String fileProvFree2) {
            this.fileProvFree2 = fileProvFree2;
        }

        private boolean file1Empty = false;
        private boolean file2Empty = false;

        public String getFileId() {
            return fileId;
        }

        public void setFileId(String fid) {
            this.fileId = fid;
        }

        public String getFileName1() {
            return fileName1;
        }

        public void setFileName1(String fn) {
            this.fileName1 = fn;
        }

        public String getFileType1() {
            return fileType1;
        }

        public void setFileType1(String ft) {
            this.fileType1 = ft;
        }

        public String getFileSize1() {
            return fileSize1;
        }

        public void setFileSize1(String fs) {
            this.fileSize1 = fs;
        }

        public String getFileCat1() {
            return fileCat1;
        }

        public void setFileCat1(String fc) {
            this.fileCat1 = fc;
        }

        public String getFileDesc1() {
            return fileDesc1;
        }

        public void setFileDesc1(String fd) {
            this.fileDesc1 = fd;
        }

        public String getFileName2() {
            return fileName2;
        }

        public void setFileName2(String fn) {
            this.fileName2 = fn;
        }

        public String getFileType2() {
            return fileType2;
        }

        public void setFileType2(String ft) {
            this.fileType2 = ft;
        }

        public String getFileSize2() {
            return fileSize2;
        }

        public void setFileSize2(String fs) {
            this.fileSize2 = fs;
        }

        public String getFileCat2() {
            return fileCat2;
        }

        public void setFileCat2(String fc) {
            this.fileCat2 = fc;
        }

        public String getFileDesc2() {
            return fileDesc2;
        }

        public void setFileDesc2(String fd) {
            this.fileDesc2 = fd;
        }

        public boolean isFile1Empty() {
            return file1Empty;
        }

        public boolean isFile2Empty() {
            return file2Empty;
        }

        public void setFile1Empty(boolean state) {
            file1Empty = state;
        }

        public void setFile2Empty(boolean state) {
            file2Empty = state;
        }

        public DataFile.ChecksumType getFileChecksumType() {
            return fileChecksumType;
        }

        public void setFileChecksumType(DataFile.ChecksumType fileChecksumType) {
            this.fileChecksumType = fileChecksumType;
        }

        public String getFileChecksumValue() {
            return fileChecksumValue;
        }

        public void setFileChecksumValue(String fileChecksumValue) {
            this.fileChecksumValue = fileChecksumValue;
        }

    }
    
}
