/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author skraffmiller
 */
public class DatasetVersionUI {

    public DatasetVersionUI() {
    }

    private Map<MetadataBlock, List<DatasetField>> metadataBlocksForView = new HashMap();
    private Map<MetadataBlock, List<DatasetField>> metadataBlocksForEdit = new HashMap();

    public Map<MetadataBlock, List<DatasetField>> getMetadataBlocksForView() {
        return metadataBlocksForView;
    }

    public void setMetadataBlocksForView(Map<MetadataBlock, List<DatasetField>> metadataBlocksForView) {
        this.metadataBlocksForView = metadataBlocksForView;
    }

    public Map<MetadataBlock, List<DatasetField>> getMetadataBlocksForEdit() {
        return metadataBlocksForEdit;
    }

    public void setMetadataBlocksForEdit(Map<MetadataBlock, List<DatasetField>> metadataBlocksForEdit) {
        this.metadataBlocksForEdit = metadataBlocksForEdit;
    }

    public DatasetVersionUI(DatasetVersion datasetVersion) {
        /*takes in the values of a dataset version 
         and apportions them into lists for 
         viewing and editng in the dataset page.
         */
        setDatasetVersion(datasetVersion);
        this.setDatasetAuthors(new ArrayList());
        this.setDatasetRelPublications(new ArrayList());

        // loop through vaues to get fields for view mode
        for (DatasetField dsf : datasetVersion.getDatasetFields()) {
            //Special Handling for various fields displayed above tabs in dataset page view.
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.title)) {
                setTitle(dsf);
            } else if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.descriptionText)) {
                setDescription(dsf);
            } else if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.keyword)) {
                setKeyword(dsf);
            } else if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.subject)) {
                setSubject(dsf);
            } else if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.notesText)) {
                this.setNotes(dsf);                
            } else if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.author)) {
                for (DatasetFieldCompoundValue authorValue : dsf.getDatasetFieldCompoundValues()) {
                    DatasetAuthor datasetAuthor = new DatasetAuthor();
                    for (DatasetField subField : authorValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorName)) {
                            datasetAuthor.setName(subField);
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorAffiliation)) {
                            datasetAuthor.setAffiliation(subField);
                        }
                    }
                    this.getDatasetAuthors().add(datasetAuthor);
                }                
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
        
        datasetVersion.setDatasetFields(initDatasetFields());
        setMetadataValueBlocks(datasetVersion);
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
            
    private List<DatasetAuthor> datasetAuthors = new ArrayList();    
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


    public List<DatasetAuthor> getDatasetAuthors() {
        return datasetAuthors;
    }

    public void setDatasetAuthors(List<DatasetAuthor> datasetAuthors) {
        this.datasetAuthors = datasetAuthors;
    }


    public List<DatasetRelPublication> getDatasetRelPublications() {
        return datasetRelPublications;
    }

    public void setDatasetRelPublications(List<DatasetRelPublication> datasetRelPublications) {
        this.datasetRelPublications = datasetRelPublications;
    }



    public String getRelPublicationCitation() {
        if (!this.datasetRelPublications.isEmpty()) {
            return this.getDatasetRelPublications().get(0).getText();
        } else {
            return "";
        }
    }

    public String getRelPublicationId() {
        if (!this.datasetRelPublications.isEmpty()) {
            if (!(this.getDatasetRelPublications().get(0).getIdNumber() == null)  && !this.getDatasetRelPublications().get(0).getIdNumber().isEmpty()){
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
        if (dateString.indexOf("-") > -1) {
            return dateString.substring(0, dateString.indexOf("-"));
        }
        return dateString;
    }


    public String getAuthorsStr() {
        return getAuthorsStr(true);
    }

    public String getAuthorsStr(boolean affiliation) {
        String str = "";
        for (DatasetAuthor sa : this.getDatasetAuthors()) {
            //Fix for RedMine 3731 if Author name is just one character.
            if (str.trim().length() > 0) {
                str += "; ";
            }
            if (sa.getName() != null && !StringUtil.isEmpty(sa.getName().getValue())) {
                str += sa.getName().getValue();
            }
            if (affiliation) {
                if (sa.getAffiliation() != null && !StringUtil.isEmpty(sa.getAffiliation().getValue())) {
                    str += " (" + sa.getAffiliation().getValue() + ")";
                }
            }
        }
        return str;
    }

    public String getReleaseDate() {
        if (datasetVersion.getReleaseTime() != null) {
            Date relDate = datasetVersion.getReleaseTime();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(relDate);
            return new Integer(calendar.get(Calendar.YEAR)).toString();
        }
        return "";
    }
    
    public String getCreateDate() {
        if (datasetVersion.getCreateTime() != null) {
            Date relDate = datasetVersion.getCreateTime();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(relDate);
            return new Integer(calendar.get(Calendar.YEAR)).toString();
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
    
 // TODO: clean up init methods and get them to work, cascading all the way down.
    // right now, only work for one level of compound objects
    private DatasetField initDatasetField(DatasetField dsf) {
        if (dsf.getDatasetFieldType().isCompound()) {
            for (DatasetFieldCompoundValue cv : dsf.getDatasetFieldCompoundValues()) {
                // for each compound value; check the datasetfieldTypes associated with its type
                for (DatasetFieldType dsfType : dsf.getDatasetFieldType().getChildDatasetFieldTypes()) {
                    boolean add = true;
                    for (DatasetField subfield : cv.getChildDatasetFields()) {
                        if (dsfType.equals(subfield.getDatasetFieldType())) {
                            add = false;
                            break;
                        }
                    }

                    if (add) {
                        cv.getChildDatasetFields().add(DatasetField.createNewEmptyChildDatasetField(dsfType, cv));
                    }
                }
                
                sortDatasetFields(cv.getChildDatasetFields());
            }
        }

        return dsf;
    }

    private List<DatasetField> initDatasetFields() {
        //retList - Return List of values
        List<DatasetField> retList = new ArrayList();
        for (DatasetField dsf : this.datasetVersion.getDatasetFields()) {
            retList.add(initDatasetField(dsf));
        }
     

        //Test to see that there are values for 
        // all fields in this dataset via metadata blocks
        //only add if not added above
        for (MetadataBlock mdb : this.getDataset().getOwner().getMetadataBlocks()) {
            for (DatasetFieldType dsfType : mdb.getDatasetFieldTypes()) {
                if (!dsfType.isSubField()) {
                    boolean add = true;
                    //don't add if already added as a val
                    for (DatasetField dsf : retList) {
                        if (dsfType.equals(dsf.getDatasetFieldType())) {
                            add = false;
                            break;
                        }
                    }

                    if (add) {
                        retList.add(DatasetField.createNewEmptyDatasetField(dsfType, this.datasetVersion));
                    }
                }
            }
        }

        //sort via display order on dataset field
        Collections.sort(retList, new Comparator<DatasetField>() {
            public int compare(DatasetField d1, DatasetField d2) {
                int a = d1.getDatasetFieldType().getDisplayOrder();
                int b = d2.getDatasetFieldType().getDisplayOrder();
                return Integer.valueOf(a).compareTo(Integer.valueOf(b));
            }
        });

        return sortDatasetFields(retList);
    }  
    
    private List<DatasetField> sortDatasetFields (List<DatasetField> dsfList) {
        Collections.sort(dsfList, new Comparator<DatasetField>() {
            public int compare(DatasetField d1, DatasetField d2) {
                int a = d1.getDatasetFieldType().getDisplayOrder();
                int b = d2.getDatasetFieldType().getDisplayOrder();
                return Integer.valueOf(a).compareTo(Integer.valueOf(b));
            }
        });
        return dsfList;
    }    

    public void setMetadataValueBlocks(DatasetVersion datasetVersion) {
        //TODO: A lot of clean up on the logic of this method
        metadataBlocksForView.clear();
        metadataBlocksForEdit.clear();
        for (MetadataBlock mdb : this.datasetVersion.getDataset().getOwner().getMetadataBlocks()) {
            mdb.setEmpty(true);
            List<DatasetField> datasetFieldsForView = new ArrayList();
            List<DatasetField> datasetFieldsForEdit = new ArrayList();
            for (DatasetField dsf : datasetVersion.getDatasetFields()) {
                if (dsf.getDatasetFieldType().getMetadataBlock().equals(mdb)) {
                    datasetFieldsForEdit.add(dsf);
                    if (!dsf.isEmpty()) {
                        mdb.setEmpty(false);
                        datasetFieldsForView.add(dsf);
                    }
                }
            }
            
            if (!datasetFieldsForView.isEmpty()) {
                metadataBlocksForView.put(mdb, datasetFieldsForView);
            }
            if (!datasetFieldsForEdit.isEmpty()) {
                metadataBlocksForEdit.put(mdb, datasetFieldsForEdit);
            }            
        }
    }

}
