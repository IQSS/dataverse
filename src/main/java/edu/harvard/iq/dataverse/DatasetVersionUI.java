/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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

    @EJB
    DataverseServiceBean dataverseService;

    public DatasetVersionUI() {
    }

    private Map<MetadataBlock, Map<DatasetField, List<DatasetFieldValue>>> metadataBlocksForView = new HashMap();
    private Map<MetadataBlock, Map<DatasetField, List<DatasetFieldValue>>> metadataBlocksForEdit = new HashMap();

    public Map<MetadataBlock, Map<DatasetField, List<DatasetFieldValue>>> getMetadataBlocksForView() {
        return metadataBlocksForView;
    }

    public void setMetadataBlocksForView(Map<MetadataBlock, Map<DatasetField, List<DatasetFieldValue>>> metadataBlocksForView) {
        this.metadataBlocksForView = metadataBlocksForView;
    }

    public Map<MetadataBlock, Map<DatasetField, List<DatasetFieldValue>>> getMetadataBlocksForEdit() {
        return metadataBlocksForEdit;
    }

    public void setMetadataBlocksForEdit(Map<MetadataBlock, Map<DatasetField, List<DatasetFieldValue>>> metadataBlocksForEdit) {
        this.metadataBlocksForEdit = metadataBlocksForEdit;
    }

    public DatasetVersionUI(DatasetVersion datasetVersion) {
        /*takes in the values of a dataset version 
         and apportions them into lists for 
         viewing and editng in the dataset page.
         */
        setDatasetVersion(datasetVersion);
        this.setDatasetAuthors(new ArrayList());
        this.setDatasetKeywords(new ArrayList());
        this.setDatasetRelPublications(new ArrayList());
        this.setSubjects(new ArrayList());
        this.setDisplaysAboveTabs(new ArrayList());

        // loop through vaues to get fields for view mode
        for (DatasetFieldValue dsfv : datasetVersion.getDatasetFieldValues()) {
            if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.title)) {
                setTitle(dsfv);
            } else if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.descriptionText)) {
                setDescription(dsfv);
            } else if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.author)) {
                Collection childVals = dsfv.getChildDatasetFieldValues();
                DatasetAuthor datasetAuthor = new DatasetAuthor();
                if (childVals != null) {
                    for (Object cv : childVals) {
                        DatasetFieldValue cvo = (DatasetFieldValue) cv;
                        if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.authorName)) {
                            datasetAuthor.setName(cvo);
                        }
                        if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.authorAffiliation)) {
                            datasetAuthor.setAffiliation(cvo);
                        }
                    }
                }
                this.getDatasetAuthors().add(datasetAuthor);
            } else if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.keyword)) {
                this.getDatasetKeywords().add(dsfv);
            } else if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.subject)) {
                this.getSubjects().add(dsfv.getStrValue());
            } else if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.publication)) {
                //Special handling for Related Publications
                // Treated as below the tabs for editing, but must get first value for display above tabs    
                if (this.datasetRelPublications.isEmpty()) {
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
                                datasetRelPublication.setUrl(null);
                            }
                            if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.distributorURL)) {
                                datasetRelPublication.setUrl(cvo.getStrValue());
                            }
                        }
                    }
                    this.getDatasetRelPublications().add(datasetRelPublication);
                }

            } else if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.notesText)) {
                this.setNotesText(dsfv.getStrValue());
            } else if (dsfv.getDatasetField().isDisplayOnCreate()
                    && ((!dsfv.getDatasetField().isHasParent() && !dsfv.getDatasetField().isHasChildren() && dsfv.getStrValue() != null && !dsfv.getStrValue().isEmpty())
                    || (dsfv.getDatasetField().isHasChildren() && !dsfv.isChildEmpty()))) {
                //any fields designated as show on create

                this.getDisplaysAboveTabs().add(dsfv);
                if (dsfv.getChildDatasetFieldValues() != null && !dsfv.getChildDatasetFieldValues().isEmpty()) {
                    for (DatasetFieldValue dsfvChild : dsfv.getChildDatasetFieldValues()) {
                        this.getDisplaysAboveTabs().add(dsfvChild);
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

    private List<DatasetFieldValue> datasetKeywords = new ArrayList();

    public List<DatasetFieldValue> getDatasetKeywords() {
        return datasetKeywords;
    }

    public void setDatasetKeywords(List<DatasetFieldValue> datasetKeywords) {
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
        } else {
            if (!StringUtil.isEmpty(getProductionDate())) {
                if (!StringUtil.isEmpty(str)) {
                    str += ", ";
                }
                str += getYearForCitation(getProductionDate());
            }
        }

        if (!StringUtil.isEmpty(getTitle().getStrValue())) {
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            str += "\"" + getTitle().getStrValue() + "\"";
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
            if (str.trim().length() > 1) {
                str += "; ";
            }
            if (sa.getName() != null && !StringUtil.isEmpty(sa.getName().getStrValue())) {
                str += sa.getName().getStrValue();
            }
            if (affiliation) {
                if (sa.getAffiliation() != null && !StringUtil.isEmpty(sa.getAffiliation().getStrValue())) {
                    str += " (" + sa.getAffiliation().getStrValue() + ")";
                }
            }
        }
        return str;
    }

    public String getKeywordsStr() {
        String str = "";
        for (DatasetFieldValue sa : this.getDatasetKeywords()) {
            if (str.trim().length() > 1) {
                str += "; ";
            }
            if (sa.getStrValue() != null && sa.getStrValue().toString() != null && !sa.getStrValue().toString().trim().isEmpty()) {
                str += sa.getStrValue().toString().trim();
            }
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
                return dsfv.getStrValue();
            }
        }
        return "";
    }

    public String getDistributionDate() {
        for (DatasetFieldValue dsfv : datasetVersion.getDatasetFieldValues()) {
            if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.distributionDate)) {
                return dsfv.getStrValue();
            }
        }
        return "";
    }

    public void setMetadataValueBlocks(DatasetVersion datasetVersion) {
        //TODO: A lot of clean up on the logic of this method
        metadataBlocksForView.clear();
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
                if (dsfv.getDatasetField().isHasChildren() || (dsfv.getStrValue() != null && !dsfv.getStrValue().trim().equals(""))) {
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
                metadataBlocksForView.put(mdb, mdbMap);
            }
        }

        metadataBlocksForEdit.clear();
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

            metadataBlocksForEdit.put(mdb, mdbMap);
        }
    }

}
