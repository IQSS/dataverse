package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VarGroup;
import edu.harvard.iq.dataverse.datavariable.VariableMetadataUtil;
import edu.harvard.iq.dataverse.util.StringUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import org.apache.commons.lang3.StringUtils;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author skraffmiller
 */
public final class DatasetVersionDifference {
    private static final Logger logger = Logger.getLogger(DatasetVersionDifference.class.getCanonicalName());

    private DatasetVersion newVersion;
    private DatasetVersion originalVersion;
    private List<List<DatasetField[]>> detailDataByBlock = new ArrayList<>();
    private List<datasetFileDifferenceItem> datasetFilesDiffList;
    private List<datasetReplaceFileItem> datasetFilesReplacementList;
    private List<FileMetadata> addedFiles = new ArrayList<>();
    private List<FileMetadata> removedFiles = new ArrayList<>();
    private List<FileMetadata> changedFileMetadata = new ArrayList<>();
    private Map<FileMetadata, Map<String,List<String>>> changedFileMetadataDiff = new HashMap<>();
    private List<FileMetadata> changedVariableMetadata = new ArrayList<>();
    private List<FileMetadata[]> replacedFiles = new ArrayList<>();
    private List<String[]> changedTermsAccess = new ArrayList<>();
    private List<SummaryNote> summaryDataForNote = new ArrayList<>();
    private List<SummaryNote> blockDataForNote = new ArrayList<>();

    private List<DifferenceSummaryGroup> differenceSummaryGroups = new ArrayList<>();

    public List<DifferenceSummaryGroup> getDifferenceSummaryGroups() {
        return differenceSummaryGroups;
    }

