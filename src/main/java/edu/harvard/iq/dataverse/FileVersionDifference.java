/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 *
 * @author skraffmi
 */
public class FileVersionDifference {
    
    private final DatasetVersion newVersion;
    private final DatasetVersion originalVersion;
    private final FileMetadata newFileMetadata;
    private final FileMetadata originalFileMetadata;   
    private boolean details = false;

    public String getDisplay() {
        String retval = "";
        if (this.newFileMetadata == null){
           return ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileRemoved") + ".";
        }
        if (this.originalFileMetadata == null){
           return ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileAdded")+ ".";
        }
        //Check to see if File replaced
        if (!this.originalFileMetadata.getDataFile().equals(this.newFileMetadata.getDataFile())){
                retval =  ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileReplaced") + "; ";
        }
        //append replaced message with all other changes
        if (this.differenceSummaryGroups.isEmpty()){
            return retval + ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.noChanges")+ ".";      
        } else {                
            return retval + trimResult(getSummaryGroupsDisplay())+ ".";
        }

    }
    
    
    private String trimResult(String str){
        
            if (str.endsWith(" ")) {
                str = str.substring(0, str.length() - 1);
            }
            if (str.endsWith(";")) {
                str = str.substring(0, str.length() - 1);
            }
            return str;
        
    }

    private List<FileDifferenceSummaryGroup> differenceSummaryGroups = new ArrayList();
    
    private List<FileDifferenceDetailItem> differenceDetailItems = new ArrayList();
    
    private String getSummaryGroupsDisplay(){
        String retVal = "";
        for (FileDifferenceSummaryGroup group: differenceSummaryGroups){
             retVal+= group.toString();
        }
        return retVal;
    }
    
    public FileVersionDifference(FileMetadata newFileMetadata, FileMetadata originalFileMetadata) {

       this(newFileMetadata, originalFileMetadata, false);
           

    } 
    
    public FileVersionDifference(FileMetadata newFileMetadata, FileMetadata originalFileMetadata, boolean details) {

        this.newFileMetadata = newFileMetadata;
        this.originalFileMetadata = originalFileMetadata;
        this.details = details;
        
        if (newFileMetadata == null) {
            this.newVersion = null;
        } else {
            this.newVersion = newFileMetadata.getDatasetVersion();
        }
        if (originalFileMetadata == null) {
            this.originalVersion = null;
        } else {
            this.originalVersion = originalFileMetadata.getDatasetVersion();
        }

        compareMetadata(newFileMetadata, originalFileMetadata);
        //Compare versions - File Metadata first

    } 
    
    
    private void compareMetadata(FileMetadata newFileMetadata, FileMetadata originalFileMetadata ){
        if (newFileMetadata != null && originalFileMetadata != null) {
            if (!newFileMetadata.getLabel().equals(originalFileMetadata.getLabel())) {
                if (details) {
                    differenceDetailItems.add(new FileDifferenceDetailItem("Label", originalFileMetadata.getLabel(), newFileMetadata.getLabel()));
                }
                updateDifferenceSummary("File Metadata", "File Name", 1, 0, 0, 0);
            }
        }
        if (newFileMetadata != null && originalFileMetadata != null) {
            if (newFileMetadata.getDescription() != null
                    && originalFileMetadata.getDescription() != null
                    && !newFileMetadata.getDescription().equals(originalFileMetadata.getDescription())) {
                if (details) {
                    differenceDetailItems.add(new FileDifferenceDetailItem("Description", originalFileMetadata.getDescription(), newFileMetadata.getDescription()));
                }
                updateDifferenceSummary("File Metadata", "Description", 1, 0, 0, 0);
            }
            if (newFileMetadata.getDescription() != null
                    && originalFileMetadata.getDescription() == null
                    ) {
                if (details) {
                    differenceDetailItems.add(new FileDifferenceDetailItem("Description", "", newFileMetadata.getDescription()));
                }
                updateDifferenceSummary("File Metadata", "Description", 0, 1, 0, 0);
            }
            if (newFileMetadata.getDescription() == null
                    && originalFileMetadata.getDescription() != null
                    ) {
                if (details) {
                    differenceDetailItems.add(new FileDifferenceDetailItem("Description", originalFileMetadata.getDescription(), "" ));
                }
                updateDifferenceSummary("File Metadata", "Description", 0, 0, 1, 0);
            }
        }  
        if (newFileMetadata != null && originalFileMetadata != null) {

            String value1 = originalFileMetadata.getCategoriesByName().toString();
            String value2 = newFileMetadata.getCategoriesByName().toString();
            if (value1 == null || value1.equals("") || value1.equals(" ")) {
                value1 = "";
            }
            if (value2 == null || value2.equals("") || value2.equals(" ")) {
                value2 = "";
            }

            if (!value1.equals(value2)) {
                updateDifferenceSummary("File Tags", "", 1, 0, 0, 0);
            }
        }
    }
    
    
    private void updateDifferenceSummary(String groupLabel, String itemLabel, int changed, int added, int deleted, int replaced) {
        FileDifferenceSummaryGroup summaryGroup = new FileDifferenceSummaryGroup(groupLabel);
        FileDifferenceSummaryItem summaryItem = new FileDifferenceSummaryItem(itemLabel, changed, added, deleted, replaced, false);
        
        if (!this.differenceSummaryGroups.contains(summaryGroup)) {
            summaryGroup.getFileDifferenceSummaryItems().add(summaryItem);
            this.differenceSummaryGroups.add(summaryGroup);
        } else {
            this.differenceSummaryGroups.stream().filter((test) -> (test.equals(summaryGroup))).forEach((test) -> {
                test.getFileDifferenceSummaryItems().add(summaryItem);
            });
        }
    }
    


    
    private void addDifferenceSummaryItem(){
        
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
            this.fileDifferenceSummaryItems = new ArrayList();
            
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
            String retval = getName() + ": ";
            
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
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            return true;
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
        
        @Override
        public String toString(){
            if (!multiple){
                return this.name + " " +  ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.metadataChanged") + "; ";
            }
            
            return this.name;
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
