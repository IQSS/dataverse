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
    private  FileMetadata newFileMetadata;
    private  FileMetadata originalFileMetadata;   
    private boolean details = false;

    public String getDisplay() {
        String retval = "";
        if (this.newFileMetadata.getDataFile() == null && this.originalFileMetadata != null){
           return ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileRemoved");
        }
        if (this.originalFileMetadata == null &&  this.newFileMetadata.getDataFile() != null ){
           return ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileAdded");
        }
        if (this.newFileMetadata.getDataFile() == null && this.originalFileMetadata == null){
           return ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileNotInVersion");
        }
        //Check to see if File replaced
        if (this.originalFileMetadata != null && this.originalFileMetadata.getDataFile() != null 
                && this.newFileMetadata.getDataFile() != null 
                && !this.originalFileMetadata.getDataFile().equals(this.newFileMetadata.getDataFile())){
            retval =  ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileReplaced") + "; ";
        }
        //append replaced message with all other changes
        if (this.differenceSummaryGroups.isEmpty()){
            return retval + ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.noChanges");      
        } else {                
            return retval + trimResult(getSummaryGroupsDisplay());
        }
    }

    private String trimResult(String str){
        
            if (str.endsWith(" ")) {
                str = str.substring(0, str.length() - 1);
            }
            if (str.endsWith(";")) {
                str = str.substring(0, str.length() - 1);
            }
            if (str.endsWith(":")) {
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
        
        if (newFileMetadata.getDataFile() == null && originalFileMetadata == null){
            //File in neither version
            //Don't add any groups
        }
        
        if (newFileMetadata.getDataFile() == null && originalFileMetadata != null){
            //File Deleted
            updateDifferenceSummary(ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileGroupTitle"), "", 0, 0, 1, 0);
            return;
        }
        
        if (this.originalFileMetadata == null && this.newFileMetadata.getDataFile() != null ){
            //File Added
            updateDifferenceSummary(ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileGroupTitle"), "", 1, 0, 0, 0);
        }
        
        //Check to see if File replaced
        if (originalFileMetadata != null &&
                 newFileMetadata.getDataFile() != null && originalFileMetadata.getDataFile() != null &&!this.originalFileMetadata.getDataFile().equals(this.newFileMetadata.getDataFile())){
            updateDifferenceSummary(ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileGroupTitle"), "", 0, 0, 0, 1);
        }
        
        if ( originalFileMetadata != null) {
            if (!newFileMetadata.getLabel().equals(originalFileMetadata.getLabel())) {
                if (details) {
                    differenceDetailItems.add(new FileDifferenceDetailItem(ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileNameDetailTitle"), originalFileMetadata.getLabel(), newFileMetadata.getLabel()));
                }
                updateDifferenceSummary(ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileMetadataGroupTitle"), 
                        ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileNameDetailTitle"), 0, 1, 0, 0);
            }
        }
        if ( originalFileMetadata != null) {
            if (newFileMetadata.getDescription() != null
                    && originalFileMetadata.getDescription() != null
                    && !newFileMetadata.getDescription().equals(originalFileMetadata.getDescription())) {
                if (details) {
                    differenceDetailItems.add(new FileDifferenceDetailItem(ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.descriptionDetailTitle"), originalFileMetadata.getDescription(), newFileMetadata.getDescription()));
                }
                updateDifferenceSummary(ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileMetadataGroupTitle"), 
                        ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.descriptionDetailTitle"), 0, 1, 0, 0);
            }
            if (newFileMetadata.getDescription() != null
                    && originalFileMetadata.getDescription() == null
                    ) {
                if (details) {
                    differenceDetailItems.add(new FileDifferenceDetailItem(ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.descriptionDetailTitle"), "", newFileMetadata.getDescription()));
                }
                updateDifferenceSummary(ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileMetadataGroupTitle"), 
                        ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.descriptionDetailTitle"), 1, 0, 0, 0);
            }
            if (newFileMetadata.getDescription() == null
                    && originalFileMetadata.getDescription() != null
                    ) {
                if (details) {
                    differenceDetailItems.add(new FileDifferenceDetailItem(ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.descriptionDetailTitle"), originalFileMetadata.getDescription(), "" ));
                }
                updateDifferenceSummary(ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileMetadataGroupTitle"), 
                        ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.descriptionDetailTitle"), 0, 0, 1, 0);
            }
        }  
        if (originalFileMetadata != null) {
            /*
            get Tags differences
            */
            String value1 = originalFileMetadata.getCategoriesByName().toString();
            String value2 = newFileMetadata.getCategoriesByName().toString();
            if (value1 == null || value1.equals("") || value1.equals(" ")) {
                value1 = "";
            }
            if (value2 == null || value2.equals("") || value2.equals(" ")) {
                value2 = "";
            }

            if (!value1.equals(value2)) {
                int added = newFileMetadata.getCategoriesByName().size() - originalFileMetadata.getCategoriesByName().size();
                added = added < 0 ? 0 : added;
                int deleted = originalFileMetadata.getCategoriesByName().size() - newFileMetadata.getCategoriesByName().size();
                deleted = deleted < 0 ? 0 : deleted;
                if (originalFileMetadata.getCategoriesByName().size() == newFileMetadata.getCategoriesByName().size());
                int changed = originalFileMetadata.getCategoriesByName().size();
                updateDifferenceSummary(ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileTagsGroupTitle"), "", added, changed, deleted, 0, true);
            }
            
            /*
            Get Restriction Differences
            */
            value1 = originalFileMetadata.isRestricted() ? ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileRestricted") : ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileUnrestricted");
            value2 = newFileMetadata.isRestricted() ? ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileRestricted") : ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileUnrestricted");
            if (!value1.equals(value2)) {
                if (!value1.equals(value2)) {
                    updateDifferenceSummary(ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.fileAccessTitle"), value2, 0, 0, 0, 0);
                }
            }
        }
    }
    
     private void updateDifferenceSummary(String groupLabel, String itemLabel, int changed, int added, int deleted, int replaced) {
        updateDifferenceSummary(groupLabel, itemLabel, changed, added, deleted, replaced, false);
    }
    
    
    private void updateDifferenceSummary(String groupLabel, String itemLabel, int changed, int added, int deleted, int replaced, boolean multiple) {
        FileDifferenceSummaryGroup summaryGroup = new FileDifferenceSummaryGroup(groupLabel);
        FileDifferenceSummaryItem summaryItem = new FileDifferenceSummaryItem(itemLabel, changed, added, deleted, replaced, multiple);
        
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
        
        public String getDisplayValue(){
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
            String append = "";
            
            if (!multiple){
                append = changed == 1 ?   ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.actionChanged") + "; " : ";";
                append = added == 1 ?   ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.actionAdded") + "; " : append;
                append = deleted == 1 ?   ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.actionRemoved") + "; " : append;
                append = replaced == 1 ?   ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.actionReplaced") + "; " : append;
                return this.name + " " + append;
            } else {
                append = added >= 1 ? added + " " +   ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.actionAdded") + "; " : "";
                append += changed >= 1 ? changed + " " +  ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.actionChanged") + "; " : "";
                append +=  deleted >= 1 ? deleted + " " +  ResourceBundle.getBundle("Bundle").getString("file.versionDifferences.actionRemoved") + "; " : "";
                return this.name + " " + append;
            }           
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
