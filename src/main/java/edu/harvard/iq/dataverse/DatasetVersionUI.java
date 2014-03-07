/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.faces.model.SelectItem;

/**
 *
 * @author skraffmiller
 */
public class DatasetVersionUI {
    @EJB
    DataverseServiceBean dataverseService; 
    public DatasetVersionUI() {
    }
    
    private Map<MetadataBlock, List> metadataBlockValues = new HashMap(); 
    
    public List<Map.Entry<MetadataBlock, List>> getMetadataBlockValues() { 
       Set<Map.Entry<MetadataBlock, List>> metadataSet = metadataBlockValues.entrySet();
        return new ArrayList<>(metadataSet);
    }
    
    public DatasetVersionUI(DatasetVersion datasetVersion) {
        /*takes in the values of a dataset version 
         and apportions them into lists for 
         viewing and editng in the dataset page.
         */
        setDatasetVersion(datasetVersion);
        this.setDatasetAuthors(new ArrayList());
        this.setDatasetKeywords(new ArrayList());
        this.setDatasetDistributors(new ArrayList()); 
        this.setSubjects(new ArrayList());

        for (DatasetFieldValue dsfv : datasetVersion.getDatasetFieldValues()) {
            //Special Fields for 'above the fold'
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
                        if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.authorLastName)) {
                            datasetAuthor.setLastName(cvo);
                        }
                        if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.authorFirstName)) {
                            datasetAuthor.setFirstName(cvo);
                        }
                        if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.authorAffiliation)) {
                            datasetAuthor.setAffiliation(cvo);
                        }
                    }
                }
                datasetAuthor.setAuthorAsOrg(false);
                this.getDatasetAuthors().add(datasetAuthor);
            } else if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.keyword)) {
                this.getDatasetKeywords().add(dsfv);
            } 
            else if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.distributor)) {
                Collection childVals = dsfv.getChildDatasetFieldValues();
                DatasetDistributor datasetDistributor = new DatasetDistributor();
                if (childVals != null) {
                    for (Object cv : childVals) {
                        DatasetFieldValue cvo = (DatasetFieldValue) cv;
                        if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.distributorName)) {
                            datasetDistributor.setName(cvo);
                        }
                        if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.distributorAbbreviation)) {
                            datasetDistributor.setAbbreviation(cvo);
                        }
                        if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.distributorAffiliation)) {
                            datasetDistributor.setAffiliation(cvo);
                        }
                        if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.distributorLogo)) {
                            datasetDistributor.setLogo(cvo);
                        }
                        if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.distributorURL)) {
                            datasetDistributor.setUrl(cvo);
                        }
                    }
                }
                this.getDatasetDistributors().add(datasetDistributor);            
            } else if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.subject)) {
                this.getSubjects().add(dsfv.getStrValue());
            } else if (dsfv.getDatasetField().isShowAboveFold() &&  !dsfv.getDatasetField().isHasParent() &&  !dsfv.getDatasetField().isHasChildren() ) {
                this.getAboveFoldGeneralValues().add(dsfv);                                
            } 
        }
     setMetadataValueBlocks(datasetVersion);
     loadSubjectControlledVocabulary();
    }
    
    private Dataset getDataset(){
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

    private List <String> subjects = new ArrayList();
    public List<String>  getSubjects() {
        return subjects;
    }
    public void setSubjects(List <String> subjects) {
        this.subjects = subjects;
    }

    private List <SelectItem> subjectControlledVocabulary = new ArrayList();
    public List<SelectItem>  getSubjectControlledVocabulary() {
        return subjectControlledVocabulary;
    }
    public void setSubjectControlledVocabulary(List <SelectItem> subjectControlledVocabulary) {
        this.subjectControlledVocabulary = subjectControlledVocabulary;
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
    
    private List<DatasetTopicClass> datasetTopicClasses = new ArrayList();
    public List<DatasetTopicClass> getDatasetTopicClasses() {
        return datasetTopicClasses;
    }
    public void setDatasetTopicClasses(List<DatasetTopicClass> datasetTopicClasses) {
        this.datasetTopicClasses = datasetTopicClasses;
    } 
    
    private List<DatasetDistributor> datasetDistributors;
    public List<DatasetDistributor> getDatasetDistributors() {
        return datasetDistributors;
    }
    public void setDatasetDistributors(List<DatasetDistributor> datasetDistributors) {
        this.datasetDistributors = datasetDistributors;
    } 
    
    public String getUNF() {
        //todo get UNF to calculate and display here.
        return "";
    }
    
    private List<DatasetFieldValue> aboveFoldGeneralValues = new ArrayList();
    public List<DatasetFieldValue> getAboveFoldGeneralValues() {
        return aboveFoldGeneralValues;
    }
    public void setAboveFoldGeneralValues(List<DatasetFieldValue> aboveFoldGeneralValues) {
        this.aboveFoldGeneralValues = aboveFoldGeneralValues;
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
         while (root.getOwner() != null){
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
            if (sa.getName() != null && !StringUtil.isEmpty(sa.getName().getStrValue())){
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
            if (sa.getStrValue() != null && sa.getStrValue().toString() != null && !sa.getStrValue().toString().trim().isEmpty() ){
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
            if (sa != null && sa.toString() != null && !sa.toString().trim().isEmpty() ){
                 str += sa.toString().trim(); 
            }
        }
        return str;
    }
    
    public String getTopicClassStr() {
        String str = "";
        for (DatasetTopicClass sa : this.getDatasetTopicClasses()) {
            if (str.trim().length() > 1) {
                str += "; ";
            }
            if (sa.getVocabURI() != null && !sa.getVocabURI().getStrValue().isEmpty() ){
                 str += "<a href='" + sa.getVocabURI().getStrValue() + "' target='_blank'>"; 
            }
            if (sa.getVocab() != null && !sa.getVocab().getStrValue().isEmpty() ){
                 str += sa.getVocab().getStrValue(); 
            }
            if (sa.getVocabURI() != null && !sa.getVocabURI().getStrValue().isEmpty() ){
                 str += "</a>";
            }
            if (sa.getVocab() != null && !sa.getVocab().getStrValue().isEmpty() ){
                 str += ": "; 
        }
            if (sa.getValue() != null && !sa.getValue().getStrValue().isEmpty() ){
                 str += sa.getValue().getStrValue();
            }
        }
        return str;
    }

    public String getProductionDate() {        
        for (DatasetFieldValue dsfv : datasetVersion.getDatasetFieldValues()){
            if(dsfv.getDatasetField().getName().equals(DatasetFieldConstant.productionDate)){
                 return dsfv.getStrValue();
            }
        }
        return "";
    }

    public String getDistributionDate() {
         for (DatasetFieldValue dsfv : datasetVersion.getDatasetFieldValues()){
            if(dsfv.getDatasetField().getName().equals(DatasetFieldConstant.distributionDate)){
                 return dsfv.getStrValue();
            }
        }
        return "";
    }

    public String getDistributorNames() {
        String str = "";
        for (DatasetDistributor sd : this.getDatasetDistributors()) {
            if (str.trim().length() > 1) {
                str += ";";
            }
            if (sd.getName() != null){
                str += sd.getName().getStrValue();
            }
        }
        return str.trim();
    }
    
    public void setMetadataValueBlocks(DatasetVersion datasetVersion) {
        metadataBlockValues.clear();
        for (MetadataBlock mdb : this.datasetVersion.getDataset().getOwner().getMetadataBlocks()) {
            List addList = new ArrayList();
            if (!mdb.isShowOnCreate()) {
                for (DatasetFieldValue dsfv : datasetVersion.getDatasetFieldValues()) {
                    if (!dsfv.getDatasetField().isShowAboveFold() && dsfv.getDatasetField().getMetadataBlock().equals(mdb)) {
                        addList.add(dsfv);
                    }
                }
                Collections.sort(addList, new Comparator<DatasetFieldValue>() {
                    public int compare(DatasetFieldValue d1, DatasetFieldValue d2) {
                        int a = d1.getDatasetField().getDisplayOrder();
                        int b = d2.getDatasetField().getDisplayOrder();
                        return Integer.valueOf(a).compareTo(Integer.valueOf(b));
                    }
                });
                metadataBlockValues.put(mdb, addList);
            }
        }
    }
    
    private void loadSubjectControlledVocabulary(){        
       for (DatasetFieldValue dsfv : datasetVersion.getDatasetFieldValues()){
            if(dsfv.getDatasetField().getName().equals(DatasetFieldConstant.subject)){
                 this.subjectControlledVocabulary.clear();
                 for (ControlledVocabularyValue value : dsfv.getDatasetField().getControlledVocabularyValuess()){
                     SelectItem item = new SelectItem();
                     item.setValue(value.getStrValue());
                     item.setLabel(value.getStrValue());
                     this.subjectControlledVocabulary.add(item);
                 }
                 break; //only do once if multiple values
            }
        }               
    }
}
