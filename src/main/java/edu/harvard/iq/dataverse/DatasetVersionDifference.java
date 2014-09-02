package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author skraffmiller
 */
public class DatasetVersionDifference {

    private DatasetVersion newVersion;
    private DatasetVersion originalVersion;
    private List<List> detailDataByBlock = new ArrayList<>();
    private List<datasetFileDifferenceItem> datasetFilesDiffList;
    private List<FileMetadata> addedFiles = new ArrayList();
    private List<FileMetadata> removedFiles = new ArrayList();
    private List<FileMetadata> changedFileMetadata = new ArrayList();
    private List<Object[]> summaryDataForNote = new ArrayList();
    private List<Object[]> blockDataForNote = new ArrayList();
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
            if (dsfn.getDatasetFieldType().isPrimitive()) {
                for (DatasetField dsfo : originalVersion.getDatasetFields()) {
                    if (dsfo.getDatasetFieldType().equals(dsfn.getDatasetFieldType())) {
                        added = false;
                        break;
                    }
                }
                if (added && !dsfn.isEmpty()) {
                    updateBlockSummary(dsfn, dsfn.getDatasetFieldValues().size(), 0, 0);
                    addToSummary(null, dsfn);
                }
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
        for (List blockList : detailDataByBlock) {
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
        Collections.sort(detailDataByBlock, new Comparator<List>() {
            public int compare(List l1, List l2) {
                DatasetField dsfa[] = (DatasetField[]) l1.get(0);
                DatasetField dsfb[] = (DatasetField[]) l2.get(0);
                int a = dsfa[0].getDatasetFieldType().getMetadataBlock().getId().intValue();
                int b = dsfb[0].getDatasetFieldType().getMetadataBlock().getId().intValue();
                return Integer.valueOf(a).compareTo(Integer.valueOf(b));
            }
        });
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
                    } else if (!newValue.isEmpty() && !originalValue.equals(newValue)) {
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
                if (nString != null && oString != null && !oString.equals(nString)) {
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
                    datasetFilesDiffList.add(fdi);
                }
                i++;
                j++;
            } else if (fm2.getDataFile().getId() != null && fm1.getDataFile().getId().compareTo(fm2.getDataFile().getId()) > 0) {
                datasetFileDifferenceItem fdi = selectFileMetadataDiffs(null, fm2);
                fdi.setFileId(fm2.getDataFile().getId().toString());
                datasetFilesDiffList.add(fdi);

                j++;
            } else if (fm2.getDataFile().getId() == null || fm1.getDataFile().getId().compareTo(fm2.getDataFile().getId()) < 0) {
                datasetFileDifferenceItem fdi = selectFileMetadataDiffs(fm1, null);
                fdi.setFileId(fm1.getDataFile().getId().toString());
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
        value1 = fm1.getCategory();
        value2 = fm2.getCategory();

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

            fdi.setFileCat1(fm1.getCategory());
            fdi.setFileDesc1(fm1.getDescription());

            fdi.setFile2Empty(true);

        } else if (fm1 == null) {
            fdi.setFile1Empty(true);

            fdi.setFileName2(fm2.getLabel());
            fdi.setFileType2(fm2.getDataFile().getFriendlyType());
            //fdi.setFileSize2(FileUtil.byteCountToDisplaySize(new File(fm2.getStudyFile().getFileSystemLocation()).length()));
            fdi.setFileCat2(fm2.getCategory());
            fdi.setFileDesc2(fm2.getDescription());

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
            value1 = fm1.getCategory();
            value2 = fm2.getCategory();

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
        }
        return fdi;
    }

    public class datasetFileDifferenceItem {

        public datasetFileDifferenceItem() {
        }

        private String fileId;

        private String fileName1;
        private String fileType1;
        private String fileSize1;
        private String fileCat1;
        private String fileDesc1;

        private String fileName2;
        private String fileType2;
        private String fileSize2;
        private String fileCat2;
        private String fileDesc2;

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
}
