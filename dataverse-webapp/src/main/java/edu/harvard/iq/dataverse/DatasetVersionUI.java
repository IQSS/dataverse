/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.common.MarkupChecker;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRelPublication;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import org.apache.commons.collections4.SetUtils;

import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author skraffmiller
 */
@ViewScoped
public class DatasetVersionUI implements Serializable {
    
    @EJB
    private DataverseFieldTypeInputLevelServiceBean dataverseFieldTypeInputLevelService;

    public DatasetVersionUI() {
    }

    public enum MetadataBlocksMode {
        FOR_VIEW,
        FOR_EDIT
    }
    
    private Map<MetadataBlock, List<DatasetField>> metadataBlocks = new HashMap<>();

    public Map<MetadataBlock, List<DatasetField>> getMetadataBlocks() {
        return metadataBlocks;
    }

    public DatasetVersionUI initDatasetVersionUI(DatasetVersion datasetVersion, MetadataBlocksMode mode) {
        /*takes in the values of a dataset version 
         and apportions them into lists for 
         viewing and editng in the dataset page.
         */

        setDatasetVersion(datasetVersion);
        //this.setDatasetAuthors(new ArrayList());
        this.setDatasetRelPublications(new ArrayList<>());

        // loop through vaues to get fields for view mode
        for (DatasetField dsf : datasetVersion.getDatasetFields()) {
            //Special Handling for various fields displayed above tabs in dataset page view.
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.title)) {
                setTitle(dsf);
            } else if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.description)) {
                setDescription(dsf);
                String descriptionString = "";
                if (dsf.getDatasetFieldCompoundValues() != null && dsf.getDatasetFieldCompoundValues().get(0) != null) {
                    DatasetFieldCompoundValue descriptionValue = dsf.getDatasetFieldCompoundValues().get(0);
                    for (DatasetField subField : descriptionValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.descriptionText) && !subField.isEmptyForDisplay()) {
                            descriptionString = subField.getValue();
                        }
                    }
                }
                setDescriptionDisplay(MarkupChecker.sanitizeBasicHTML(descriptionString));
            } else if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.keyword)) {
                setKeyword(dsf);
                String keywordString = "";
                for (DatasetFieldCompoundValue keywordValue : dsf.getDatasetFieldCompoundValues()) {
                    for (DatasetField subField : keywordValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.keywordValue) && !subField.isEmptyForDisplay()) {
                            if (keywordString.isEmpty()) {
                                keywordString = subField.getValue();
                            } else {
                                keywordString += ", " + subField.getValue();
                            }
                        }
                    }
                }
                setKeywordDisplay(keywordString);
            } else if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.subject) && !dsf.isEmptyForDisplay()) {
                setSubject(dsf);
            } else if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.notesText) && !dsf.isEmptyForDisplay()) {
                this.setNotes(dsf);
            } else if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.publication)) {
                //Special handling for Related Publications
                // Treated as below the tabs for editing, but must get first value for display above tabs    
                if (this.datasetRelPublications.isEmpty()) {
                    for (DatasetFieldCompoundValue relPubVal : dsf.getDatasetFieldCompoundValues()) {
                        DatasetRelPublication datasetRelPublication = new DatasetRelPublication();
                        datasetRelPublication.setTitle(dsf.getDatasetFieldType().getTitle());
                        datasetRelPublication.setDescription(dsf.getDatasetFieldType().getDescription());
                        for (DatasetField subField : relPubVal.getChildDatasetFields()) {
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.publicationCitation)) {
                                datasetRelPublication.setText(subField.getValue());
                            }
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.publicationIDNumber)) {
                                datasetRelPublication.setIdNumber(subField.getValue());
                            }
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.publicationIDType)) {
                                datasetRelPublication.setIdType(subField.getValue());
                            }
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.publicationURL)) {
                                datasetRelPublication.setUrl(subField.getValue());
                            }
                        }
                        this.getDatasetRelPublications().add(datasetRelPublication);
                    }
                }
            }
        }

        Set<MetadataBlock> dataverseMetadataBlocks = new HashSet<>(this.getDataset().getOwner().getRootMetadataBlocks());
        long  metadataBlocksDataverseId = this.getDataset().getOwner().getMetadataRootId();
        
        
        List<DatasetField> datasetFields = datasetVersion.getDatasetFields();
        if (mode == MetadataBlocksMode.FOR_EDIT) {
            datasetFields = createBlankDatasetFields(datasetVersion, dataverseMetadataBlocks);
        }
        sortDatasetFieldsRecursively(datasetFields);
        datasetVersion.setDatasetFields(datasetFields);

        updateDatasetFieldIncludeFlag(datasetVersion, metadataBlocksDataverseId);
        
        if (mode == MetadataBlocksMode.FOR_EDIT) {
            metadataBlocks = buildMetadataBlocksForEdit(datasetVersion, dataverseMetadataBlocks);
            
        } else if (mode == MetadataBlocksMode.FOR_VIEW) {
            metadataBlocks = buildMetadataBlocksForView(datasetVersion);
        }

        return this;
    }
    
    private Dataset getDataset() {
        return this.datasetVersion.getDataset();
    }

    private DatasetVersion datasetVersion;

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    private DatasetField title;
    private DatasetField description;
    private DatasetField keyword;
    private DatasetField subject;
    private DatasetField notes;
    private String keywordDisplay;

    public String getKeywordDisplay() {
        return keywordDisplay;
    }

    public void setKeywordDisplay(String keywordDisplay) {
        this.keywordDisplay = keywordDisplay;
    }

    private String descriptionDisplay;

    public String getDescriptionDisplay() {
        return descriptionDisplay;
    }

    public void setDescriptionDisplay(String descriptionDisplay) {
        this.descriptionDisplay = descriptionDisplay;
    }


    private List<DatasetRelPublication> datasetRelPublications;

    public DatasetField getTitle() {
        return title;
    }

    public void setTitle(DatasetField title) {
        this.title = title;
    }

    public DatasetField getDescription() {
        return description;
    }

    public void setDescription(DatasetField description) {
        this.description = description;
    }

    public DatasetField getKeyword() {
        return keyword;
    }

    public void setKeyword(DatasetField keyword) {
        this.keyword = keyword;
    }

    public DatasetField getSubject() {
        return subject;
    }

    public void setSubject(DatasetField subject) {
        this.subject = subject;
    }

    public DatasetField getNotes() {
        return notes;
    }

    public void setNotes(DatasetField notes) {
        this.notes = notes;
    }


    public List<DatasetRelPublication> getDatasetRelPublications() {
        return datasetRelPublications;
    }

    public void setDatasetRelPublications(List<DatasetRelPublication> datasetRelPublications) {
        this.datasetRelPublications = datasetRelPublications;
    }


    public String getRelPublicationCitation() {
        if (this.datasetRelPublications != null && !this.datasetRelPublications.isEmpty()) {
            return this.getDatasetRelPublications().get(0).getText();
        } else {
            return "";
        }
    }

    public String getRelPublicationId() {
        if (!this.datasetRelPublications.isEmpty()) {
            if (!(this.getDatasetRelPublications().get(0).getIdNumber() == null) && !this.getDatasetRelPublications().get(0).getIdNumber().isEmpty()) {
                return this.getDatasetRelPublications().get(0).getIdType() + ": " + this.getDatasetRelPublications().get(0).getIdNumber();
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

    public String getRelPublicationUrl() {
        if (!this.datasetRelPublications.isEmpty()) {
            return this.getDatasetRelPublications().get(0).getUrl();
        } else {
            return "";
        }
    }

    public String getUNF() {
        //todo get UNF to calculate and display here.
        return "";
    }

    //TODO - make sure getCitation works
    private String getYearForCitation(String dateString) {
        //get date to first dash only
        if (dateString.contains("-")) {
            return dateString.substring(0, dateString.indexOf("-"));
        }
        return dateString;
    }

    public String getReleaseDate() {
        if (datasetVersion.getReleaseTime() != null) {
            Date relDate = datasetVersion.getReleaseTime();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(relDate);
            return Integer.toString(calendar.get(Calendar.YEAR));
        }
        return "";
    }

    public String getCreateDate() {
        if (datasetVersion.getCreateTime() != null) {
            Date relDate = datasetVersion.getCreateTime();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(relDate);
            return Integer.toString(calendar.get(Calendar.YEAR));
        }
        return "";
    }

    public String getProductionDate() {
        for (DatasetField dsfv : datasetVersion.getDatasetFields()) {
            if (dsfv.getDatasetFieldType().getName().equals(DatasetFieldConstant.productionDate)) {
                return dsfv.getValue();
            }
        }
        return "";
    }

    public String getDistributionDate() {
        for (DatasetField dsfv : datasetVersion.getDatasetFields()) {
            if (dsfv.getDatasetFieldType().getName().equals(DatasetFieldConstant.distributionDate)) {
                return dsfv.getValue();
            }
        }
        return "";
    }

    /***
    *
    * Note: Updated to retrieve DataverseFieldTypeInputLevel objects in single query
    *
    */
   private void updateDatasetFieldIncludeFlag(DatasetVersion datasetVersion, long metadataBlocksDataverseId) {
       
       List<DatasetField> datasetFields = datasetVersion.getFlatDatasetFields();
       List<Long> datasetFieldTypeIds = new ArrayList<>();
       
       for (DatasetField dsf: datasetFields) {
           datasetFieldTypeIds.add(dsf.getDatasetFieldType().getId());
       }
       
       List<Long> fieldTypeIdsToHide = dataverseFieldTypeInputLevelService
               .findByDataverseIdAndDatasetFieldTypeIdList(metadataBlocksDataverseId, datasetFieldTypeIds).stream()
               .filter(inputLevel -> !inputLevel.isInclude())
               .map(inputLevel -> inputLevel.getDatasetFieldType().getId())
               .collect(Collectors.toList());
       
       
       for (DatasetField dsf: datasetFields) {
           dsf.setInclude(true);
           if (fieldTypeIdsToHide.contains(dsf.getDatasetFieldType().getId())) {
               dsf.setInclude(false);
           }
       }
   }
    
    
    // TODO: clean up init methods and get them to work, cascading all the way down.
    // right now, only work for one level of compound objects
    private DatasetField createBlankChildDatasetFields(DatasetField dsf) {
        if (dsf.getDatasetFieldType().isCompound()) {
            for (DatasetFieldCompoundValue cv : dsf.getDatasetFieldCompoundValues()) {
                // for each compound value; check the datasetfieldTypes associated with its type
                Set<DatasetFieldType> allChildFieldTypes = new HashSet<>(dsf.getDatasetFieldType().getChildDatasetFieldTypes());
                
                Set<DatasetFieldType> alreadyPresentChildFieldTypes = new HashSet<>();
                for (DatasetField df: cv.getChildDatasetFields()) {
                    DatasetFieldType dsft = df.getDatasetFieldType();
                    alreadyPresentChildFieldTypes.add(dsft);
                }
                
                Set<DatasetFieldType> missingChildFieldTypes = SetUtils.difference(allChildFieldTypes, alreadyPresentChildFieldTypes);
                
                missingChildFieldTypes.forEach(dsft -> {
                    cv.getChildDatasetFields().add(DatasetField.createNewEmptyChildDatasetField(dsft, cv));
                });
            }
        }

        return dsf;
    }

    private List<DatasetField> createBlankDatasetFields(DatasetVersion datasetVersion, Set<MetadataBlock> dataverseMetadataBlocks) {
        //retList - Return List of values
        List<DatasetField> retList = new ArrayList<>();
        for (DatasetField dsf : datasetVersion.getDatasetFields()) {
            retList.add(createBlankChildDatasetFields(dsf));
        }

        Set<DatasetFieldType> allFieldTypes = new HashSet<>();
        for (MetadataBlock mdb: dataverseMetadataBlocks) {
            for (DatasetFieldType dsft: mdb.getDatasetFieldTypes()) {
                if (!dsft.isSubField()) {
                    allFieldTypes.add(dsft);
                }
            }
        }
        
        Set<DatasetFieldType> alreadyPresentFieldTypes = new HashSet<>();
        for (DatasetField df: datasetVersion.getDatasetFields()) {
            alreadyPresentFieldTypes.add(df.getDatasetFieldType());
        }
        
        Set<DatasetFieldType> missingFieldTypes = SetUtils.difference(allFieldTypes, alreadyPresentFieldTypes);
        
        missingFieldTypes.forEach(dsft -> {
            retList.add(DatasetField.createNewEmptyDatasetField(dsft, datasetVersion));
        });

        return retList;
    }

    private void sortDatasetFieldsRecursively(List<DatasetField> dsfList) {
        for (DatasetField dsf : dsfList) {
            if (dsf.getDatasetFieldType().isCompound()) {
                for (DatasetFieldCompoundValue cv : dsf.getDatasetFieldCompoundValues()) {
                    sortDatasetFields(cv.getChildDatasetFields());
                }
            }
        }
        sortDatasetFields(dsfList);
        
    }
    private void sortDatasetFields(List<DatasetField> dsfList) {
        Collections.sort(dsfList, Comparator.comparing(df -> df.getDatasetFieldType().getDisplayOrder()));
    }

    private Map<MetadataBlock, List<DatasetField>> buildMetadataBlocksForView(DatasetVersion datasetVersion) {
        Map<MetadataBlock, List<DatasetField>> metadataBlocksForView = new HashMap<>();
        
        for (DatasetField dsf: datasetVersion.getDatasetFields()) {
            if (!dsf.isEmptyForDisplay()) {
                MetadataBlock metadataBlockOfField = dsf.getDatasetFieldType().getMetadataBlock();
                metadataBlocksForView.putIfAbsent(metadataBlockOfField, new ArrayList<>());
                metadataBlocksForView.get(metadataBlockOfField).add(dsf);
            }
        }
        
        return metadataBlocksForView;
    }
    
    private Map<MetadataBlock, List<DatasetField>> buildMetadataBlocksForEdit(DatasetVersion datasetVersion,
            Set<MetadataBlock> dataverseMetadataBlocks) {
        Map<MetadataBlock, List<DatasetField>> metadataBlocksForEdit = new HashMap<>();
        
        for (DatasetField dsf: datasetVersion.getDatasetFields()) {
            MetadataBlock metadataBlockOfField = dsf.getDatasetFieldType().getMetadataBlock();
            
            if (!dsf.isEmptyForDisplay() || dataverseMetadataBlocks.contains(metadataBlockOfField)) {
                metadataBlocksForEdit.putIfAbsent(metadataBlockOfField, new ArrayList<>());
                metadataBlocksForEdit.get(metadataBlockOfField).add(dsf);
            }
        }
        
        updateEmptyAndHasRequiredFlag(metadataBlocksForEdit);
        
        return metadataBlocksForEdit;
    }
    
    private void updateEmptyAndHasRequiredFlag(Map<MetadataBlock, List<DatasetField>> metadataBlocksForEdit) {
        for (MetadataBlock mdb : metadataBlocksForEdit.keySet()) {
            mdb.setEmpty(allFieldsEmpty(metadataBlocksForEdit.get(mdb)));
            mdb.setHasRequired(anyFieldsRequired(metadataBlocksForEdit.get(mdb)));
        }
    }

    private boolean anyFieldsRequired(List<DatasetField> datasetFields) {
        return datasetFields.stream().anyMatch(DatasetField::isRequired);
    }
    
    private boolean allFieldsEmpty(List<DatasetField> datasetFields) {
        return datasetFields.stream().allMatch(DatasetField::isEmptyForDisplay);
    }
}
