/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.BundleUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author skraffmi
 */
public final class FileVersionDifference {
    
    private  FileMetadata newFileMetadata;
    private  FileMetadata originalFileMetadata;   
    private boolean details = false;
    private boolean same = false;



    private List<FileDifferenceSummaryGroup> differenceSummaryGroups = new ArrayList<>();   
    private List<FileDifferenceDetailItem> differenceDetailItems = new ArrayList<>();
    
    public FileVersionDifference(FileMetadata newFileMetadata, FileMetadata originalFileMetadata) {

       this(newFileMetadata, originalFileMetadata, false);
           
    } 
    
    public FileVersionDifference(FileMetadata newFileMetadata, FileMetadata originalFileMetadata, boolean details) {

        this.newFileMetadata = newFileMetadata;
        this.originalFileMetadata = originalFileMetadata;
        this.details = details;

        this.same = compareMetadata(newFileMetadata, originalFileMetadata);
        //Compare versions - File Metadata first

    } 
    
    
    public boolean compareMetadata(FileMetadata newFileMetadata, FileMetadata originalFileMetadata) {

        /*
        This  method both determines if there has been a change in file metadata between the two versions supplied 
        and it updates the FileVersionDifference object which is used to display the differences on the dataset versions tab.
        The return value is used by the index service bean tomark whether a file needs to be re-indexed in the context of a dataset update.
        When there are changes (after v4.19)to the file metadata data model this method must be updated.
        retVal of True means metadatas are equal.        
        */        
        
        boolean retVal = true;
        if (newFileMetadata.getDataFile() == null && originalFileMetadata == null){
            //File in neither version
            //Don't add any groups
            return true;
        }
        
        if (newFileMetadata.getDataFile() == null && originalFileMetadata != null){
            //File Deleted
            if (details) {
                updateDifferenceSummary("", BundleUtil.getStringFromBundle("file.versionDifferences.fileGroupTitle"), 0, 0, 1, 0);
            }
            return false;
        }

        if (this.originalFileMetadata == null && this.newFileMetadata.getDataFile() != null){
            //File Added
            if (!details) {
                return false;
            }
            retVal = false;
            updateDifferenceSummary("", BundleUtil.getStringFromBundle("file.versionDifferences.fileGroupTitle"), 1, 0, 0, 0);
        }
        
        if (originalFileMetadata != null) {
            // Check to see if File replaced
            if (newFileMetadata.getDataFile() != null && originalFileMetadata.getDataFile() != null && !this.originalFileMetadata.getDataFile().equals(this.newFileMetadata.getDataFile())) {
                if (!details)
                    return false;
                updateDifferenceSummary("", BundleUtil.getStringFromBundle("file.versionDifferences.fileGroupTitle"), 0, 0, 0, 1);
                retVal = false;
            }

            /*
             * Get Restriction Differences
             */
            if (originalFileMetadata.isRestricted() != newFileMetadata.isRestricted()) {
                if (details) {
                    String value2 = newFileMetadata.isRestricted() ? BundleUtil.getStringFromBundle("file.versionDifferences.fileRestricted") : BundleUtil.getStringFromBundle("file.versionDifferences.fileUnrestricted");
                    updateDifferenceSummary(BundleUtil.getStringFromBundle("file.versionDifferences.fileAccessTitle"), value2, 0, 0, 0, 0);
                }
                retVal = false;
            }

            if (!newFileMetadata.getLabel().equals(originalFileMetadata.getLabel())) {
                if (details) {
                    differenceDetailItems.add(new FileDifferenceDetailItem(BundleUtil.getStringFromBundle("file.versionDifferences.fileNameDetailTitle"), originalFileMetadata.getLabel(), newFileMetadata.getLabel()));
                } else{
                    return false;
                }
                updateDifferenceSummary(BundleUtil.getStringFromBundle("file.versionDifferences.fileMetadataGroupTitle"),
                        BundleUtil.getStringFromBundle("file.versionDifferences.fileNameDetailTitle"), 0, 1, 0, 0);
                retVal = false;
            }

            //Description differences
            if (newFileMetadata.getDescription() != null
                    && originalFileMetadata.getDescription() != null
                    && !newFileMetadata.getDescription().equals(originalFileMetadata.getDescription())) {
                if (details) {
                    differenceDetailItems.add(new FileDifferenceDetailItem(BundleUtil.getStringFromBundle("file.versionDifferences.descriptionDetailTitle"), originalFileMetadata.getDescription(), newFileMetadata.getDescription()));
                } else {
                    return false;
                }
                updateDifferenceSummary(BundleUtil.getStringFromBundle("file.versionDifferences.fileMetadataGroupTitle"),
                        BundleUtil.getStringFromBundle("file.versionDifferences.descriptionDetailTitle"), 0, 1, 0, 0);
                retVal = false;
            }
            if (newFileMetadata.getDescription() != null
                    && originalFileMetadata.getDescription() == null
                    ) {
                if (details) {
                    differenceDetailItems.add(new FileDifferenceDetailItem(BundleUtil.getStringFromBundle("file.versionDifferences.descriptionDetailTitle"), "", newFileMetadata.getDescription()));
                } else {
                    return false;
                }
                updateDifferenceSummary(BundleUtil.getStringFromBundle("file.versionDifferences.fileMetadataGroupTitle"),
                        BundleUtil.getStringFromBundle("file.versionDifferences.descriptionDetailTitle"), 1, 0, 0, 0);
                retVal = false;
            }
            if (newFileMetadata.getDescription() == null
                    && originalFileMetadata.getDescription() != null
                    ) {
                if (details) {
                    differenceDetailItems.add(new FileDifferenceDetailItem(BundleUtil.getStringFromBundle("file.versionDifferences.descriptionDetailTitle"), originalFileMetadata.getDescription(), "" ));
                } else {
                    return false;
                }
                updateDifferenceSummary(BundleUtil.getStringFromBundle("file.versionDifferences.fileMetadataGroupTitle"),
                        BundleUtil.getStringFromBundle("file.versionDifferences.descriptionDetailTitle"), 0, 0, 1, 0);
                retVal = false;
            }
            //Provenance Description differences
            if ((newFileMetadata.getProvFreeForm() != null && !newFileMetadata.getProvFreeForm().isEmpty())
                    && (originalFileMetadata.getProvFreeForm() != null && !originalFileMetadata.getProvFreeForm().isEmpty())
                    && !newFileMetadata.getProvFreeForm().equals(originalFileMetadata.getProvFreeForm())) {
                if (details) {
                    differenceDetailItems.add(new FileDifferenceDetailItem(BundleUtil.getStringFromBundle("file.versionDifferences.provenanceDetailTitle"), originalFileMetadata.getProvFreeForm(), newFileMetadata.getProvFreeForm()));
                } else {
                    return false;
                }
                updateDifferenceSummary(BundleUtil.getStringFromBundle("file.versionDifferences.fileMetadataGroupTitle"),
                        BundleUtil.getStringFromBundle("file.versionDifferences.provenanceDetailTitle"), 0, 1, 0, 0);
                retVal = false;
            }
            if ((newFileMetadata.getProvFreeForm() != null && !newFileMetadata.getProvFreeForm().isEmpty())
                    && (originalFileMetadata.getProvFreeForm() == null || originalFileMetadata.getProvFreeForm().isEmpty())
                    ) {
                if (details) {
                    differenceDetailItems.add(new FileDifferenceDetailItem(BundleUtil.getStringFromBundle("file.versionDifferences.provenanceDetailTitle"), "", newFileMetadata.getProvFreeForm()));
                } else {
                    return false;
                }
                updateDifferenceSummary(BundleUtil.getStringFromBundle("file.versionDifferences.fileMetadataGroupTitle"),
                        BundleUtil.getStringFromBundle("file.versionDifferences.provenanceDetailTitle"), 1, 0, 0, 0);
                retVal = false;
            }
            if ((newFileMetadata.getProvFreeForm() == null || newFileMetadata.getProvFreeForm().isEmpty())
                    && (originalFileMetadata.getProvFreeForm() != null && !originalFileMetadata.getProvFreeForm().isEmpty())
                    ) {
                if (details) {
                    differenceDetailItems.add(new FileDifferenceDetailItem(BundleUtil.getStringFromBundle("file.versionDifferences.provenanceDetailTitle"), originalFileMetadata.getProvFreeForm(), "" ));
                } else {
                    return false;
                }
                updateDifferenceSummary(BundleUtil.getStringFromBundle("file.versionDifferences.fileMetadataGroupTitle"),
                        BundleUtil.getStringFromBundle("file.versionDifferences.provenanceDetailTitle"), 0, 0, 1, 0);
                retVal = false;
            }
            /*
            get Tags differences
            */
            String value1 = originalFileMetadata.getCategoriesByName().toString();
            String value2 = newFileMetadata.getCategoriesByName().toString();
            if (value1 == null || value1.isEmpty() || value1.equals(" ")) {
                value1 = "";
            }
            if (value2 == null || value2.isEmpty() || value2.equals(" ")) {
                value2 = "";
            }

            if (!value1.equals(value2)) {
                if (!details) {
                    return false;
                }
                int added = 0;
                int deleted = 0;
                
                added = newFileMetadata.getCategoriesByName().stream().map((tag) -> {
                    boolean found = false;
                    for (String tagOld : originalFileMetadata.getCategoriesByName() ){
                        if (tag.equals(tagOld)){
                            found = true;
                            break;
                        }
                    }
                    return found;
                }).filter((found) -> (!found)).map((_item) -> 1).reduce(added, Integer::sum);
                
                for (String tag : originalFileMetadata.getCategoriesByName()) {
                    boolean found = false;
                    for (String tagNew : newFileMetadata.getCategoriesByName() ){
                        if (tag.equals(tagNew)){
                            found = true;
                            break;
                        }
                    }
                    if (!found){
                        deleted++;
                    }
                }
                if (added > 0){
                    updateDifferenceSummary(BundleUtil.getStringFromBundle("file.versionDifferences.fileTagsGroupTitle"), "", added, 0, 0, 0, true);
                }
                if (deleted > 0){
                    updateDifferenceSummary(BundleUtil.getStringFromBundle("file.versionDifferences.fileTagsGroupTitle"), "", 0, 0, deleted, 0, true);
                }
                retVal = false;
            }

        }
        return retVal;
    }
    
