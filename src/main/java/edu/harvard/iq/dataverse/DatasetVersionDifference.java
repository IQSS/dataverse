package edu.harvard.iq.dataverse;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author skraffmiller
 */
public class DatasetVersionDifference {

    private DatasetVersion newVersion;
    private DatasetVersion originalVersion;
    private List<List> detailDataByBlock = new ArrayList();
    private List<FileMetadata> addedFiles = new ArrayList();
    private List<FileMetadata> removedFiles = new ArrayList();
    private List<FileMetadata> changedFileMetadata = new ArrayList();
    private List<DatasetField> addedData = new ArrayList();
    private List<DatasetField> removedData = new ArrayList();
    private List<DatasetField> changedData = new ArrayList();
    private List<DatasetField> addedSummaryData = new ArrayList();
    private List<DatasetField> removedSummaryData = new ArrayList();
    private List<DatasetField> changedSummaryData = new ArrayList();
    private List<Object[]> summaryDataForNote = new ArrayList();
    private List<Object[]> blockDataForNote = new ArrayList();

    public DatasetVersionDifference(DatasetVersion newVersion, DatasetVersion originalVersion) {

        setOriginalVersion(originalVersion);
        setNewVersion(newVersion);
        //Compare Data
        for (DatasetField dsfo : originalVersion.getDatasetFields()) {
            boolean deleted = true;
            for (DatasetField dsfn : newVersion.getDatasetFields()) {
                if (dsfo.getDatasetFieldType().equals(dsfn.getDatasetFieldType())) {
                    deleted = false;
                    if (dsfo.getDatasetFieldType().isPrimitive()) {
                        if (!dsfo.getDatasetFieldType().getFieldType().equals("email") && !compareValuesPrimitive(dsfo, dsfn)) {
                            if (!dsfo.getDatasetFieldType().isDisplayOnCreate()) {
                                changedData.add(dsfo);
                                changedData.add(dsfn);
                                compareValuesCount(dsfo, dsfn, false);
                            } else {
                                changedSummaryData.add(dsfo);
                                changedSummaryData.add(dsfn);
                                compareValuesCount(dsfo, dsfn, false);
                            }
                            addToSummary(dsfo, dsfn);
                        }
                    } else {
                        if (!compareValuesCompound(dsfo, dsfn)) {
                            if (!dsfo.getDatasetFieldType().isDisplayOnCreate()) {
                                changedData.add(dsfo);
                                changedData.add(dsfn);
                                compareValuesCount(dsfo, dsfn, true);
                            } else {
                                changedSummaryData.add(dsfo);
                                changedSummaryData.add(dsfn);
                                compareValuesCount(dsfo, dsfn, true);
                            }
                            addToSummary(dsfo, dsfn);
                        }
                    }
                    break; //if found go to next dataset field
                }
            }
            if (deleted && !dsfo.isEmpty()) {
                if (dsfo.getDatasetFieldType().isDisplayOnCreate()) {
                    removedSummaryData.add(dsfo);
                } else {
                    removedData.add(dsfo);
                }
                addToSummary(dsfo, null);
            }
        }
        for (DatasetField dsfn : newVersion.getDatasetFields()) {
            boolean added = true;
            for (DatasetField dsfo : originalVersion.getDatasetFields()) {
                if (dsfo.getDatasetFieldType().equals(dsfn.getDatasetFieldType())) {
                    added = false;
                    break;
                }
            }
            if (added && !dsfn.isEmpty()) {
                if (dsfn.getDatasetFieldType().isDisplayOnCreate()) {
                    addedSummaryData.add(dsfn);
                } else {
                    addedData.add(dsfn);
                }
                addToSummary(null, dsfn);
            }
        }

        for (FileMetadata fmdo : originalVersion.getFileMetadatas()) {
            boolean deleted = true;
            for (FileMetadata fmdn : newVersion.getFileMetadatas()) {
                if (fmdo.getDataFile().equals(fmdn.getDataFile())) {
                    deleted = false;
                    if (!compareFileMetadatas(fmdo, fmdn)) {
                        changedFileMetadata.add(fmdo);
                        changedFileMetadata.add(fmdn);
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
    }

    private void addToList(List listIn, DatasetField dsfo, DatasetField dsfn) {
        DatasetField[] dsfArray;
        dsfArray = new DatasetField[2];
        dsfArray[0] = dsfo;
        dsfArray[1] = dsfn;
        listIn.add(dsfArray);
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
        for (List blockList : detailDataByBlock) {
            DatasetField dsft[] = (DatasetField[]) blockList.get(0);
            if (dsft[0].getDatasetFieldType().getMetadataBlock().equals(dsfo.getDatasetFieldType().getMetadataBlock())) {
                addToList(blockList, dsfo, dsfn);
                addedToAll = true;
            }
        }
        if (!addedToAll) {
            List<DatasetField[]> newList = new ArrayList<>();
            addToList(newList, dsfo, dsfn);
            detailDataByBlock.add(newList);
        }
    }

    private void updateBlockSummary(DatasetField dsf, int added, int deleted, int changed) {

        boolean addedToAll = false;
        for (Object[] blockList : blockDataForNote) {
            DatasetField dsft = (DatasetField) blockList[0];
            if (dsft.getDatasetFieldType().getMetadataBlock().equals(dsf.getDatasetFieldType().getMetadataBlock())) {
                blockList[1] = (Integer) blockList[1] + added;
                blockList[2] = (Integer) blockList[2] + deleted;
                blockList[3] = (Integer) blockList[3] + changed;
                addedToAll = true;
            }
        }
        if (!addedToAll) {
            Object[] newArray = new Object[4];
            newArray[0] = dsf;
            newArray[1] = added;
            newArray[2] = deleted;
            newArray[3] = changed;
            blockDataForNote.add(newArray);
        }

    }

    private void addToNoteSummary(DatasetField dsfo, int added, int deleted, int changed) {
        Object[] noteArray = new Object[4];
        noteArray[0] = dsfo;
        noteArray[1] = added;
        noteArray[2] = deleted;
        noteArray[3] = changed;
        summaryDataForNote.add(noteArray);
    }

    private boolean compareFileMetadatas(FileMetadata fmdo, FileMetadata fmdn) {

        if (!(fmdo.getDescription().equals(fmdn.getDescription()))) {
            return false;
        }
        if (!(fmdo.getCategory().equals(fmdn.getCategory()))) {
            return false;
        }
        if (!(fmdo.getLabel().equals(fmdn.getLabel()))) {
            return false;
        }

        return true;
    }

    private boolean compareValuesPrimitive(DatasetField originalField, DatasetField newField) {
        if (originalField.isEmpty() && newField.isEmpty()) {
            return true;
        }

        String originalValue = originalField.getDisplayValue();
        String newValue = newField.getDisplayValue();
        return originalValue.equals(newValue);

    }

    private boolean compareValuesCompound(DatasetField originalField, DatasetField newField) {
        String originalValue = "";
        String newValue = "";
        if (originalField.isEmpty() && newField.isEmpty()) {
            return true;
        }
        for (DatasetFieldCompoundValue datasetFieldCompoundValueNew : newField.getDatasetFieldCompoundValues()) {
            for (DatasetField dsfn : datasetFieldCompoundValueNew.getChildDatasetFields()) {
                newValue += dsfn.getDisplayValue() + ", ";
            }
            newValue += "; ";
        }
        for (DatasetFieldCompoundValue datasetFieldCompoundValueOriginal : originalField.getDatasetFieldCompoundValues()) {
            for (DatasetField dsfn : datasetFieldCompoundValueOriginal.getChildDatasetFields()) {
                originalValue += dsfn.getDisplayValue() + ", ";
            }
            originalValue += "; ";
        }
        return originalValue.equals(newValue);
    }

    private void compareValuesCount(DatasetField originalField, DatasetField newField, boolean compound) {
        String originalValue = "";
        String newValue = "";
        int countOriginal = 0;
        int countNew = 0;
        int totalAdded = 0;
        int totalDeleted = 0;
        int totalChanged = 0;
        int loopIndex = 0;

        countNew = newField.getDatasetFieldCompoundValues().size();

        if (compound) {
            for (DatasetFieldCompoundValue datasetFieldCompoundValueOriginal : originalField.getDatasetFieldCompoundValues()) {
                if (newField.getDatasetFieldCompoundValues().size() >= loopIndex + 1) {
                    for (DatasetField dsfo : datasetFieldCompoundValueOriginal.getChildDatasetFields()) {
                        if (!dsfo.getDisplayValue().isEmpty()) {
                            originalValue += dsfo.getDisplayValue() + ", ";
                        }
                    }
                    for (DatasetField dsfn : newField.getDatasetFieldCompoundValues().get(loopIndex).getChildDatasetFields()) {
                        if (!dsfn.getDisplayValue().isEmpty()) {
                            newValue += dsfn.getDisplayValue() + ", ";
                        }
                    }
                    if (!newValue.isEmpty() && !originalValue.isEmpty() && !originalValue.equals(newValue)) {
                        totalChanged++;
                    }
                }
                loopIndex++;
                if (!originalValue.isEmpty()) {
                    countOriginal++;
                }

            }
        } else {
            int index = 0;
            for (String valString : originalField.getValues()) {
                if (valString != null && !valString.isEmpty()) {
                    countOriginal++;
                }
            }
            for (String valString : newField.getValues()) {
                if (valString != null && !valString.isEmpty()) {
                    countNew++;
                }
            }
            String nString = "";
            for (String oString : originalField.getValues()) {
                if (newField.getValues().size() >= (index + 1)) {
                    nString = newField.getValues().get(index);
                }
                if (nString != null && oString != null && !oString.equals(nString)) {
                    totalChanged++;
                }
            }
        }

        if (countNew > countOriginal) {
            totalAdded = countNew - countOriginal;
        }

        if (countOriginal > countNew) {
            totalDeleted = countOriginal - countNew;
        }
        
        if (originalField.getDatasetFieldType().isDisplayOnCreate()){
            addToNoteSummary(originalField, totalAdded, totalDeleted, totalChanged);
        } else {
            updateBlockSummary(originalField, totalAdded, totalDeleted, totalChanged);
        }               
    }

    public List<String> getFileNotes() {
        String retString;
        List retList = new ArrayList();
        if (addedFiles.size() > 0) {
            retString = "Files Added: " + addedFiles.size() + "; ";
            retList.add(retString);
        }
        if (removedFiles.size() > 0) {
            retString = "Files Deleted: " + removedFiles.size() + "; ";
            retList.add(retString);
        }
        if (changedFileMetadata.size() > 0) {
            retString = "Number of File Metadata changed:" + changedFileMetadata.size() / 2;
            retList.add(retString);
        }
        return retList;
    }

    public String getNote() {
        String retString = "";
        if (addedSummaryData.size() > 0) {
            retString += "Summary data added";
            int count = 0;
            for (DatasetField dsf : addedSummaryData) {
                if (count == 0) {
                    retString += ":";
                } else {
                    retString += "; ";
                }
                retString += " " + dsf.getDatasetFieldType().getDisplayName();
                count++;
            }
        }
        if (removedSummaryData.size() > 0) {
            retString += "Summary data deleted";
            int count = 0;
            for (DatasetField dsf : removedSummaryData) {
                if (count == 0) {
                    retString += ":";
                } else {
                    retString += "; ";
                }
                retString += " " + dsf.getDatasetFieldType().getDisplayName();
                count++;
            }
        }
        if (changedSummaryData.size() > 0) {
            retString += "Summary data changed:";
            for (Iterator<DatasetField> iter = changedSummaryData.iterator(); iter.hasNext();) {
                DatasetField dsf = iter.next();
                retString += " " + dsf.getDatasetFieldType().getDisplayName() + "; ";
                DatasetField dsfn = iter.next();
            }
        }

        if (addedData.size() > 0) {
            retString += addedData.size() + " Additional Citation Metadata Added; ";
        }
        if (removedData.size() > 0) {
            retString += "Number of Additional Data deleted" + removedData.size() + "; ";
        }
        if (changedData.size() > 0) {
            retString += changedData.size() / 2 + " Additional Citation Metadata Changed; ";
        }
        if (addedFiles.size() > 0) {
            retString += "Files Added: " + addedFiles.size() + "; ";
        }
        if (removedFiles.size() > 0) {
            retString += "Files Deleted: " + removedFiles.size() + "; ";
        }
        if (changedFileMetadata.size() > 0) {
            retString += "Number of File Metadata changed" + changedFileMetadata.size() / 2 + "; ";
        }

        return retString;
    }

    public List<List> getDetailDataByBlock() {
        return detailDataByBlock;
    }

    public void setDetailDataByBlock(List<List> detailDataByBlock) {
        this.detailDataByBlock = detailDataByBlock;
    }

    public List<FileMetadata> getAddedFiles() {
        return addedFiles;
    }

    public void setAddedFiles(List<FileMetadata> addedFiles) {
        this.addedFiles = addedFiles;
    }

    public List<FileMetadata> getRemovedFiles() {
        return removedFiles;
    }

    public void setRemovedFiles(List<FileMetadata> removedFiles) {
        this.removedFiles = removedFiles;
    }

    public List<DatasetField> getAddedData() {
        return addedData;
    }

    public void setAddedData(List<DatasetField> addedData) {
        this.addedData = addedData;
    }

    public List<DatasetField> getRemovedData() {
        return removedData;
    }

    public void setRemovedData(List<DatasetField> removedData) {
        this.removedData = removedData;
    }

    public List<DatasetField> getChangedData() {
        return changedData;
    }

    public void setChangedData(List<DatasetField> changedData) {
        this.changedData = changedData;
    }

    public List<DatasetField> getAddedSummaryData() {
        return addedSummaryData;
    }

    public void setAddedSummaryData(List<DatasetField> addedSummaryData) {
        this.addedSummaryData = addedSummaryData;
    }

    public List<DatasetField> getRemovedSummaryData() {
        return removedSummaryData;
    }

    public void setRemovedSummaryData(List<DatasetField> removedSummaryData) {
        this.removedSummaryData = removedSummaryData;
    }

    public List<DatasetField> getChangedSummaryData() {
        return changedSummaryData;
    }

    public void setChangedSummaryData(List<DatasetField> changedSummaryData) {
        this.changedSummaryData = changedSummaryData;
    }

    public DatasetVersion getNewVersion() {
        return newVersion;
    }

    public void setNewVersion(DatasetVersion newVersion) {
        this.newVersion = newVersion;
    }

    public DatasetVersion getOriginalVersion() {
        return originalVersion;
    }

    public void setOriginalVersion(DatasetVersion originalVersion) {
        this.originalVersion = originalVersion;
    }

    public List<FileMetadata> getChangedFileMetadata() {
        return changedFileMetadata;
    }

    public void setChangedFileMetadata(List<FileMetadata> changedFileMetadata) {
        this.changedFileMetadata = changedFileMetadata;
    }
    
    
    public List<Object[]> getSummaryDataForNote() {
        return summaryDataForNote;
    }

    public List<Object[]> getBlockDataForNote() {
        return blockDataForNote;
    }

    public void setSummaryDataForNote(List<Object[]> summaryDataForNote) {
        this.summaryDataForNote = summaryDataForNote;
    }

    public void setBlockDataForNote(List<Object[]> blockDataForNote) {
        this.blockDataForNote = blockDataForNote;
    }

}
