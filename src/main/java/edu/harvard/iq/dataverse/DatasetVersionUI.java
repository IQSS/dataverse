/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.ejb.EJB;

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

    public String getCitation() {
        return getCitation(false);
    }

    public String getCitation(boolean isOnlineVersion) {

        Dataset dataset = getDataset();

        String str = "";

        boolean includeAffiliation = false;
        String authors = getAuthorsStr(includeAffiliation);
        if (!StringUtil.isEmpty(authors)) {
            str += authors;
        }

        if (!StringUtil.isEmpty(getReleaseDate())) {
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            str += getReleaseDate();
        } else if (!StringUtil.isEmpty(getCreateDate())) {
            if (!StringUtil.isEmpty(str)) {
                str += ", <";
            }
            str += getYearForCitation(getCreateDate()) + ">";             
        } 

        if (!StringUtil.isEmpty(getTitle().getValue())) {
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            str += "\"" + getTitle().getValue() + "\"";
        }

        if (!StringUtil.isEmpty(dataset.getIdentifier())) {
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            if (isOnlineVersion) {
                str += "<a href=\"" + dataset.getPersistentURL() + "\">" + dataset.getIdentifier() + "</a>";
            } else {
                str += dataset.getPersistentURL();
            }
        }

        //Get root dataverse name for Citation
        Dataverse root = getDatasetVersion().getDataset().getOwner();
        while (root.getOwner() != null) {
            root = root.getOwner();
        }
        String rootDataverseName = root.getName();
        if (!StringUtil.isEmpty(rootDataverseName)) {
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            str += " " + rootDataverseName + " [Publisher] ";
        }

        if (this.getDatasetVersion().getVersionNumber() != null) {
            str += " V" + this.getDatasetVersion().getVersionNumber();
            str += " [Version]";
        }
        /*UNF is not calculated yet
         if (!StringUtil.isEmpty(getUNF())) {
         if (!StringUtil.isEmpty(str)) {
         str += " ";
         }
         str += getUNF();
         }
         String distributorNames = getDistributorNames();
         if (distributorNames.trim().length() > 0) {
         str += " " + distributorNames;
         str += " [Distributor]";
         }*/
        return str;
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

    public void setMetadataValueBlocks(DatasetVersion datasetVersion) {
        //TODO: A lot of clean up on the logic of this method
        metadataBlocksForView.clear();
        metadataBlocksForEdit.clear();
        for (MetadataBlock mdb : this.datasetVersion.getDataset().getOwner().getMetadataBlocks()) {
            List<DatasetField> datasetFieldsForView = new ArrayList();
            List<DatasetField> datasetFieldsForEdit = new ArrayList();
            for (DatasetField dsf : datasetVersion.getDatasetFields()) {
                if (dsf.getDatasetFieldType().getMetadataBlock().equals(mdb)) {
                    datasetFieldsForEdit.add(dsf);
                    if (!dsf.isEmpty()) {
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