     private void updateDifferenceSummary(String groupLabel, String itemLabel, int added, int changed, int deleted, int replaced) {
        updateDifferenceSummary(groupLabel, itemLabel, added, changed, deleted, replaced, false);
    }
    
    
    private void updateDifferenceSummary(String groupLabel, String itemLabel, int added, int changed, int deleted, int replaced, boolean multiple) {
        FileDifferenceSummaryGroup summaryGroup = new FileDifferenceSummaryGroup(groupLabel);
        FileDifferenceSummaryItem summaryItem = new FileDifferenceSummaryItem(itemLabel, added, changed, deleted, replaced, multiple);
        
        if (!this.differenceSummaryGroups.contains(summaryGroup)) {    
            summaryGroup.getFileDifferenceSummaryItems().add(summaryItem);
            this.differenceSummaryGroups.add(summaryGroup);
        } else {
            this.differenceSummaryGroups.stream().filter((test) -> (test.equals(summaryGroup))).forEach((test) -> {
                test.getFileDifferenceSummaryItems().add(summaryItem);
            });
        }
    }
    
    public FileMetadata getNewFileMetadata(){
        return this.newFileMetadata;
    }
    
    public void setNewFileMetadata(FileMetadata in){
         this.newFileMetadata = in;
    }

        public FileMetadata getOriginalFileMetadata() {
        return originalFileMetadata;
    }

