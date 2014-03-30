/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    private Map<MetadataBlock, List<DatasetFieldValue>> metadataBlocksForView = new HashMap();
    private Map<MetadataBlock, List<DatasetFieldValue>> metadataBlocksForEdit = new HashMap();

    public Map<MetadataBlock, List<DatasetFieldValue>> getMetadataBlocksForView() {
        return metadataBlocksForView;
    }

    public void setMetadataBlocksForView(Map<MetadataBlock, List<DatasetFieldValue>> metadataBlocksForView) {
        this.metadataBlocksForView = metadataBlocksForView;
    }

    public Map<MetadataBlock, List<DatasetFieldValue>> getMetadataBlocksForEdit() {
        return metadataBlocksForEdit;
    }

    public void setMetadataBlocksForEdit(Map<MetadataBlock, List<DatasetFieldValue>> metadataBlocksForEdit) {
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
        this.setSubjects(new ArrayList());
        this.setDisplaysAboveTabs(new ArrayList());

        // loop through vaues to get fields for view mode
        for (DatasetFieldValue dsfv : datasetVersion.getDatasetFieldValues()) {
            //Special Handling for various fields displayed above tabs in dataset page view.
            if (dsfv.getDatasetFieldType().getName().equals(DatasetFieldConstant.title)) {
                setTitle(dsfv);
            } else if (dsfv.getDatasetFieldType().getName().equals(DatasetFieldConstant.descriptionText)) {
                setDescription(dsfv);
            } else if (dsfv.getDatasetFieldType().getName().equals(DatasetFieldConstant.author)) {
                for (DatasetFieldCompoundValue authorValue : dsfv.getDatasetFieldCompoundValues()) {
                    DatasetAuthor datasetAuthor = new DatasetAuthor();
                    for (DatasetFieldValue subField : authorValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorName)) {
                            datasetAuthor.setName(subField);
                        }
                        if (subField.getDatasetField().getName().equals(DatasetFieldConstant.authorAffiliation)) {
                            datasetAuthor.setAffiliation(subField);
                        }
                    }
                    this.getDatasetAuthors().add(datasetAuthor);
                }

            } else if (dsfv.getDatasetFieldType().getName().equals(DatasetFieldConstant.keyword)) {
                setDatasetKeywords(dsfv);
            } else if (dsfv.getDatasetFieldType().getName().equals(DatasetFieldConstant.subject)) {
                for (ControlledVocabularyValue cvv : dsfv.getControlledVocabularyValues()) {
                    this.getSubjects().add(cvv.getStrValue());
                }
            } else if (dsfv.getDatasetFieldType().getName().equals(DatasetFieldConstant.publication)) {
                //Special handling for Related Publications
                // Treated as below the tabs for editing, but must get first value for display above tabs    
                if (this.datasetRelPublications.isEmpty()) {/*
                     Collection childVals = dsfv.getChildDatasetFieldValues();
                     DatasetRelPublication datasetRelPublication = new DatasetRelPublication();
                     if (childVals != null) {
                     for (Object cv : childVals) {
                     DatasetFieldValue cvo = (DatasetFieldValue) cv;
                     if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.publicationCitation)) {
                     datasetRelPublication.setText(cvo.getStrValue());
                     }
                     if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.publicationIDNumber)) {
                     datasetRelPublication.setIdNumber(cvo.getStrValue());
                     }
                     if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.publicationIDType)) {
                     datasetRelPublication.setIdType(cvo.getStrValue());
                     }
                     if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.publicationURL)) {
                     datasetRelPublication.setUrl(cvo.getStrValue());
                     }
                     //if no pub URL is available get distributor url
                     if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.distributorURL) && datasetRelPublication.getUrl().isEmpty()) {
                     datasetRelPublication.setUrl(cvo.getStrValue());
                     }
                     }
                     }
                     this.getDatasetRelPublications().add(datasetRelPublication);*/

                }

            } else if (dsfv.getDatasetFieldType().getName().equals(DatasetFieldConstant.notesText)) {
                this.setNotesText(dsfv.getValue());
            }/* else if (dsfv.getDatasetField().isDisplayOnCreate()
             && ((!dsfv.getDatasetField().isHasParent() && !dsfv.getDatasetField().isHasChildren() && dsfv.getValue() != null && !dsfv.getValue().isEmpty())
             || (dsfv.getDatasetField().isHasChildren() && !dsfv.isChildEmpty()))) {
             //any fields designated as show on create

             this.getDisplaysAboveTabs().add(dsfv);
             if (dsfv.getChildDatasetFieldValues() != null && !dsfv.getChildDatasetFieldValues().isEmpty()) {
             for (DatasetFieldValue dsfvChild : dsfv.getChildDatasetFieldValues()) {
             this.getDisplaysAboveTabs().add(dsfvChild);
             }
             }
             }*/

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

    private DatasetFieldValue title;

    public DatasetFieldValue getTitle() {
        return title;
    }

    public void setTitle(DatasetFieldValue title) {
        this.title = title;
    }

    private List<String> subjects = new ArrayList();

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    private List<DatasetAuthor> datasetAuthors = new ArrayList();

    public List<DatasetAuthor> getDatasetAuthors() {
        return datasetAuthors;
    }

    public void setDatasetAuthors(List<DatasetAuthor> datasetAuthors) {
        this.datasetAuthors = datasetAuthors;
    }

    private DatasetFieldValue description;

    public DatasetFieldValue getDescription() {
        return description;
    }

    public void setDescription(DatasetFieldValue description) {
        this.description = description;
    }

    private DatasetFieldValue datasetKeywords;

    public DatasetFieldValue getDatasetKeywords() {
        return datasetKeywords;
    }

    public void setDatasetKeywords(DatasetFieldValue datasetKeywords) {
        this.datasetKeywords = datasetKeywords;
    }

    private List<DatasetFieldValue> displaysAboveTabs = new ArrayList();

    public List<DatasetFieldValue> getDisplaysAboveTabs() {
        return displaysAboveTabs;
    }

    public void setDisplaysAboveTabs(List<DatasetFieldValue> displaysAboveTabs) {
        this.displaysAboveTabs = displaysAboveTabs;
    }

    private List<DatasetRelPublication> datasetRelPublications;

    public List<DatasetRelPublication> getDatasetRelPublications() {
        return datasetRelPublications;
    }

    public void setDatasetRelPublications(List<DatasetRelPublication> datasetRelPublications) {
        this.datasetRelPublications = datasetRelPublications;
    }

    private String notesText;

    public String getNotesText() {
        return notesText;
    }

    public void setNotesText(String notesText) {
        this.notesText = notesText;
    }

    public String getRelPublicationCitation() {
        if (!this.datasetRelPublications.isEmpty()) {
            return this.getDatasetRelPublications().get(0).getText();
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

        if (!StringUtil.isEmpty(getDistributionDate())) {
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            str += getYearForCitation(getDistributionDate());
        } else if (!StringUtil.isEmpty(getProductionDate())) {

            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            str += getYearForCitation(getProductionDate());
//getting 2014 for citation 
//while still possible that prod date and deposit date are empty               
        } else {
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            str += "2014";

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

    public String getKeywordsStr() {
        String str = "";
        for (String keyword : this.getDatasetKeywords().getValues()) {
            str += (str.length() > 1 ? "; " : " ") + keyword;
        }
        return str;
    }

    public String getSubjectStr() {
        String str = "";
        for (String sa : this.getSubjects()) {
            if (str.trim().length() > 1) {
                str += "; ";
            }
            if (sa != null && sa.toString() != null && !sa.toString().trim().isEmpty()) {
                str += sa.toString().trim();
            }
        }
        return str;
    }

    public String getProductionDate() {
        for (DatasetFieldValue dsfv : datasetVersion.getDatasetFieldValues()) {
            if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.productionDate)) {
                return dsfv.getValue();
            }
        }
        return "";
    }

    public String getDistributionDate() {
        for (DatasetFieldValue dsfv : datasetVersion.getDatasetFieldValues()) {
            if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.distributionDate)) {
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
            List<DatasetFieldValue> datasetFieldsForView = new ArrayList();
            List<DatasetFieldValue> datasetFieldsForEdit = new ArrayList();
            for (DatasetFieldValue dsf : datasetVersion.getDatasetFields()) {
                if (dsf.getDatasetField().getMetadataBlock().equals(mdb)) {
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


        /*
         for (MetadataBlock mdb : this.datasetVersion.getDataset().getOwner().getMetadataBlocks()) {
         Map<DatasetField, List<DatasetFieldValue>> mdbMap = new TreeMap(
         new Comparator<DatasetField>() {
         public int compare(DatasetField d1, DatasetField d2) {
         int a = d1.getDisplayOrder();
         int b = d2.getDisplayOrder();
         return Integer.valueOf(a).compareTo(Integer.valueOf(b));
         }
         });
         boolean addBlock = false;
         for (DatasetFieldValue dsfv : datasetVersion.getDatasetFieldValues()) {
         if (dsfv.getDatasetField().isHasChildren() || !StringUtil.isEmpty(dsfv.getValue())) {
         if (dsfv.getDatasetField().getMetadataBlock().equals(mdb)) {
         List<DatasetFieldValue> dsfvValues = mdbMap.get(dsfv.getDatasetField());
         if (dsfvValues == null) {
         dsfvValues = new ArrayList();
         }
         dsfvValues.add(dsfv);
         mdbMap.put(dsfv.getDatasetField(), dsfvValues);
         if (!dsfv.getDatasetField().isHasChildren()) {
         addBlock = true;
         }
         }
         }
         }

         if (addBlock) {
         //metadataBlocksForView.put(mdb, mdbMap);
         }
         }
         */
        /*
         for (MetadataBlock mdb : this.datasetVersion.getDataset().getOwner().getMetadataBlocks()) {
         Map<DatasetField, List<DatasetFieldValue>> mdbMap = new TreeMap(
         new Comparator<DatasetField>() {
         public int compare(DatasetField d1, DatasetField d2) {
         int a = d1.getDisplayOrder();
         int b = d2.getDisplayOrder();
         return Integer.valueOf(a).compareTo(Integer.valueOf(b));
         }
         });

         for (DatasetFieldValue dsfv : datasetVersion.getDatasetFieldValues()) {
         if (dsfv.getDatasetField().getMetadataBlock().equals(mdb)) {
         List<DatasetFieldValue> dsfvValues = mdbMap.get(dsfv.getDatasetField());
         if (dsfvValues == null) {
         dsfvValues = new ArrayList();
         }
         dsfvValues.add(dsfv);
         mdbMap.put(dsfv.getDatasetField(), dsfvValues);
         }
         }

         //metadataBlocksForEdit.put(mdb, mdbMap);
         }
         */
    }

}
