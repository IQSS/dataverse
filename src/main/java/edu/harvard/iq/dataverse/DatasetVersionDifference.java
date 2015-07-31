package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.StringUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import java.util.ResourceBundle;

/**
 *
 * @author skraffmiller
 */
public class DatasetVersionDifference {

    private DatasetVersion newVersion;
    private DatasetVersion originalVersion;
    private List<List<DatasetField[]>> detailDataByBlock = new ArrayList<>();
    private List<datasetFileDifferenceItem> datasetFilesDiffList;
    private List<FileMetadata> addedFiles = new ArrayList<>();
    private List<FileMetadata> removedFiles = new ArrayList<>();
    private List<FileMetadata> changedFileMetadata = new ArrayList<>();
    private List<String[]> changedTermsAccess = new ArrayList<>();
    private List<SummaryData> summaryDataForNote = new ArrayList<>();
    private List<BlockData> blockDataForNote = new ArrayList<>();
    String noFileDifferencesFoundLabel = "";

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
                        if (!dsfo.getDatasetFieldType().getFieldType().equals("email")) {
                            compareValues(dsfo, dsfn, false);
                        }
                    } else {
                        compareValues(dsfo, dsfn, true);
                    }
                    break; //if found go to next dataset field
                }
            }
            if (deleted && !dsfo.isEmpty()) {
                updateBlockSummary(dsfo, 0, dsfo.getDatasetFieldValues().size(), 0);
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
                if (dsfn.getDatasetFieldType().isPrimitive()){
                   updateBlockSummary(dsfn, dsfn.getDatasetFieldValues().size(), 0, 0);
                } else {
                   updateBlockSummary(dsfn, dsfn.getDatasetFieldCompoundValues().size(), 0, 0);
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
        initDatasetFilesDifferencesList();

        //Sort within blocks by datasetfieldtype dispaly order then....
        //sort via metadatablock order - citation first...
        for (List<DatasetField[]> blockList : detailDataByBlock) {
            Collections.sort(blockList, new Comparator<DatasetField[]>() {
                public int compare(DatasetField[] l1, DatasetField[] l2) {
                    DatasetField dsfa = l1[0];  //(DatasetField[]) l1.get(0);
                    DatasetField dsfb = l2[0];
                    int a = dsfa.getDatasetFieldType().getDisplayOrder();
                    int b = dsfb.getDatasetFieldType().getDisplayOrder();
                    return Integer.valueOf(a).compareTo(Integer.valueOf(b));
                }
            });
        }
        Collections.sort(detailDataByBlock, new Comparator<List<DatasetField[]>>() {
            public int compare(List<DatasetField[]> l1, List<DatasetField[]> l2) {
                DatasetField dsfa[] = l1.get(0);
                DatasetField dsfb[] = l2.get(0);
                int a = dsfa[0].getDatasetFieldType().getMetadataBlock().getId().intValue();
                int b = dsfb[0].getDatasetFieldType().getMetadataBlock().getId().intValue();
                return Integer.valueOf(a).compareTo(Integer.valueOf(b));
            }
        });
        getTermsDifferences();
    }
    

    
    private void getTermsDifferences() {

        changedTermsAccess = new ArrayList<>();
        if (!StringUtil.nullToEmpty(newVersion.getTermsOfUse()).equals(StringUtil.nullToEmpty(originalVersion.getTermsOfUse()))) {
            String diffLabel = ResourceBundle.getBundle("Bundle").getString("file.dataFilesTab.terms.list.termsOfUse.header");
            changedTermsAccess = addToTermsChangedList(changedTermsAccess, diffLabel, StringUtil.nullToEmpty(originalVersion.getTermsOfUse()), StringUtil.nullToEmpty(newVersion.getTermsOfUse()));
        }
        if (!StringUtil.nullToEmpty(newVersion.getConfidentialityDeclaration()).equals(StringUtil.nullToEmpty(originalVersion.getConfidentialityDeclaration()))) {
            String diffLabel = ResourceBundle.getBundle("Bundle").getString("file.dataFilesTab.terms.list.termsOfUse.addInfo.declaration");
            changedTermsAccess = addToTermsChangedList(changedTermsAccess, diffLabel, StringUtil.nullToEmpty(originalVersion.getConfidentialityDeclaration()), StringUtil.nullToEmpty(newVersion.getConfidentialityDeclaration()));
        }
        if (!StringUtil.nullToEmpty(newVersion.getSpecialPermissions()).equals(StringUtil.nullToEmpty(originalVersion.getSpecialPermissions()))) {
            String diffLabel = ResourceBundle.getBundle("Bundle").getString("file.dataFilesTab.terms.list.termsOfUse.addInfo.permissions");
            changedTermsAccess = addToTermsChangedList(changedTermsAccess, diffLabel, StringUtil.nullToEmpty(originalVersion.getSpecialPermissions()), StringUtil.nullToEmpty(newVersion.getSpecialPermissions()));
        }
        if (!StringUtil.nullToEmpty(newVersion.getRestrictions()).equals(StringUtil.nullToEmpty(originalVersion.getRestrictions()))) {
            String diffLabel = ResourceBundle.getBundle("Bundle").getString("file.dataFilesTab.terms.list.termsOfUse.addInfo.restrictions");
            changedTermsAccess = addToTermsChangedList(changedTermsAccess, diffLabel, StringUtil.nullToEmpty(originalVersion.getRestrictions()), StringUtil.nullToEmpty(newVersion.getRestrictions()));
            
        }
        if (!StringUtil.nullToEmpty(newVersion.getCitationRequirements()).equals(StringUtil.nullToEmpty(originalVersion.getCitationRequirements()))) {
            String diffLabel = ResourceBundle.getBundle("Bundle").getString("file.dataFilesTab.terms.list.termsOfUse.addInfo.citationRequirements");
            changedTermsAccess = addToTermsChangedList(changedTermsAccess, diffLabel, StringUtil.nullToEmpty(originalVersion.getCitationRequirements()), StringUtil.nullToEmpty(newVersion.getCitationRequirements()));
        }
        if (!StringUtil.nullToEmpty(newVersion.getDepositorRequirements()).equals(StringUtil.nullToEmpty(originalVersion.getDepositorRequirements()))) {
            String diffLabel = ResourceBundle.getBundle("Bundle").getString("file.dataFilesTab.terms.list.termsOfUse.addInfo.depositorRequirements");
            changedTermsAccess = addToTermsChangedList(changedTermsAccess, diffLabel, StringUtil.nullToEmpty(originalVersion.getDepositorRequirements()), StringUtil.nullToEmpty(newVersion.getDepositorRequirements()));
        }
        if (!StringUtil.nullToEmpty(newVersion.getConditions()).equals(StringUtil.nullToEmpty(originalVersion.getConditions()))) {
            String diffLabel = ResourceBundle.getBundle("Bundle").getString("file.dataFilesTab.terms.list.termsOfUse.addInfo.conditions");
            changedTermsAccess = addToTermsChangedList(changedTermsAccess, diffLabel, StringUtil.nullToEmpty(originalVersion.getConditions()), StringUtil.nullToEmpty(newVersion.getConditions()));
        }
        if (!StringUtil.nullToEmpty(newVersion.getDisclaimer()).equals(StringUtil.nullToEmpty(originalVersion.getDisclaimer()))) {
            String diffLabel = ResourceBundle.getBundle("Bundle").getString("file.dataFilesTab.terms.list.termsOfUse.addInfo.disclaimer");
            changedTermsAccess = addToTermsChangedList(changedTermsAccess, diffLabel, StringUtil.nullToEmpty(originalVersion.getDisclaimer()), StringUtil.nullToEmpty(newVersion.getDisclaimer()));
        }

        if (!StringUtil.nullToEmpty(newVersion.getTermsOfAccess()).equals(StringUtil.nullToEmpty(originalVersion.getTermsOfAccess()))) {
            String diffLabel = ResourceBundle.getBundle("Bundle").getString("file.dataFilesTab.terms.list.termsOfAccess.termsOfsAccess");
            changedTermsAccess = addToTermsChangedList(changedTermsAccess, diffLabel, StringUtil.nullToEmpty(originalVersion.getTermsOfAccess()), StringUtil.nullToEmpty(newVersion.getTermsOfAccess()));
        }
        if (!StringUtil.nullToEmpty(newVersion.getDataAccessPlace()).equals(StringUtil.nullToEmpty(originalVersion.getDataAccessPlace()))) {
            String diffLabel = ResourceBundle.getBundle("Bundle").getString("file.dataFilesTab.terms.list.termsOfAccess.addInfo.dataAccessPlace");
            changedTermsAccess = addToTermsChangedList(changedTermsAccess, diffLabel, StringUtil.nullToEmpty(originalVersion.getDataAccessPlace()), StringUtil.nullToEmpty(newVersion.getDataAccessPlace()));
        }
        if (!StringUtil.nullToEmpty(newVersion.getOriginalArchive()).equals(StringUtil.nullToEmpty(originalVersion.getOriginalArchive()))) {
            String diffLabel = ResourceBundle.getBundle("Bundle").getString("file.dataFilesTab.terms.list.termsOfAccess.addInfo.originalArchive");
            changedTermsAccess = addToTermsChangedList(changedTermsAccess, diffLabel, StringUtil.nullToEmpty(originalVersion.getOriginalArchive()), StringUtil.nullToEmpty(newVersion.getOriginalArchive()));
        }
        if (!StringUtil.nullToEmpty(newVersion.getAvailabilityStatus()).equals(StringUtil.nullToEmpty(originalVersion.getAvailabilityStatus()))) {
            String diffLabel = ResourceBundle.getBundle("Bundle").getString("file.dataFilesTab.terms.list.termsOfAccess.addInfo.availabilityStatus");
            changedTermsAccess = addToTermsChangedList(changedTermsAccess, diffLabel, StringUtil.nullToEmpty(originalVersion.getAvailabilityStatus()), StringUtil.nullToEmpty(newVersion.getAvailabilityStatus()));
        }
        if (!StringUtil.nullToEmpty(newVersion.getContactForAccess()).equals(StringUtil.nullToEmpty(originalVersion.getContactForAccess()))) {
            String diffLabel = ResourceBundle.getBundle("Bundle").getString("file.dataFilesTab.terms.list.termsOfAccess.addInfo.contactForAccess");
            changedTermsAccess = addToTermsChangedList(changedTermsAccess, diffLabel, StringUtil.nullToEmpty(originalVersion.getContactForAccess()), StringUtil.nullToEmpty(newVersion.getContactForAccess()));
        }
        if (!StringUtil.nullToEmpty(newVersion.getSizeOfCollection()).equals(StringUtil.nullToEmpty(originalVersion.getSizeOfCollection()))) {
            String diffLabel = ResourceBundle.getBundle("Bundle").getString("file.dataFilesTab.terms.list.termsOfAccess.addInfo.sizeOfCollection");
            changedTermsAccess = addToTermsChangedList(changedTermsAccess, diffLabel, StringUtil.nullToEmpty(originalVersion.getSizeOfCollection()), StringUtil.nullToEmpty(newVersion.getSizeOfCollection()));
        }
        if (!StringUtil.nullToEmpty(newVersion.getStudyCompletion()).equals(StringUtil.nullToEmpty(originalVersion.getStudyCompletion()))) {
            String diffLabel = ResourceBundle.getBundle("Bundle").getString("file.dataFilesTab.terms.list.termsOfAccess.addInfo.studyCompletion");
            changedTermsAccess = addToTermsChangedList(changedTermsAccess, diffLabel, StringUtil.nullToEmpty(originalVersion.getStudyCompletion()), StringUtil.nullToEmpty(newVersion.getStudyCompletion()));
        }
        

    }

    private List<String[]> addToTermsChangedList(List<String[]> listIn, String label, String origVal, String newVal) {
        String[] diffArray;
        diffArray = new String[3];
        diffArray[0] = label;
        diffArray[1] = origVal;
        diffArray[2] = newVal;
        listIn.add(diffArray);
        return listIn;
    }


    private void addToList(List<DatasetField[]> listIn, DatasetField dsfo, DatasetField dsfn) {
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
        for (List<DatasetField[]> blockList : detailDataByBlock) {
            DatasetField dsft[] = blockList.get(0);
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
        for (BlockData blockList : blockDataForNote) {
            DatasetField dsft = blockList.getDatasetField();
            if (dsft.getDatasetFieldType().getMetadataBlock().equals(dsf.getDatasetFieldType().getMetadataBlock())) {
                blockList.increaseAdded(added);
                blockList.increaseDeleted(deleted);
                blockList.increaseChanged(changed);
                addedToAll = true;
            }
        }
        if (!addedToAll) {
            BlockData bn = new BlockData(dsf, added, deleted, changed);
            blockDataForNote.add(bn);
        }

    }

    private void addToNoteSummary(DatasetField dsfo, int added, int deleted, int changed) {
        SummaryData note = new SummaryData(dsfo, added, deleted, changed);
        summaryDataForNote.add(note);
    }

    private boolean compareFileMetadatas(FileMetadata fmdo, FileMetadata fmdn) {

        if (!StringUtils.equals(fmdo.getDescription(), fmdn.getDescription())) {
            return false;
        }
        
        if (!StringUtils.equals(fmdo.getCategoriesByName().toString(), fmdn.getCategoriesByName().toString())) {
            return false;
        }
        
        if (!StringUtils.equals(fmdo.getLabel(), fmdn.getLabel())) {
            return false;
        }
        
        return fmdo.isRestricted() == fmdn.isRestricted();
    }
    
    private void compareValues(DatasetField originalField, DatasetField newField, boolean compound) {
        String originalValue = "";
        String newValue = "";
        int countOriginal = 0;
        int countNew = 0;
        int totalAdded = 0;
        int totalDeleted = 0;
        int totalChanged = 0;
        int loopIndex = 0;

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
                    if (originalValue.isEmpty() && !newValue.isEmpty()) {
                        totalAdded++;
                    } else if (!newValue.isEmpty() && !originalValue.trim().equals(newValue.trim())) {
                        totalChanged++;
                    }
                }
                loopIndex++;
            }
            countNew = newField.getDatasetFieldCompoundValues().size();
            countOriginal = originalField.getDatasetFieldCompoundValues().size();
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
            originalValue = originalField.getDisplayValue();
            newValue = newField.getDisplayValue();
            for (String oString : originalField.getValues()) {
                if (newField.getValues().size() >= (index + 1)) {
                    nString = newField.getValues().get(index);
                }
                if (nString != null && oString != null && !oString.trim().equals(nString.trim())) {
                    totalChanged++;
                }
            }
            if (originalValue.equalsIgnoreCase(newValue)) {
                totalChanged = 0;
            }
        }
        
        if (countNew > countOriginal) {
            totalAdded = countNew - countOriginal;
        }

        if (countOriginal > countNew) {
            totalDeleted = countOriginal - countNew;
        }
        if ((totalAdded + totalDeleted + totalChanged) > 0) {
            if (originalField.getDatasetFieldType().isDisplayOnCreate()) {
                addToNoteSummary(originalField, totalAdded, totalDeleted, totalChanged);
                addToSummary(originalField, newField);
            } else {
                updateBlockSummary(originalField, totalAdded, totalDeleted, totalChanged);
                addToSummary(originalField, newField);
            }
        }
    }

    public String getFileNote() {
        String retString = "";

        if (addedFiles.size() > 0) {
            retString = "Files (Added: " + addedFiles.size();
        }

        if (removedFiles.size() > 0) {
            if (retString.isEmpty()) {
                retString = "Files (Removed: " + removedFiles.size();
            } else {
                retString += "; Removed: " + removedFiles.size();
            }
        }

        if (changedFileMetadata.size() > 0) {
            if (retString.isEmpty()) {
                retString = "Files (Changed File Metadata: " + changedFileMetadata.size() / 2;
            } else {
                retString += "; Changed File Metadata: " + changedFileMetadata.size() / 2;
            }
        }

        if (!retString.isEmpty()) {
            retString += ")";
        }

        return retString;
    }

    public List<List<DatasetField[]>> getDetailDataByBlock() {
        return detailDataByBlock;
    }

    public void setDetailDataByBlock(List<List<DatasetField[]>> detailDataByBlock) {
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

    public List<SummaryData> getSummaryDataForNote() {
        return summaryDataForNote;
    }

    public List<BlockData> getBlockDataForNote() {
        return blockDataForNote;
    }

    public void setSummaryDataForNote(List<SummaryData> summaryDataForNote) {
        this.summaryDataForNote = summaryDataForNote;
    }

    public void setBlockDataForNote(List<BlockData> blockDataForNote) {
        this.blockDataForNote = blockDataForNote;
    }
    
    
    public List<String[]> getChangedTermsAccess() {
        return changedTermsAccess;
    }

    public void setChangedTermsAccess(List<String[]> changedTermsAccess) {
        this.changedTermsAccess = changedTermsAccess;
    }

    private void initDatasetFilesDifferencesList() {
        datasetFilesDiffList = new ArrayList<datasetFileDifferenceItem>();

        // Study Files themselves are version-less;
        // In other words, 2 different versions can have different sets of
        // study files, but the files themselves don't have versions.
        // So in order to find the differences between the 2 sets of study
        // files in 2 versions we can just go through the lists of the
        // files and compare the ids. If both versions have the file with
        // the same file id, it is the same file.
        // UPDATE: in addition to the above, even when the 2 versions share the
        // same study file, the file metadatas ARE version-specific, so some of
        // the fields there (filename, etc.) may be different. If this is the
        // case, we want to display these differences as well.
        if (originalVersion.getFileMetadatas().size() == 0 && newVersion.getFileMetadatas().size() == 0) {
            noFileDifferencesFoundLabel = "No data files in either version of the study";
            return;
        }

        int i = 0;
        int j = 0;

        FileMetadata fm1;
        FileMetadata fm2;
        
           Collections.sort(originalVersion.getFileMetadatas(), new Comparator<FileMetadata>() {
                public int compare(FileMetadata l1, FileMetadata l2) {
                    FileMetadata fm1 = l1;  //(DatasetField[]) l1.get(0);
                    FileMetadata fm2 = l2;
                    int a = fm1.getDataFile().getId().intValue();
                    int b = fm2.getDataFile().getId().intValue();
                    return Integer.valueOf(a).compareTo(Integer.valueOf(b));
                }
            });
           
           // Here's a potential problem: this new version may have been created
           // specifically because new files are being added to the dataset. 
           // In which case there may be files associated with this new version 
           // with no database ids - since they haven't been saved yet. 
           // So if we try to sort the files in the version the way we did above, 
           // by ID, it may fail with a null pointer. 
           // To solve this, we should simply check if the file has the id; and if not, 
           // sort it higher than any file with an id - because it is a most recently
           // added file. Since we are only doing this for the purposes of generating
           // version differences, this should be OK. 
           //   -- L.A. Aug. 2014
           
            Collections.sort(newVersion.getFileMetadatas(), new Comparator<FileMetadata>() {
                public int compare(FileMetadata l1, FileMetadata l2) {
                    FileMetadata fm1 = l1;  //(DatasetField[]) l1.get(0);
                    FileMetadata fm2 = l2;
                    Long a = fm1.getDataFile().getId();
                    Long b = fm2.getDataFile().getId();
                    
                    if (a == null && b == null) {
                        return 0;
                    } else if (a == null) {
                        return 1; 
                    } else if (b == null) {
                        return -1;
                    }
                    return a.compareTo(b);
                }
            });

        while (i < originalVersion.getFileMetadatas().size()
                && j < newVersion.getFileMetadatas().size()) {
            fm1 = originalVersion.getFileMetadatas().get(i);
            fm2 = newVersion.getFileMetadatas().get(j);

            if (fm2.getDataFile().getId() != null && fm1.getDataFile().getId().compareTo(fm2.getDataFile().getId()) == 0) {
                // The 2 versions share the same study file;
                // Check if the metadata information is identical in the 2 versions
                // of the metadata:
                if (fileMetadataIsDifferent(fm1, fm2)) {
                    datasetFileDifferenceItem fdi = selectFileMetadataDiffs(fm1, fm2);
                    fdi.setFileId(fm1.getDataFile().getId().toString());
                    fdi.setFileMD5(fm1.getDataFile().getmd5());
                    datasetFilesDiffList.add(fdi);
                }
                i++;
                j++;
            } else if (fm2.getDataFile().getId() != null && fm1.getDataFile().getId().compareTo(fm2.getDataFile().getId()) > 0) {
                datasetFileDifferenceItem fdi = selectFileMetadataDiffs(null, fm2);
                fdi.setFileId(fm2.getDataFile().getId().toString());
                fdi.setFileMD5(fm2.getDataFile().getmd5());
                datasetFilesDiffList.add(fdi);

                j++;
            } else if (fm2.getDataFile().getId() == null || fm1.getDataFile().getId().compareTo(fm2.getDataFile().getId()) < 0) {
                datasetFileDifferenceItem fdi = selectFileMetadataDiffs(fm1, null);
                fdi.setFileId(fm1.getDataFile().getId().toString());
                fdi.setFileMD5(fm1.getDataFile().getmd5());
                datasetFilesDiffList.add(fdi);

                i++;
            }
        }

        // We've reached the end of at least one file list.
        // Whatever files are left on either of the 2 lists are automatically "different"
        // between the 2 versions.
        while (i < originalVersion.getFileMetadatas().size()) {
            fm1 = originalVersion.getFileMetadatas().get(i);
            datasetFileDifferenceItem fdi = selectFileMetadataDiffs(fm1, null);
            fdi.setFileId(fm1.getDataFile().getId().toString());
            fdi.setFileMD5(fm1.getDataFile().getmd5());
            datasetFilesDiffList.add(fdi);

            i++;
        }

        while (j < newVersion.getFileMetadatas().size()) {
            fm2 = newVersion.getFileMetadatas().get(j);
            datasetFileDifferenceItem fdi = selectFileMetadataDiffs(null, fm2);
            if (fm2.getDataFile().getId() != null) {
                fdi.setFileId(fm2.getDataFile().getId().toString());
            } else {
                fdi.setFileId("[UNASSIGNED]");
            }
            if (fm2.getDataFile().getmd5() != null) {
                fdi.setFileMD5(fm2.getDataFile().getmd5());
            } else {
                fdi.setFileMD5("[UNASSIGNED]");
            }
            datasetFilesDiffList.add(fdi);

            j++;
        }

        if (datasetFilesDiffList.size() == 0) {
            noFileDifferencesFoundLabel = "These study versions have identical sets of data files";
        }
    }

    private boolean fileMetadataIsDifferent(FileMetadata fm1, FileMetadata fm2) {
        if (fm1 == null && fm2 == null) {
            return false;
        }

        if (fm1 == null && fm2 != null) {
            return true;
        }

        if (fm2 == null && fm1 != null) {
            return true;
        }

        // Both are non-null metadata objects.
        // We simply go through the 5 metadata fields, if any one of them
        // is different between the 2 versions, we declare the objects
        // different.
        String value1;
        String value2;

        // filename:
        value1 = fm1.getLabel();
        value2 = fm2.getLabel();

        if (value1 == null || value1.equals("") || value1.equals(" ")) {
            value1 = "";
        }
        if (value2 == null || value2.equals("") || value2.equals(" ")) {
            value2 = "";
        }

        if (!value1.equals(value2)) {
            return true;
        }

        // file type:
        value1 = fm1.getDataFile().getFriendlyType();
        value2 = fm2.getDataFile().getFriendlyType();

        if (value1 == null || value1.equals("") || value1.equals(" ")) {
            value1 = "";
        }
        if (value2 == null || value2.equals("") || value2.equals(" ")) {
            value2 = "";
        }

        if (!value1.equals(value2)) {
            return true;
        }

        // file size:
        /*
         value1 = FileUtil.byteCountToDisplaySize(new File(fm1.getStudyFile().getFileSystemLocation()).length());
         value2 = FileUtil.byteCountToDisplaySize(new File(fm2.getStudyFile().getFileSystemLocation()).length());

         if (value1 == null || value1.equals("") || value1.equals(" ")) {
         value1 = "";
         }
         if (value2 == null || value2.equals("") || value2.equals(" ")) {
         value2 = "";
         }

         if(!value1.equals(value2)) {
         return true;
         }
         */
        // file category:
        value1 = fm1.getCategoriesByName().toString();
        value2 = fm2.getCategoriesByName().toString();

        if (value1 == null || value1.equals("") || value1.equals(" ")) {
            value1 = "";
        }
        if (value2 == null || value2.equals("") || value2.equals(" ")) {
            value2 = "";
        }

        if (!value1.equals(value2)) {
            return true;
        }

        // file description:
        value1 = fm1.getDescription();
        value2 = fm2.getDescription();

        if (value1 == null || value1.equals("") || value1.equals(" ")) {
            value1 = "";
        }
        if (value2 == null || value2.equals("") || value2.equals(" ")) {
            value2 = "";
        }

        if (!value1.equals(value2)) {
            return true;
        }
        
        //File restrictions
        
        value1 = fm1.isRestricted() ? "Restricted" : "Not Restricted";
        value2 = fm2.isRestricted() ? "Restricted" : "Not Restricted";
        
        if (!value1.equals(value2)) {
            return true;
        }
        // if we got this far, the 2 metadatas are identical:
        return false;
    }

    private datasetFileDifferenceItem selectFileMetadataDiffs(FileMetadata fm1, FileMetadata fm2) {
        datasetFileDifferenceItem fdi = new datasetFileDifferenceItem();

        if (fm1 == null && fm2 == null) {
            // this should never happen; but if it does,
            // we return an empty diff object.

            return fdi;

        }
        if (fm2 == null) {
            fdi.setFileName1(fm1.getLabel());
            fdi.setFileType1(fm1.getDataFile().getFriendlyType());
            //fdi.setFileSize1(FileUtil. (new File(fm1.getDataFile().getFileSystemLocation()).length()));

            // deprecated: fdi.setFileCat1(fm1.getCategory());
            fdi.setFileDesc1(fm1.getDescription());
            if(!fm1.getCategoriesByName().isEmpty()){
                fdi.setFileCat1(fm1.getCategoriesByName().toString());
            }

            fdi.setFileRest1(fm1.isRestricted() ? "Restricted" : "Not Restricted");
            fdi.setFile2Empty(true);

        } else if (fm1 == null) {
            fdi.setFile1Empty(true);

            fdi.setFileName2(fm2.getLabel());
            fdi.setFileType2(fm2.getDataFile().getFriendlyType());
            
            //fdi.setFileSize2(FileUtil.byteCountToDisplaySize(new File(fm2.getStudyFile().getFileSystemLocation()).length()));
            // deprecated: fdi.setFileCat2(fm2.getCategory());
            fdi.setFileDesc2(fm2.getDescription());
            if(!fm2.getCategoriesByName().isEmpty()){
                fdi.setFileCat2(fm2.getCategoriesByName().toString());
            }

            fdi.setFileRest2(fm2.isRestricted() ? "Restricted" : "Not Restricted");
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

            if (value1 == null || value1.equals("") || value1.equals(" ")) {
                value1 = "";
            }
            if (value2 == null || value2.equals("") || value2.equals(" ")) {
                value2 = "";
            }

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
            if (value1 == null || value1.equals("") || value1.equals(" ")) {
                value1 = "";
            }
            if (value2 == null || value2.equals("") || value2.equals(" ")) {
                value2 = "";
            }

            if (!value1.equals(value2)) {
                fdi.setFileCat1(value1);
                fdi.setFileCat2(value2);
            } 

            // file description:
            value1 = fm1.getDescription();
            value2 = fm2.getDescription();

            if (value1 == null || value1.equals("") || value1.equals(" ")) {
                value1 = "";
            }
            if (value2 == null || value2.equals("") || value2.equals(" ")) {
                value2 = "";
            }

            if (!value1.equals(value2)) {

                fdi.setFileDesc1(value1);
                fdi.setFileDesc2(value2);
            }

            value1 = fm1.isRestricted() ? "Restricted" : "Not Restricted";
            value2 = fm2.isRestricted() ? "Restricted" : "Not Restricted";
            if (!value1.equals(value2)) {
                fdi.setFileRest1(value1);
                fdi.setFileRest2(value2);
            }
        }
        return fdi;
    }

    public class datasetFileDifferenceItem {

        public datasetFileDifferenceItem() {
        }

        private String fileId;
        private String fileMD5;

        private String fileName1;
        private String fileType1;
        private String fileSize1;
        private String fileCat1;
        private String fileDesc1;
        private String fileRest1;
        
        private String fileName2;
        private String fileType2;
        private String fileSize2;
        private String fileCat2;
        private String fileDesc2;
        private String fileRest2;

        public String getFileRest1() {
            return fileRest1;
        }

        public void setFileRest1(String fileRest1) {
            this.fileRest1 = fileRest1;
        }

        public String getFileRest2() {
            return fileRest2;
        }

        public void setFileRest2(String fileRest2) {
            this.fileRest2 = fileRest2;
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
        
        public String getFileMD5() {
            return fileMD5;
        }

        public void setFileMD5(String fileMD5) {
            this.fileMD5 = fileMD5;
        }

    }

    public List<datasetFileDifferenceItem> getDatasetFilesDiffList() {
        return datasetFilesDiffList;
    }

    public void setDatasetFilesDiffList(List<datasetFileDifferenceItem> datasetFilesDiffList) {
        this.datasetFilesDiffList = datasetFilesDiffList;
    }

    public String getNoFileDifferencesFoundLabel() {
        return noFileDifferencesFoundLabel;
    }

    public void setNoFileDifferencesFoundLabel(String noFileDifferencesFoundLabel) {
        this.noFileDifferencesFoundLabel = noFileDifferencesFoundLabel;
    }

    /**
     * Representation of a version difference summary.
     */
    public class SummaryData {
        private DatasetField dsf;
        private int added, deleted, changed;

        private SummaryData(DatasetField dsfo, int added, int deleted, int changed) {
            this.dsf = dsfo;
            this.added = added;
            this.deleted = deleted;
            this.changed = changed;
        }

        public DatasetField getDatasetField() {
            return dsf;
        }

        public void setDatasetField(DatasetField dsf) {
            this.dsf = dsf;
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

        public int getChanged() {
            return changed;
        }

        public void setChanged(int changed) {
            this.changed = changed;
        }

    }

    /**
     * Representation of a version difference of a value in a metadata block.
     */
    public class BlockData {
        private DatasetField dsf;
        private int added, deleted, changed;

        private BlockData(DatasetField dsfo, int added, int deleted, int changed) {
            this.dsf = dsfo;
            this.added = added;
            this.deleted = deleted;
            this.changed = changed;
        }

        public DatasetField getDatasetField() {
            return dsf;
        }

        public void setDatasetField(DatasetField dsf) {
            this.dsf = dsf;
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

        public int getChanged() {
            return changed;
        }

        public void setChanged(int changed) {
            this.changed = changed;
        }

        private void increaseAdded(int added) {
            this.added += added;
        }

        private void increaseDeleted(int deleted) {
            this.deleted += deleted;
        }

        private void increaseChanged(int changed) {
            this.changed += changed;
        }

    }
}