    public void setOriginalFileMetadata(FileMetadata originalFileMetadata) {
        this.originalFileMetadata = originalFileMetadata;
    }
    
    public boolean isSame() {
        return same;
    }

    public void setSame(boolean same) {
        this.same = same;
    }
    
    
    public List<FileDifferenceSummaryGroup> getDifferenceSummaryGroups() {
        return differenceSummaryGroups;
    }

    public void setDifferenceSummaryGroups(List<FileDifferenceSummaryGroup> differenceSummaryGroups) {
        this.differenceSummaryGroups = differenceSummaryGroups;
    }

    public  class FileDifferenceSummaryGroup {



        private String name;
        private List<FileDifferenceSummaryItem> fileDifferenceSummaryItems;
        
        public FileDifferenceSummaryGroup(String name) {
            this.name = name;
            this.fileDifferenceSummaryItems = new ArrayList<>();
            
        }
        
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<FileDifferenceSummaryItem> getFileDifferenceSummaryItems() {
            return fileDifferenceSummaryItems;
        }

        public void setFileDifferenceSummaryItems(List<FileDifferenceSummaryItem> fileDifferenceSummaryItems) {
            this.fileDifferenceSummaryItems = fileDifferenceSummaryItems;
        }
        
        @Override
        public String toString() {
            
            String retval = getName();
            if (!retval.isEmpty()){
                retval += ": ";
            }
            
            for (FileDifferenceSummaryItem item : this.fileDifferenceSummaryItems){
                retval += " " + item.toString();
            }
            
            return retval;
        }
        
        
        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + Objects.hashCode(this.fileDifferenceSummaryItems);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final FileDifferenceSummaryGroup other = (FileDifferenceSummaryGroup) obj;
            return Objects.equals(this.name, other.name);
        }
    }
    
    public final class FileDifferenceDetailItem{
        private String displayName;
        private String originalValue;
        private String newValue;

        public FileDifferenceDetailItem(String displayName, String originalValue, String newValue) {
            this.displayName = displayName;
            this.originalValue = originalValue;
            this.newValue = newValue;
        }
        
        
        
        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getOriginalValue() {
            return originalValue;
        }

        public void setOriginalValue(String originalValue) {
            this.originalValue = originalValue;
        }

        public String getNewValue() {
            return newValue;
        }

        public void setNewValue(String newValue) {
            this.newValue = newValue;
        }

    }
    
    
    
    public class FileDifferenceSummaryItem{


        private String name;
        private int added;
        private int changed;
        private int deleted;
        private int replaced;
        private boolean multiple;
        
        public FileDifferenceSummaryItem(String name, int added, int changed, int deleted, int replaced, boolean multiple) {
            this.name = name;
            this.added = added;
            this.changed = changed;
            this.deleted = deleted;
            this.replaced = replaced;
            this.multiple = multiple;
        }
        
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAdded() {
            return added;
        }

        public void setAdded(int added) {
            this.added = added;
        }

        public int getChanged() {
            return changed;
        }

        public void setChanged(int changed) {
            this.changed = changed;
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
    
}