    public void setDifferenceSummaryGroups(List<DifferenceSummaryGroup> differenceSummaryGroups) {
        this.differenceSummaryGroups = differenceSummaryGroups;
    }

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
                if (dsfo.getDatasetFieldType().isPrimitive()) {
                    if (dsfo.getDatasetFieldType().isControlledVocabulary()) {
                        updateBlockSummary(dsfo, 0, dsfo.getControlledVocabularyValues().size(), 0);
                    } else {
                        updateBlockSummary(dsfo, 0, dsfo.getDatasetFieldValues().size(), 0);
                    }
                } else {
                    updateBlockSummary(dsfo, 0, dsfo.getDatasetFieldCompoundValues().size(), 0);
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
                if (dsfn.getDatasetFieldType().isPrimitive()){
                   if (dsfn.getDatasetFieldType().isControlledVocabulary()) {
                       updateBlockSummary(dsfn, dsfn.getControlledVocabularyValues().size(), 0, 0);
                   } else {
                       updateBlockSummary(dsfn, dsfn.getDatasetFieldValues().size(), 0, 0);
                   }                  
                } else {
                   updateBlockSummary(dsfn, dsfn.getDatasetFieldCompoundValues().size(), 0, 0);
                }
                addToSummary(null, dsfn);
            }
        }
        long startTime = System.currentTimeMillis();
        Map<Long, FileMetadata> originalFileMetadataMap = new HashMap<>();
        Map<Long, FileMetadata> previousIDtoFileMetadataMap = new HashMap<>();
        for (FileMetadata fmdo : originalVersion.getFileMetadatas()) {
            originalFileMetadataMap.put(fmdo.getDataFile().getId(), fmdo);
        }

        for (FileMetadata fmdn : newVersion.getFileMetadatas()) {
            DataFile ndf = fmdn.getDataFile();
            Long id = ndf.getId();
            FileMetadata fmdo = originalFileMetadataMap.get(id);
            //If this file was in the original version
            if(fmdo!= null) {
                //Check for differences
                Map<String, List<String>> fileMetadataDiff = compareFileMetadatas(fmdo, fmdn);
                if (!fileMetadataDiff.isEmpty()) {
                    changedFileMetadata.add(fmdo);
                    changedFileMetadata.add(fmdn);
                    // TODO: find a better key for the map. needs to be something that doesn't change
                    changedFileMetadataDiff.put(fmdo, fileMetadataDiff);
                }
                if (!VariableMetadataUtil.compareVariableMetadata(fmdo,fmdn) || !compareVarGroup(fmdo, fmdn)) {
                    changedVariableMetadata.add(fmdo);
                    changedVariableMetadata.add(fmdn);
                }
                // And drop it from the list since it can't be a deleted file
                originalFileMetadataMap.remove(id);
            } else {
                //It wasn't in the original version
                Long prevID = ndf.getPreviousDataFileId();
                //It might be a replacement file or an added file
                if(prevID != null) {
                    //Add it to a map so we can check later to see if it's a replacement
                    previousIDtoFileMetadataMap.put(prevID, fmdn);
                } else {
                    //Otherwise make it an added file now
                    addedFiles.add(fmdn);
                }
            }
        }
        //Finally check any remaining files from the original version that weren't in the new version'
        for (Long removedId : originalFileMetadataMap.keySet()) {
            //See if it has been replaced
            FileMetadata replacingFmd = previousIDtoFileMetadataMap.get(removedId);
            FileMetadata fmdRemoved = originalFileMetadataMap.get(removedId);
            if (replacingFmd != null) {
                //This is a replacement
                replacedFiles.add(new FileMetadata[] { fmdRemoved, replacingFmd });
                //Drop if from the map 
                previousIDtoFileMetadataMap.remove(removedId);
            } else {
                //This is a removed file
                removedFiles.add(fmdRemoved);
            }
        }
        // Any fms left are not updating existing files and aren't replacing a file, but
        // they are claiming a previous file id. That shouldn't be possible, but this will
        // make sure they get listed in the difference if they do
        for (Entry<Long, FileMetadata> entry : previousIDtoFileMetadataMap.entrySet()) {
            logger.warning("Previous file id claimed for a new file: fmd id: " + entry.getValue() + ", previous file id: " + entry.getKey());
            addedFiles.add(entry.getValue());
        }
        
        logger.fine("Main difference loop execution time: " + (System.currentTimeMillis() - startTime) + " ms");
        initDatasetFilesDifferencesList();

        //Sort within blocks by datasetfieldtype display order
        for (List<DatasetField[]> blockList : detailDataByBlock) {
            Collections.sort(blockList, (DatasetField[] l1, DatasetField[] l2) -> {
                    DatasetField dsfa = l1[0];  //(DatasetField[]) l1.get(0);
                    DatasetField dsfb = l2[0];
                    int a = dsfa.getDatasetFieldType().getDisplayOrder();
                    int b = dsfb.getDatasetFieldType().getDisplayOrder();
                return Integer.valueOf(a).compareTo(b);
            });
        }
        //Sort existing compoundValues by datasetfieldtype display order
        for (List<DatasetField[]> blockList : detailDataByBlock) {
            for (DatasetField[] dfarr : blockList) {
                for (DatasetField df : dfarr) {
                    for (DatasetFieldCompoundValue dfcv : df.getDatasetFieldCompoundValues()) {
                        Collections.sort(dfcv.getChildDatasetFields(), DatasetField.DisplayOrder);
                    }
                }
            }
        }
        //Sort via metadatablock order
        Collections.sort(detailDataByBlock, (List l1, List l2) -> {
                DatasetField dsfa[] = (DatasetField[]) l1.get(0);
                DatasetField dsfb[] = (DatasetField[]) l2.get(0);
                int a = dsfa[0].getDatasetFieldType().getMetadataBlock().getId().intValue();
                int b = dsfb[0].getDatasetFieldType().getMetadataBlock().getId().intValue();
            return Integer.valueOf(a).compareTo(b);
        });
        getTermsDifferences();
    }
    

       
    private void getTermsDifferences() {

        TermsOfUseAndAccess originalTerms = originalVersion.getTermsOfUseAndAccess();
        if(originalTerms == null) {
            originalTerms = new TermsOfUseAndAccess();
        }
        // newTerms should never be null
        TermsOfUseAndAccess newTerms = newVersion.getTermsOfUseAndAccess();
        if(newTerms == null) {
            logger.warning("New version does not have TermsOfUseAndAccess");
            newTerms = new TermsOfUseAndAccess();
        }
        
        checkAndAddToChangeList(originalTerms.getTermsOfUse(), newTerms.getTermsOfUse(),
                BundleUtil.getStringFromBundle("file.dataFilesTab.terms.list.termsOfUse.header"));
        checkAndAddToChangeList(originalTerms.getConfidentialityDeclaration(), newTerms.getConfidentialityDeclaration(),
                BundleUtil.getStringFromBundle("file.dataFilesTab.terms.list.termsOfUse.addInfo.declaration"));
        checkAndAddToChangeList(originalTerms.getSpecialPermissions(), newTerms.getSpecialPermissions(),
                BundleUtil.getStringFromBundle("file.dataFilesTab.terms.list.termsOfUse.addInfo.permissions"));
        checkAndAddToChangeList(originalTerms.getRestrictions(), newTerms.getRestrictions(),
                BundleUtil.getStringFromBundle("file.dataFilesTab.terms.list.termsOfUse.addInfo.restrictions"));
        checkAndAddToChangeList(originalTerms.getCitationRequirements(), newTerms.getCitationRequirements(),
                BundleUtil.getStringFromBundle("file.dataFilesTab.terms.list.termsOfUse.addInfo.citationRequirements"));
        checkAndAddToChangeList(originalTerms.getDepositorRequirements(), newTerms.getDepositorRequirements(),
                BundleUtil.getStringFromBundle("file.dataFilesTab.terms.list.termsOfUse.addInfo.depositorRequirements"));
        checkAndAddToChangeList(originalTerms.getConditions(), newTerms.getConditions(),
                BundleUtil.getStringFromBundle("file.dataFilesTab.terms.list.termsOfUse.addInfo.conditions"));
        checkAndAddToChangeList(originalTerms.getDisclaimer(), newTerms.getDisclaimer(),
                BundleUtil.getStringFromBundle("file.dataFilesTab.terms.list.termsOfUse.addInfo.disclaimer"));
        checkAndAddToChangeList(originalTerms.getTermsOfAccess(), newTerms.getTermsOfAccess(),
                BundleUtil.getStringFromBundle("file.dataFilesTab.terms.list.termsOfAccess.termsOfsAccess"));
        checkAndAddToChangeList(originalTerms.getDataAccessPlace(), newTerms.getDataAccessPlace(),
                BundleUtil.getStringFromBundle("file.dataFilesTab.terms.list.termsOfAccess.addInfo.dataAccessPlace"));
        checkAndAddToChangeList(originalTerms.getOriginalArchive(), newTerms.getOriginalArchive(),
                BundleUtil.getStringFromBundle("file.dataFilesTab.terms.list.termsOfAccess.addInfo.originalArchive"));
        checkAndAddToChangeList(originalTerms.getAvailabilityStatus(), newTerms.getAvailabilityStatus(),
                BundleUtil.getStringFromBundle("file.dataFilesTab.terms.list.termsOfAccess.addInfo.availabilityStatus"));
        checkAndAddToChangeList(originalTerms.getContactForAccess(), newTerms.getContactForAccess(),
                BundleUtil.getStringFromBundle("file.dataFilesTab.terms.list.termsOfAccess.addInfo.contactForAccess"));
        checkAndAddToChangeList(originalTerms.getSizeOfCollection(), newTerms.getSizeOfCollection(),
                BundleUtil.getStringFromBundle("file.dataFilesTab.terms.list.termsOfAccess.addInfo.sizeOfCollection"));
        checkAndAddToChangeList(originalTerms.getStudyCompletion(), newTerms.getStudyCompletion(),
                BundleUtil.getStringFromBundle("file.dataFilesTab.terms.list.termsOfAccess.addInfo.studyCompletion"));
    }
    
    private void checkAndAddToChangeList(String originalTerm, String newTerm,
            String termLabel) {
        originalTerm = StringUtil.nullToEmpty(originalTerm);
        newTerm = StringUtil.nullToEmpty(newTerm);
        if(!originalTerm.equals(newTerm)) {
            changedTermsAccess.add(new String[]{termLabel, originalTerm, newTerm});
        }
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
        for (SummaryNote blockList : blockDataForNote) {
            
            DatasetField dsft = blockList.dsfo;
            if (dsft.getDatasetFieldType().getMetadataBlock().equals(dsf.getDatasetFieldType().getMetadataBlock())) {
                blockList.added = blockList.added + added;
                blockList.deleted = blockList.deleted + deleted;
                blockList.changed = blockList.changed + changed;
                addedToAll = true;
            }
        }
        if (!addedToAll) {
            SummaryNote newNote = new SummaryNote();
            newNote.dsfo = dsf;
            newNote.added = added;
            newNote.deleted = deleted;
            newNote.changed = changed;
            blockDataForNote.add(newNote);
        }
    }

    private void addToNoteSummary(DatasetField dsfo, Integer added, Integer deleted, Integer changed) {
        SummaryNote summaryNote = new SummaryNote();
        summaryNote.dsfo = dsfo;
        summaryNote.added = added;
        summaryNote.deleted = deleted;
        summaryNote.changed = changed;
        summaryDataForNote.add(summaryNote);
    }

    static boolean compareVarGroup(FileMetadata fmdo, FileMetadata fmdn) {
        List<VarGroup> vglo = fmdo.getVarGroups();
        List<VarGroup> vgln = fmdn.getVarGroups();

        if (vglo.size() != vgln.size()) {
            return false;
        }
        int count = 0;
        for (VarGroup vgo : vglo) {
            for (VarGroup vgn : vgln) {
                if (!VariableMetadataUtil.checkDiff(vgo.getLabel(), vgn.getLabel())) {
                    Set<DataVariable> dvo = vgo.getVarsInGroup();
                    Set<DataVariable> dvn = vgn.getVarsInGroup();
                    if (dvo.equals(dvn)) {
                        count++;
                    } else {
                        return false;
                    }
                }
            }
        }
        if (count == vglo.size()) {
            return true;
        } else {
            return false;
        }
    }

    public static Map<String,List<String>> compareFileMetadatas(FileMetadata fmdo, FileMetadata fmdn) {
        Map<String,List<String>> fileMetadataChanged = new HashMap<>();
        if (!StringUtils.equals(StringUtil.nullToEmpty(fmdo.getDescription()), StringUtil.nullToEmpty(fmdn.getDescription()))) {
            fileMetadataChanged.put("Description",
                    List.of(StringUtil.nullToEmpty(fmdo.getDescription()), StringUtil.nullToEmpty(fmdn.getDescription())));
        }

        if (!StringUtils.equals(StringUtil.nullToEmpty(fmdo.getCategoriesByName().toString()), StringUtil.nullToEmpty(fmdn.getCategoriesByName().toString()))) {
            fileMetadataChanged.put("Categories",
                    List.of(fmdo.getCategoriesByName().toString(), fmdn.getCategoriesByName().toString()));
        }
        
        if (!StringUtils.equals(StringUtil.nullToEmpty(fmdo.getLabel()), StringUtil.nullToEmpty(fmdn.getLabel()))) {
            fileMetadataChanged.put("Label",
                    List.of(StringUtil.nullToEmpty(fmdo.getLabel()), StringUtil.nullToEmpty(fmdn.getLabel())));
        }
        
        if (!StringUtils.equals(StringUtil.nullToEmpty(fmdo.getDirectoryLabel()), StringUtil.nullToEmpty(fmdn.getDirectoryLabel()))) {
            fileMetadataChanged.put("File Path",
                    List.of(StringUtil.nullToEmpty(fmdo.getDirectoryLabel()), StringUtil.nullToEmpty(fmdn.getDirectoryLabel())));
        }
        
        if (!StringUtils.equals(StringUtil.nullToEmpty(fmdo.getProvFreeForm()), StringUtil.nullToEmpty(fmdn.getProvFreeForm()))) {
            fileMetadataChanged.put("ProvFreeForm",
                    List.of(StringUtil.nullToEmpty(fmdo.getProvFreeForm()), StringUtil.nullToEmpty(fmdn.getProvFreeForm())));
        }

        if (fmdo.isRestricted() != fmdn.isRestricted()) {
            fileMetadataChanged.put("isRestricted",
                    List.of(String.valueOf(fmdo.isRestricted()), String.valueOf(fmdn.isRestricted())));
        }

        return fileMetadataChanged;
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
            retString = BundleUtil.getStringFromBundle("dataset.version.file.added", Arrays.asList(addedFiles.size()+""));
        }

        if (removedFiles.size() > 0) {
            if (retString.isEmpty()) {
                retString = BundleUtil.getStringFromBundle("dataset.version.file.removed", Arrays.asList(removedFiles.size()+""));
            } else {
                retString += BundleUtil.getStringFromBundle("dataset.version.file.removed2", Arrays.asList(removedFiles.size()+""));
            }
        }
        
        if (replacedFiles.size() > 0) {
            if (retString.isEmpty()) {
                retString = BundleUtil.getStringFromBundle("dataset.version.file.replaced", Arrays.asList(replacedFiles.size()+""));
            } else {
                retString += BundleUtil.getStringFromBundle("dataset.version.file.replaced2", Arrays.asList(replacedFiles.size()+""));
            }
        }
        

        if (changedFileMetadata.size() > 0) {
            if (retString.isEmpty()) {
                retString = BundleUtil.getStringFromBundle("dataset.version.file.changed", Arrays.asList(changedFileMetadata.size() / 2+""));
            } else {
                retString += BundleUtil.getStringFromBundle("dataset.version.file.changed2", Arrays.asList(changedFileMetadata.size() / 2+""));
            }
        }

        if (changedVariableMetadata.size()  > 0) {
            if (retString.isEmpty()) {
                retString = BundleUtil.getStringFromBundle("dataset.version.variablemetadata.changed", Arrays.asList(changedVariableMetadata.size() / 2+""));
            } else {
                retString += BundleUtil.getStringFromBundle("dataset.version.variablemetadata.changed2", Arrays.asList(changedVariableMetadata.size() / 2+""));
            }
        }

        if (!retString.isEmpty()) {
            retString += ")";
        }

        return retString;
    }
    
    public List<datasetReplaceFileItem> getDatasetFilesReplacementList() {
        return datasetFilesReplacementList;
    }

    public void setDatasetFilesReplacementList(List<datasetReplaceFileItem> datasetFilesReplacementList) {
        this.datasetFilesReplacementList = datasetFilesReplacementList;
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

    public List<SummaryNote> getSummaryDataForNote() {
        return summaryDataForNote;
    }

    public List<SummaryNote> getBlockDataForNote() {
        return blockDataForNote;
    }

    public void setSummaryDataForNote(List<SummaryNote> summaryDataForNote) {
        this.summaryDataForNote = summaryDataForNote;
    }

    public void setBlockDataForNote(List<SummaryNote> blockDataForNote) {
        this.blockDataForNote = blockDataForNote;
    }
    
    
    public List<String[]> getChangedTermsAccess() {
        return changedTermsAccess;
    }

    public void setChangedTermsAccess(List<String[]> changedTermsAccess) {
        this.changedTermsAccess = changedTermsAccess;
    }

    private void initDatasetFilesDifferencesList() {
        datasetFilesDiffList = new ArrayList<>();
        datasetFilesReplacementList = new ArrayList <>();
        
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

        int i = 0;
        int j = 0;

        FileMetadata fm1;
        FileMetadata fm2;
        
        // We also have to be careful sorting this FileMetadatas. If we sort the 
        // lists as they are still attached to their respective versions, we may end
        // up messing up the page, which was rendered based on the specific order 
        // of these in the working version! 
        // So the right way of doing this is to create defensive copies of the
        // lists; extra memory, but safer. 
        // -- L.A. Nov. 2016
        
        List<FileMetadata> fileMetadatasNew = new ArrayList<>(newVersion.getFileMetadatas());
        List<FileMetadata> fileMetadatasOriginal = new ArrayList<>(originalVersion.getFileMetadatas());
        
        if (!replacedFiles.isEmpty()) {
            
            replacedFiles.stream().map((replacedPair) -> {
                FileMetadata replacedFile = replacedPair[0];
                FileMetadata newFile = replacedPair[1];
                fileMetadatasNew.remove(newFile);
                fileMetadatasOriginal.remove(replacedFile);
                datasetFileDifferenceItem fdi = selectFileMetadataDiffs(replacedFile, newFile);
                datasetReplaceFileItem fdr = new datasetReplaceFileItem();
                String diffLabel = BundleUtil.getStringFromBundle("file.dataFilesTab.versions.replaced");
                fdr.setLeftColumn(diffLabel);
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
            }).forEach((fdr) -> {
                datasetFilesReplacementList.add(fdr);
            });

        }

        Collections.sort(fileMetadatasOriginal, (FileMetadata l1, FileMetadata l2) -> {
            FileMetadata fm3 = l1; //(DatasetField[]) l1.get(0);
            FileMetadata fm4 = l2;
            int a = fm3.getDataFile().getId().intValue();
            int b = fm4.getDataFile().getId().intValue();
            return Integer.valueOf(a).compareTo(b);
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

        Collections.sort(fileMetadatasNew, (FileMetadata l1, FileMetadata l2) -> {
            FileMetadata fm3 = l1; //(DatasetField[]) l1.get(0);
            FileMetadata fm4 = l2;
            Long a = fm3.getDataFile().getId();
            Long b = fm4.getDataFile().getId();
                if (a == null && b == null) {
                    return 0;
                } else if (a == null) {
                    return 1;
                } else if (b == null) {
                    return -1;
                }
                return a.compareTo(b);
        });

        while (i < fileMetadatasOriginal.size()
                && j < fileMetadatasNew.size()) {
            fm1 = fileMetadatasOriginal.get(i);
            fm2 = fileMetadatasNew.get(j);

            if (fm2.getDataFile().getId() != null && fm1.getDataFile().getId().compareTo(fm2.getDataFile().getId()) == 0) {
                // The 2 versions share the same study file;
                // Check if the metadata information is identical in the 2 versions
                // of the metadata:
                if (fileMetadataIsDifferent(fm1, fm2)) {
                    datasetFileDifferenceItem fdi = selectFileMetadataDiffs(fm1, fm2);
                    fdi.setFileId(fm1.getDataFile().getId().toString());
                    fdi.setFileChecksumType(fm1.getDataFile().getChecksumType());
                    fdi.setFileChecksumValue(fm1.getDataFile().getChecksumValue());
                    datasetFilesDiffList.add(fdi);
                }
                i++;
                j++;
            } else if (fm2.getDataFile().getId() != null && fm1.getDataFile().getId().compareTo(fm2.getDataFile().getId()) > 0) {
                datasetFileDifferenceItem fdi = selectFileMetadataDiffs(null, fm2);
                fdi.setFileId(fm2.getDataFile().getId().toString());
                fdi.setFileChecksumType(fm2.getDataFile().getChecksumType());
                fdi.setFileChecksumValue(fm2.getDataFile().getChecksumValue());
                datasetFilesDiffList.add(fdi);

                j++;
            } else if (fm2.getDataFile().getId() == null || fm1.getDataFile().getId().compareTo(fm2.getDataFile().getId()) < 0) {
                datasetFileDifferenceItem fdi = selectFileMetadataDiffs(fm1, null);
                fdi.setFileId(fm1.getDataFile().getId().toString());
                fdi.setFileChecksumType(fm1.getDataFile().getChecksumType());
                fdi.setFileChecksumValue(fm1.getDataFile().getChecksumValue());
                datasetFilesDiffList.add(fdi);

                i++;
            }
        }

        // We've reached the end of at least one file list.
        // Whatever files are left on either of the 2 lists are automatically "different"
        // between the 2 versions.
        while (i < fileMetadatasOriginal.size()) {
            fm1 = fileMetadatasOriginal.get(i);
            datasetFileDifferenceItem fdi = selectFileMetadataDiffs(fm1, null);
            fdi.setFileId(fm1.getDataFile().getId().toString());
            fdi.setFileChecksumType(fm1.getDataFile().getChecksumType());
            fdi.setFileChecksumValue(fm1.getDataFile().getChecksumValue());
            datasetFilesDiffList.add(fdi);

            i++;
        }

        while (j < fileMetadatasNew.size()) {
            fm2 = fileMetadatasNew.get(j);
            datasetFileDifferenceItem fdi = selectFileMetadataDiffs(null, fm2);
            if (fm2.getDataFile().getId() != null) {
                fdi.setFileId(fm2.getDataFile().getId().toString());
            } else {
                fdi.setFileId("[UNASSIGNED]");
            }
            if (fm2.getDataFile().getChecksumValue() != null) {
                fdi.setFileChecksumType(fm2.getDataFile().getChecksumType());
                fdi.setFileChecksumValue(fm2.getDataFile().getChecksumValue());
            } else {
                /**
                 * @todo What should we do here? checksumValue is set to
                 * "nullable = false" so it should never be non-null. Let's set
                 * it to "null" and see if this code path is ever reached. If
                 * not, the null check above can probably be safely removed.
                 */
                fdi.setFileChecksumType(null);
                fdi.setFileChecksumValue("[UNASSIGNED]");
            }
            datasetFilesDiffList.add(fdi);

            j++;
        }
    }

    private boolean fileMetadataIsDifferent(FileMetadata fm1, FileMetadata fm2) {
        if (fm1 == null){
            return fm2 != null;
        }
        if (fm2 == null) {
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

        if (value1 == null || value1.isEmpty() || value1.equals(" ")) {
            value1 = "";
        }
        if (value2 == null || value2.isEmpty() || value2.equals(" ")) {
            value2 = "";
        }

        if (!value1.equals(value2)) {
            return true;
        }

        // file type:
        value1 = fm1.getDataFile().getFriendlyType();
        value2 = fm2.getDataFile().getFriendlyType();

        if (value1 == null || value1.isEmpty() || value1.equals(" ")) {
            value1 = "";
        }
        if (value2 == null || value2.isEmpty() || value2.equals(" ")) {
            value2 = "";
        }

        if (!value1.equals(value2)) {
            return true;
        }

        // file size:
        /*
         value1 = FileUtil.byteCountToDisplaySize(new File(fm1.getStudyFile().getFileSystemLocation()).length());
         value2 = FileUtil.byteCountToDisplaySize(new File(fm2.getStudyFile().getFileSystemLocation()).length());

         if (value1 == null || value1.isEmpty() || value1.equals(" ")) {
         value1 = "";
         }
         if (value2 == null || value2.isEmpty() || value2.equals(" ")) {
         value2 = "";
         }

         if(!value1.equals(value2)) {
         return true;
         }
         */
        // file category:
        value1 = fm1.getCategoriesByName().toString();
        value2 = fm2.getCategoriesByName().toString();

        if (value1 == null || value1.isEmpty() || value1.equals(" ")) {
            value1 = "";
        }
        if (value2 == null || value2.isEmpty() || value2.equals(" ")) {
            value2 = "";
        }

        if (!value1.equals(value2)) {
            return true;
        }

        // file description:
        value1 = fm1.getDescription();
        value2 = fm2.getDescription();

        if (value1 == null || value1.isEmpty() || value1.equals(" ")) {
            value1 = "";
        }
        if (value2 == null || value2.isEmpty() || value2.equals(" ")) {
            value2 = "";
        }

        if (!value1.equals(value2)) {
            return true;
        }
        
        // Provenance Freeform Text
        value1 = fm1.getProvFreeForm();
        value2 = fm2.getProvFreeForm();
        
        if (value1 == null || value1.isEmpty() || value1.equals(" ")) {
            value1 = "";
        }
        if (value2 == null || value2.isEmpty() || value2.equals(" ")) {
            value2 = "";
        }

        if (!value1.equals(value2)) {
            return true;
        }
        
        // File restrictions
        return fm1.isRestricted() != fm2.isRestricted();
    }

    private datasetFileDifferenceItem selectFileMetadataDiffs(FileMetadata fm1, FileMetadata fm2) {
        datasetFileDifferenceItem fdi = new datasetFileDifferenceItem();

        if (fm2 == null) {
            if (fm1 == null) {
                // this should never happen; but if it does,
                // we return an empty diff object.

                return fdi;
            }
            fdi.setFileName1(fm1.getLabel());
            fdi.setFileType1(fm1.getDataFile().getFriendlyType());
            //fdi.setFileSize1(FileUtil. (new File(fm1.getDataFile().getFileSystemLocation()).length()));

            // deprecated: fdi.setFileCat1(fm1.getCategory());
            fdi.setFileDesc1(fm1.getDescription());
            if(!fm1.getCategoriesByName().isEmpty()){
                fdi.setFileCat1(fm1.getCategoriesByName().toString());
            }

            fdi.setFileProvFree1(fm1.getProvFreeForm());
            fdi.setFileRest1(BundleUtil.getStringFromBundle(getAccessLabel(fm1)));
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
            fdi.setFileProvFree2(fm2.getProvFreeForm());
            fdi.setFileRest2(BundleUtil.getStringFromBundle(getAccessLabel(fm2)));
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

            if (value1 == null || value1.isEmpty() || value1.equals(" ")) {
                value1 = "";
            }
            if (value2 == null || value2.isEmpty() || value2.equals(" ")) {
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
            if (value1 == null || value1.isEmpty() || value1.equals(" ")) {
                value1 = "";
            }
            if (value2 == null || value2.isEmpty() || value2.equals(" ")) {
                value2 = "";
            }

            if (!value1.equals(value2)) {
                fdi.setFileCat1(value1);
                fdi.setFileCat2(value2);
            } 

            // file description:
            value1 = fm1.getDescription();
            value2 = fm2.getDescription();

            if (value1 == null || value1.isEmpty() || value1.equals(" ")) {
                value1 = "";
            }
            if (value2 == null || value2.isEmpty() || value2.equals(" ")) {
                value2 = "";
            }

            if (!value1.equals(value2)) {

                fdi.setFileDesc1(value1);
                fdi.setFileDesc2(value2);
            }

            // provenance freeform
            value1 = fm1.getProvFreeForm();
            value2 = fm2.getProvFreeForm();

            if (value1 == null || value1.isEmpty() || value1.equals(" ")) {
                value1 = "";
            }
            if (value2 == null || value2.isEmpty() || value2.equals(" ")) {
                value2 = "";
            }

            if (!value1.equals(value2)) {

                fdi.setFileProvFree1(value1);
                fdi.setFileProvFree2(value2);
            }
            
            // file restricted:
            if (fm1.isRestricted() != fm2.isRestricted() || fm1.getDataFile().getEmbargo() != fm2.getDataFile().getEmbargo()) {
                fdi.setFileRest1(BundleUtil.getStringFromBundle(getAccessLabel(fm1)));
                fdi.setFileRest2(BundleUtil.getStringFromBundle(getAccessLabel(fm2)));
            }
        }
        return fdi;
    }
    
    private String getAccessLabel(FileMetadata fm) {
        boolean embargoed = fm.getDataFile().getEmbargo()!=null;
        boolean restricted = fm.isRestricted();
        if (embargoed && restricted) return "embargoedandrestricted";
        if(embargoed) return "embargoed";
        if(restricted) return "restricted";
        return "public";
    }

    public String getEditSummaryForLog() {
        
        String retVal = "";        
        
        retVal = System.lineSeparator() + this.newVersion.getTitle() + " (" + this.originalVersion.getDataset().getIdentifier() + ") was updated " + new Date();
        
        String valueString = "";
        String groupString = "";
        
        //Metadata differences displayed by Metdata block
        if (!this.detailDataByBlock.isEmpty()) {
            for (List<DatasetField[]> blocks : detailDataByBlock) {
                groupString = System.lineSeparator() + " " + BundleUtil.getStringFromBundle("dataset.versionDifferences.metadataBlock")  ;
                String blockDisplay = " " +  blocks.get(0)[0].getDatasetFieldType().getMetadataBlock().getDisplayName() + ": " +  System.lineSeparator();
                groupString += blockDisplay;
                for (DatasetField[] dsfArray : blocks) {
                    valueString = " " + BundleUtil.getStringFromBundle("dataset.versionDifferences.field") + ": ";
                    String title = dsfArray[0].getDatasetFieldType().getTitle();
                    valueString += title;
                    String oldValue = " " + BundleUtil.getStringFromBundle("dataset.versionDifferences.changed") + " " + BundleUtil.getStringFromBundle("dataset.versionDifferences.from") + ": ";
                    
                    if (!dsfArray[0].isEmpty()) {
                        if (dsfArray[0].getDatasetFieldType().isPrimitive()) {
                            oldValue += dsfArray[0].getRawValue();
                        } else {
                            oldValue += dsfArray[0].getCompoundRawValue();
                        }
                    }
                    valueString += oldValue;
                    
                    String newValue = " " + BundleUtil.getStringFromBundle("dataset.versionDifferences.to") + ": ";
                    if (!dsfArray[1].isEmpty()) {
                        if (dsfArray[1].getDatasetFieldType().isPrimitive()) {
                            newValue += dsfArray[1].getRawValue();
                        } else {
                            newValue += dsfArray[1].getCompoundRawValue();
                        }

                    }
                    valueString += newValue;
                    groupString += valueString + System.lineSeparator();
                }
                retVal += groupString + System.lineSeparator();
            }
        }
        
        // File Differences
        String fileDiff = System.lineSeparator() + BundleUtil.getStringFromBundle("file.viewDiffDialog.files.header") + ": " + System.lineSeparator();
        if(!this.getDatasetFilesDiffList().isEmpty()){
           
            String itemDiff;
            
            for (datasetFileDifferenceItem item : this.getDatasetFilesDiffList()) {
                itemDiff = BundleUtil.getStringFromBundle("file.viewDiffDialog.fileID") + ": " + item.fileId; 
                
                if (item.fileName1 != null || item.fileName2 != null) {
                    itemDiff = System.lineSeparator() + " " + BundleUtil.getStringFromBundle("file.viewDiffDialog.fileName") + ": ";
                    itemDiff += item.fileName1 != null ? item.fileName1 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable");
                    itemDiff += " : ";
                    itemDiff += item.fileName2 != null ? item.fileName2 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable") + " ";
                }

                if (item.fileType1 != null || item.fileType2 != null) {
                    itemDiff += System.lineSeparator() + " " + BundleUtil.getStringFromBundle("file.viewDiffDialog.fileType") + ": ";
                    itemDiff += item.fileType1 != null ? item.fileType1 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable");
                    itemDiff += " : ";
                    itemDiff += item.fileType2 != null ? item.fileType2 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable") + " ";
                }

                if (item.fileSize1 != null || item.fileSize2 != null) {
                    itemDiff += System.lineSeparator() + " " + BundleUtil.getStringFromBundle("file.viewDiffDialog.fileSize") + ": ";
                    itemDiff += item.fileSize1 != null ? item.fileSize1 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable");
                    itemDiff += " : ";
                    itemDiff += item.fileSize2 != null ? item.fileSize2 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable") + " ";
                }
                
                if (item.fileCat1 != null || item.fileCat2 != null) {
                    itemDiff += System.lineSeparator() + " " + BundleUtil.getStringFromBundle("file.viewDiffDialog.category") + ": ";
                    itemDiff += item.fileCat1 != null ? item.fileCat1 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable");
                    itemDiff += " : ";
                    itemDiff += item.fileCat2 != null ? item.fileCat2 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable") + " ";
                }
                
                if (item.fileDesc1 != null || item.fileDesc2 != null) {
                    itemDiff += System.lineSeparator() + " " + BundleUtil.getStringFromBundle("file.viewDiffDialog.description") + ": ";
                    itemDiff += item.fileDesc1 != null ? item.fileDesc1 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable");
                    itemDiff += " : ";
                    itemDiff += item.fileDesc2 != null ? item.fileDesc2 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable") + " ";
                }

                if (item.fileProvFree1 != null || item.fileProvFree2 != null) {
                    itemDiff += System.lineSeparator() + " " + BundleUtil.getStringFromBundle("file.viewDiffDialog.provDescription") + ": ";
                    itemDiff += item.fileProvFree1 != null ? item.fileProvFree1 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable");
                    itemDiff += " : ";
                    itemDiff += item.fileProvFree2 != null ? item.fileProvFree2 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable") + " ";
                }
                
                if (item.fileRest1 != null || item.fileRest2 != null) {
                    itemDiff += System.lineSeparator() + " " + BundleUtil.getStringFromBundle("file.viewDiffDialog.fileAccess") + ": ";
                    itemDiff += item.fileRest1 != null ? item.fileRest1 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable");
                    itemDiff += " : ";
                    itemDiff += item.fileRest2 != null ? item.fileRest2 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable") + " ";

                }
                
                fileDiff += itemDiff;
            }
                     
            retVal += fileDiff;
        }
        
        String fileReplaced = System.lineSeparator() + BundleUtil.getStringFromBundle("file.viewDiffDialog.filesReplaced")+ ": "+ System.lineSeparator();
        if(!this.getDatasetFilesReplacementList().isEmpty()){          
            String itemDiff;          
            for (datasetReplaceFileItem item : this.getDatasetFilesReplacementList()) {
                itemDiff = "";
                itemDiff = System.lineSeparator() + " " + BundleUtil.getStringFromBundle("file.viewDiffDialog.fileName") + ": ";
                itemDiff += item.fdi.fileName1 != null ? item.fdi.fileName1 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable");
                itemDiff += " : ";
                itemDiff += item.fdi.fileName2 != null ? item.fdi.fileName2 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable");
                itemDiff += System.lineSeparator() + " " + BundleUtil.getStringFromBundle("file.viewDiffDialog.fileType") + ": ";
                itemDiff += item.fdi.fileType1 != null ? item.fdi.fileType1 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable");
                itemDiff += " : ";
                itemDiff += item.fdi.fileType2 != null ? item.fdi.fileType2 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable") + " ";
                itemDiff += System.lineSeparator() + " " + BundleUtil.getStringFromBundle("file.viewDiffDialog.fileSize") + ": ";
                itemDiff += item.fdi.fileSize1 != null ? item.fdi.fileSize1 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable");
                itemDiff += " : ";
                itemDiff += item.fdi.fileSize2 != null ? item.fdi.fileSize2 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable") + " ";
                itemDiff += System.lineSeparator() + " " + BundleUtil.getStringFromBundle("file.viewDiffDialog.category") + ": ";
                itemDiff += item.fdi.fileCat1 != null ? item.fdi.fileCat1 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable");
                itemDiff += " : ";
                itemDiff += item.fdi.fileCat2 != null ? item.fdi.fileCat2 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable") + " ";
                itemDiff += System.lineSeparator() + " " + BundleUtil.getStringFromBundle("file.viewDiffDialog.description") + ": ";
                itemDiff += item.fdi.fileDesc1 != null ? item.fdi.fileDesc1 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable");
                itemDiff += " : ";
                itemDiff += item.fdi.fileDesc2 != null ? item.fdi.fileDesc2 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable") + " ";
                itemDiff += System.lineSeparator() + " " + BundleUtil.getStringFromBundle("file.viewDiffDialog.provDescription") + ": ";
                itemDiff += item.fdi.fileProvFree1 != null ? item.fdi.fileProvFree1 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable");
                itemDiff += " : ";
                itemDiff += item.fdi.fileProvFree2 != null ? item.fdi.fileProvFree2 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable") + " ";
                itemDiff += System.lineSeparator() + " " + BundleUtil.getStringFromBundle("file.viewDiffDialog.fileAccess") + ": ";
                itemDiff += item.fdi.fileRest1 != null ? item.fdi.fileRest1 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable");
                itemDiff += " : ";
                itemDiff += item.fdi.fileRest2 != null ? item.fdi.fileRest2 : BundleUtil.getStringFromBundle("file.viewDiffDialog.notAvailable") + " ";
                fileReplaced += itemDiff;
            }           
            retVal += fileReplaced;
        }
        
        String termsOfUseDiff = System.lineSeparator() + "Terms of Use and Access Changes: "+ System.lineSeparator();
        
        if (!this.changedTermsAccess.isEmpty()){
            for (String[] blocks : changedTermsAccess) {
               String itemDiff = System.lineSeparator() + blocks[0] + " " + BundleUtil.getStringFromBundle("dataset.versionDifferences.changed") + " " + BundleUtil.getStringFromBundle("dataset.versionDifferences.from") + ": ";
               itemDiff += blocks[1];
               itemDiff += " " + BundleUtil.getStringFromBundle("dataset.versionDifferences.to") + ": "+  blocks[2];
               termsOfUseDiff +=itemDiff;
            }
            retVal +=termsOfUseDiff;
        }
        
        return retVal;
    }
    
    
    public class DifferenceSummaryGroup {
        
        private String displayName;
        private String type;
        private List<DifferenceSummaryItem> differenceSummaryItems;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<DifferenceSummaryItem> getDifferenceSummaryItems() {
            return differenceSummaryItems;
        }

        public void setDifferenceSummaryItems(List<DifferenceSummaryItem> differenceSummaryItems) {
            this.differenceSummaryItems = differenceSummaryItems;
        }
    }
    
        public class SummaryNote  {
         DatasetField dsfo;
         Integer added;
         Integer deleted; 
         Integer changed; 
         
         public void setDatasetField(DatasetField dsfIn){
             dsfo = dsfIn;
         }
         
         public DatasetField getDatasetField (){
             return dsfo;
         }
         
         public void setAdded(Integer addin){
             added = addin;
         }
         
         public Integer getAdded(){
             return added;
         }
         
        public void setDeleted(Integer delin){
             deleted = delin;
         }
         
         public Integer getDeleted(){
             return deleted;
        }
        
        public void setChanged(Integer changedin){
             changed = changedin;
         }
         
         public Integer getChanged(){
             return changed;
        } 
         
         
    }
    
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
    
    public class datasetReplaceFileItem {

        public datasetFileDifferenceItem getFdi() {
            return fdi;
        }

        public void setFdi(datasetFileDifferenceItem fdi) {
            this.fdi = fdi;
        }

        public String getLeftColumn() {
            return leftColumn;
        }

        public void setLeftColumn(String leftColumn) {
            this.leftColumn = leftColumn;
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
        private datasetFileDifferenceItem fdi;
        private String leftColumn;
        private String file1Id;
        private String file2Id;
        private DataFile.ChecksumType file1ChecksumType;
        private DataFile.ChecksumType file2ChecksumType;
        private String file1ChecksumValue;
        private String file2ChecksumValue;                
    }

    public class datasetFileDifferenceItem {

        public datasetFileDifferenceItem() {
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
        private String fileRest1;
        
        private String fileName2;
        private String fileType2;
        private String fileSize2;
        private String fileCat2;
        private String fileDesc2;
        private String fileProvFree2;
        private String fileRest2;

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

    public List<datasetFileDifferenceItem> getDatasetFilesDiffList() {
        return datasetFilesDiffList;
    }

    public void setDatasetFilesDiffList(List<datasetFileDifferenceItem> datasetFilesDiffList) {
        this.datasetFilesDiffList = datasetFilesDiffList;
    }

    /*
     * Static methods to compute which blocks have changes between the two
     * DatasetVersions. Currently used to assess whether 'system metadatablocks'
     * (protected by a separate key) have changed. (Simplified from the methods
     * above that track all the individual changes)
     * 
     */
    public static Set<MetadataBlock> getBlocksWithChanges(DatasetVersion newVersion, DatasetVersion originalVersion) {
        Set<MetadataBlock> changedBlockSet = new HashSet<MetadataBlock>();

        // Compare Data
        List<DatasetField> newDatasetFields = new LinkedList<DatasetField>(newVersion.getDatasetFields());
        if (originalVersion == null) {
            // Every field is new, just list blocks used
            Iterator<DatasetField> dsfnIter = newDatasetFields.listIterator();
            while (dsfnIter.hasNext()) {
                DatasetField dsfn = dsfnIter.next();
                if (!changedBlockSet.contains(dsfn.getDatasetFieldType().getMetadataBlock())) {
                    changedBlockSet.add(dsfn.getDatasetFieldType().getMetadataBlock());
                }
            }

        } else {
            List<DatasetField> originalDatasetFields = new LinkedList<DatasetField>(originalVersion.getDatasetFields());
            Iterator<DatasetField> dsfoIter = originalDatasetFields.listIterator();
            while (dsfoIter.hasNext()) {
                DatasetField dsfo = dsfoIter.next();
                boolean deleted = true;
                Iterator<DatasetField> dsfnIter = newDatasetFields.listIterator();

                while (dsfnIter.hasNext()) {
                    DatasetField dsfn = dsfnIter.next();
                    if (dsfo.getDatasetFieldType().equals(dsfn.getDatasetFieldType())) {
                        deleted = false;
                        if (!changedBlockSet.contains(dsfo.getDatasetFieldType().getMetadataBlock())) {
                            logger.fine("Checking " + dsfo.getDatasetFieldType().getName());
                            if (dsfo.getDatasetFieldType().isPrimitive()) {
                                if (fieldsAreDifferent(dsfo, dsfn, false)) {
                                    logger.fine("Adding block for " + dsfo.getDatasetFieldType().getName());
                                    changedBlockSet.add(dsfo.getDatasetFieldType().getMetadataBlock());
                                }
                            } else {
                                if (fieldsAreDifferent(dsfo, dsfn, true)) {
                                    logger.fine("Adding block for " + dsfo.getDatasetFieldType().getName());
                                    changedBlockSet.add(dsfo.getDatasetFieldType().getMetadataBlock());
                                }
                            }
                        }
                        dsfnIter.remove();
                        break; // if found go to next dataset field
                    }
                }

                if (deleted) {
                    logger.fine("Adding block for deleted " + dsfo.getDatasetFieldType().getName());
                    changedBlockSet.add(dsfo.getDatasetFieldType().getMetadataBlock());
                }
                dsfoIter.remove();
            }
            // Only fields left are non-matching ones but they may be empty
            for (DatasetField dsfn : newDatasetFields) {
                if (!dsfn.isEmpty()) {
                    logger.fine("Adding block for added " + dsfn.getDatasetFieldType().getName());
                    changedBlockSet.add(dsfn.getDatasetFieldType().getMetadataBlock());
                }
            }
        }
        return changedBlockSet;
    }

    private static boolean fieldsAreDifferent(DatasetField originalField, DatasetField newField, boolean compound) {
        String originalValue = "";
        String newValue = "";

        if (compound) {
            for (DatasetFieldCompoundValue datasetFieldCompoundValueOriginal : originalField
                    .getDatasetFieldCompoundValues()) {
                int loopIndex = 0;
                if (newField.getDatasetFieldCompoundValues().size() >= loopIndex + 1) {
                    for (DatasetField dsfo : datasetFieldCompoundValueOriginal.getChildDatasetFields()) {
                        if (!dsfo.getDisplayValue().isEmpty()) {
                            originalValue += dsfo.getDisplayValue() + ", ";
                        }
                    }
                    for (DatasetField dsfn : newField.getDatasetFieldCompoundValues().get(loopIndex)
                            .getChildDatasetFields()) {
                        if (!dsfn.getDisplayValue().isEmpty()) {
                            newValue += dsfn.getDisplayValue() + ", ";
                        }
                    }
                    if (!originalValue.trim().equals(newValue.trim())) {
                        return true;
                    }
                }
                loopIndex++;
            }
        } else {
            originalValue = originalField.getDisplayValue();
            newValue = newField.getDisplayValue();
            if (!originalValue.equalsIgnoreCase(newValue)) {
                return true;
            }
        }
        return false;
    }

    List<FileMetadata> getChangedVariableMetadata() {
        return changedVariableMetadata;
    }

    List<FileMetadata[]> getReplacedFiles() {
        return replacedFiles;
    }
    
    public JsonObjectBuilder getSummaryDifferenceAsJson(){

        JsonObjectBuilder jobVersion = new NullSafeJsonBuilder();

        JsonObjectBuilder jobDsfOnCreate = new NullSafeJsonBuilder();
        
        
        for (SummaryNote sn : this.summaryDataForNote) {
            jobDsfOnCreate.add( sn.getDatasetField().getDatasetFieldType().getDisplayName(), getSummaryNoteAsJson(sn));
        }
        
        if (!this.summaryDataForNote.isEmpty()){
            
            jobVersion.add("Citation Metadata", jobDsfOnCreate);
            
        }
        
        for (SummaryNote sn : this.getBlockDataForNote()){
             String mdbDisplayName = sn.getDatasetField().getDatasetFieldType().getMetadataBlock().getDisplayName();
             if (mdbDisplayName.equals("Citation Metadata")){
                 mdbDisplayName = "Additional Citation Metadata";
             }
             jobVersion.add( mdbDisplayName, getSummaryNoteAsJson(sn));    
        }   
        
        jobVersion.add("files", getFileSummaryAsJson());
        
        if (!this.changedTermsAccess.isEmpty()) {
            jobVersion.add("termsAccessChanged", true);
        } else{
            jobVersion.add("termsAccessChanged", false);
        }      
                
        return jobVersion;
    }
    
    private JsonObjectBuilder getSummaryNoteAsJson(SummaryNote sn){
        JsonObjectBuilder job = new NullSafeJsonBuilder();
        //job.add("datasetFieldType", sn.getDatasetField().getDatasetFieldType().getDisplayName());
        job.add("added", sn.added);
        job.add("deleted", sn.deleted);
        job.add("changed", sn.changed);
        return job;
    }
    
    private JsonObjectBuilder getFileSummaryAsJson(){
        JsonObjectBuilder job = new NullSafeJsonBuilder();
        
        if (!addedFiles.isEmpty()) {
            job.add("added", addedFiles.size());
           
        } else{
            job.add("added", 0);
        }
        
        if (!removedFiles.isEmpty()) {
            job.add("removed", removedFiles.size());
           
        } else{
            job.add("removed", 0);
        }
        
        if (!replacedFiles.isEmpty()) {
            job.add("replaced", replacedFiles.size());
           
        } else{
            job.add("replaced", 0);
        }
        
        if (!changedFileMetadata.isEmpty()) {
            job.add("changedFileMetaData", changedFileMetadata.size());
           
        } else{
            job.add("changedFileMetaData", 0);
        }
        
        if (!changedVariableMetadata.isEmpty()) {
            job.add("changedVariableMetadata", changedVariableMetadata.size());
           
        } else{
            job.add("changedVariableMetadata", 0);
        }

        return job;
    }
       
    public JsonObjectBuilder compareVersionsAsJson() {
        JsonObjectBuilder job = new NullSafeJsonBuilder();
        JsonObjectBuilder jobVersion = new NullSafeJsonBuilder();
        jobVersion.add("versionNumber", originalVersion.getFriendlyVersionNumber());
        jobVersion.add("lastUpdatedDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(originalVersion.getLastUpdateTime()));
        job.add("oldVersion", jobVersion);
        jobVersion = new NullSafeJsonBuilder();
        jobVersion.add("versionNumber", newVersion.getFriendlyVersionNumber());
        jobVersion.add("lastUpdatedDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(newVersion.getLastUpdateTime()));
        job.add("newVersion", jobVersion);

        if (!this.detailDataByBlock.isEmpty()) {
            JsonArrayBuilder jabMetadata = Json.createArrayBuilder();
            for (List<DatasetField[]> blocks : detailDataByBlock) {
                JsonObjectBuilder jobMetadata = new NullSafeJsonBuilder();
                JsonArrayBuilder jab = Json.createArrayBuilder();
                String blockDisplay = blocks.get(0)[0].getDatasetFieldType().getMetadataBlock().getDisplayName();
                for (DatasetField[] dsfArray : blocks) {
                    JsonObjectBuilder jb = new NullSafeJsonBuilder();
                    jb.add("fieldName", dsfArray[0].getDatasetFieldType().getTitle());
                    if (dsfArray[0].getDatasetFieldType().isPrimitive()) {
                        jb.add("oldValue", dsfArray[0].getRawValue());
                    } else {
                        jb.add("oldValue", dsfArray[0].getCompoundRawValue());
                    }
                    if (dsfArray[1].getDatasetFieldType().isPrimitive()) {
                        jb.add("newValue", dsfArray[1].getRawValue());
                    } else {
                        jb.add("newValue", dsfArray[1].getCompoundRawValue());
                    }
                    jab.add(jb);
                }
                jobMetadata.add("blockName", blockDisplay);
                jobMetadata.add("changed", jab);
                jabMetadata.add(jobMetadata);
            }
            job.add("metadataChanges", jabMetadata);
        }

        // Format added, removed, and modified files
        JsonArrayBuilder jabDiffFiles = Json.createArrayBuilder();
        if (!addedFiles.isEmpty()) {
            JsonArrayBuilder jab = Json.createArrayBuilder();
            addedFiles.forEach(f -> {
                jab.add(filesDiffJson(f));
            });
            job.add("filesAdded", jab);
        }
        if (!removedFiles.isEmpty()) {
            JsonArrayBuilder jab = Json.createArrayBuilder();
            removedFiles.forEach(f -> {
                jab.add(filesDiffJson(f));
            });
            job.add("filesRemoved", jab);
        }
        if (!replacedFiles.isEmpty()) {
            JsonArrayBuilder jabReplaced = Json.createArrayBuilder();
            replacedFiles.forEach(fm -> {
                if (fm.length == 2) {
                    JsonObjectBuilder jobReplaced = new NullSafeJsonBuilder();
                    jobReplaced.add("oldFile", filesDiffJson(fm[0]));
                    jobReplaced.add("newFile", filesDiffJson(fm[1]));
                    jabReplaced.add(jobReplaced);
                }
            });
            job.add("filesReplaced", jabReplaced);
        }
        if (!changedFileMetadata.isEmpty()) {
            changedFileMetadataDiff.entrySet().forEach(entry -> {
                JsonArrayBuilder jab = Json.createArrayBuilder();
                JsonObjectBuilder jobChanged = new NullSafeJsonBuilder();
                jobChanged.add("fileName", entry.getKey().getDataFile().getDisplayName());
                jobChanged.add(entry.getKey().getDataFile().getChecksumType().name(), entry.getKey().getDataFile().getChecksumValue());
                jobChanged.add("fileId", entry.getKey().getDataFile().getId());
                entry.getValue().entrySet().forEach(e -> {
                    JsonObjectBuilder jobDiffField = new NullSafeJsonBuilder();
                    jobDiffField.add("fieldName",e.getKey());
                    jobDiffField.add("oldValue",e.getValue().get(0));
                    jobDiffField.add("newValue",e.getValue().get(1));
                    jab.add(jobDiffField);
                });
                jobChanged.add("changed", jab);
                jabDiffFiles.add(jobChanged);
            });
            job.add("fileChanges", jabDiffFiles);
        }

        // Format Terms Of Access changes
        if (!changedTermsAccess.isEmpty()) {
            JsonObjectBuilder jobTOA = new NullSafeJsonBuilder();
            JsonArrayBuilder jab = Json.createArrayBuilder();
            changedTermsAccess.forEach(toa -> {
                JsonObjectBuilder jobValue = new NullSafeJsonBuilder();
                jobValue.add("fieldName",toa[0]);
                jobValue.add("oldValue",toa[1]);
                jobValue.add("newValue",toa[2]);
                jab.add(jobValue);
            });
            jobTOA.add("changed", jab);
            job.add("TermsOfAccess", jobTOA);
        }

        return job;
    }
    private JsonObjectBuilder filesDiffJson(FileMetadata fileMetadata) {
        NullSafeJsonBuilder job = new NullSafeJsonBuilder();
        DataFile df = fileMetadata.getDataFile();
        job.add("fileName", df.getDisplayName())
                .add("filePath", fileMetadata.getDirectoryLabel())
                .add(df.getChecksumType().name(), df.getChecksumValue())
                .add("type",df.getContentType())
                .add("fileId", df.getId())
                .add("description", fileMetadata.getDescription())
                .add("isRestricted", df.isRestricted());
        if (fileMetadata.getCategories() != null && !fileMetadata.getCategories().isEmpty()) {
            JsonArrayBuilder jabCategories = Json.createArrayBuilder();
            fileMetadata.getCategories().forEach(c -> jabCategories.add(c.getName()));
            job.add("categories", jabCategories);
        }
        if (df.getTags() != null && !df.getTags().isEmpty()) {
            JsonArrayBuilder jabTags = Json.createArrayBuilder();
            df.getTags().forEach(t -> jabTags.add(t.getTypeLabel()));
            job.add("tags", jabTags);
        }
        return job;
    }
}
