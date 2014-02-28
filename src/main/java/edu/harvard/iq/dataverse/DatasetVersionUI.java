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

/**
 *
 * @author skraffmiller
 */
public class DatasetVersionUI {
    
    public DatasetVersionUI() {
    }
    
    private Map<MetadataBlock, Object> metadataBlockValues = new HashMap(); 
    
    public List<Map.Entry<MetadataBlock, Object>> getMetadataBlockValues() { 
       Set<Map.Entry<MetadataBlock, Object>> metadataSet = metadataBlockValues.entrySet();
        return new ArrayList<>(metadataSet);
    }
    
    public DatasetVersionUI(DatasetVersion datasetVersion) {
        /*takes in the values of a dataset version 
         and apportiones them into lists for 
         viewing and editng in the dataset page.
         */
        setDatasetVersion(datasetVersion);
        this.setDatasetAuthors(new ArrayList());
        this.setDatasetKeywords(new ArrayList());
        this.setDatasetDistributors(new ArrayList());
        this.setGeneralDatasetValues(new ArrayList());
        

        for (DatasetFieldValue dsfv : datasetVersion.getDatasetFieldValues()) {
            //Special Fields for 'above the fold'
            if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.title)) {
                setTitle(dsfv);
            }
            if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.descriptionText)) {
                setDescription(dsfv);
            }

            if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.author)) {
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
                //TODO save Author as Org to DB somehow?
                if(datasetAuthor.getFirstName().isEmpty() && !datasetAuthor.getLastName().isEmpty()){
                    datasetAuthor.setAuthorAsOrg(true);
                }
                this.getDatasetAuthors().add(datasetAuthor);
            }
            if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.keyword)) {
                Collection childVals = dsfv.getChildDatasetFieldValues();
                DatasetKeyword datasetKeyword = new DatasetKeyword();
                if (childVals != null) {
                    for (Object cv : childVals) {
                        DatasetFieldValue cvo = (DatasetFieldValue) cv;
                        if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.keywordValue)) {
                            datasetKeyword.setValue(cvo);
                        }
                    }
                }
                this.getDatasetKeywords().add(datasetKeyword);
            }
            if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.topicClassification)) {
                Collection childVals = dsfv.getChildDatasetFieldValues();
                DatasetTopicClass datasetTopicClass = new DatasetTopicClass();
                if (childVals != null) {
                    for (Object cv : childVals) {
                        DatasetFieldValue cvo = (DatasetFieldValue) cv;
                        if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.topicClassValue)) {
                            datasetTopicClass.setValue(cvo);
                        }
                        if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.topicClassVocab)) {
                            datasetTopicClass.setVocab(cvo);
                        }
                        if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.topicClassVocabURI)) {
                            datasetTopicClass.setVocabURI(cvo);
                        }
                    }
                }
                this.getDatasetTopicClasses().add(datasetTopicClass);
            }
            if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.distributor)) {
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
            }
        }
     setMetadataValueBlocks(datasetVersion);
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

    private List<DatasetAuthor> datasetAuthors = new ArrayList();
    public List<DatasetAuthor> getDatasetAuthors() {
        return datasetAuthors;
    }
    public void setDatasetAuthors(List<DatasetAuthor> datasetAuthors) {
        this.datasetAuthors = datasetAuthors;
    }
    
    private List<DatasetFieldValue> generalDatasetValues = new ArrayList();
    public List<DatasetFieldValue> getGeneralDatasetValues() {
        return generalDatasetValues;
    }
    public void setGeneralDatasetValues(List<DatasetFieldValue> datasetValues) {
        this.generalDatasetValues = datasetValues;
    }
    
    private DatasetFieldValue description;
    public DatasetFieldValue getDescription() {
        return description;
    }
    public void setDescription(DatasetFieldValue description) {
        this.description = description;
    }
    
    private List<DatasetKeyword> datasetKeywords = new ArrayList();
    public List<DatasetKeyword> getDatasetKeywords() {
        return datasetKeywords;
    }
    public void setDatasetKeywords(List<DatasetKeyword> datasetKeywords) {
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
        //todo get dist date from datasetfieldvalue table
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
        
                
        if (!StringUtil.isEmpty(getDatasetVersion().getDataset().getOwner().getName())) {
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            str += " " + getDatasetVersion().getDataset().getOwner().getName() + " [Publisher] ";
        }
        
                
        if (this.getDatasetVersion().getVersionNumber() != null) {
            str += " V" + this.getDatasetVersion().getVersionNumber();
            str += " [Version]";
        }
        
        if (!StringUtil.isEmpty(getUNF())) {
            if (!StringUtil.isEmpty(str)) {
                str += " ";
            }
            str += getUNF();
        }
        String distributorNames = getDistributorNames();
        if (distributorNames.length() > 0) {
            str += " " + distributorNames;
            str += " [Distributor]";
        }



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
            if (sa.getFirstName() != null && !StringUtil.isEmpty(sa.getFirstName().getStrValue())){
                str += sa.getFirstName().getStrValue(); 
            }
            if (sa.getLastName() != null && !StringUtil.isEmpty(sa.getLastName().getStrValue())){
                str += " " + sa.getLastName().getStrValue();
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
        for (DatasetKeyword sa : this.getDatasetKeywords()) {
            if (str.trim().length() > 1) {
                str += "; ";
            }
            if (sa.getValue() != null && !sa.getValue().getStrValue().isEmpty() ){
                 str += sa.getValue().getStrValue(); 
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
    
   private void setMetadataValueBlocks(DatasetVersion datasetVersion){
       
        metadataBlockValues.clear();


        
        for(MetadataBlock mdb: this.datasetVersion.getDataset().getOwner().getMetadataBlocks()){
            List addList = new ArrayList();
            if(!mdb.isShowOnCreate()){

             for (DatasetFieldValue dsfv : datasetVersion.getDatasetFieldValues() ){
             /*   This gets the sort right but does not save child vals correctly 
             if (!dsfv.getDatasetField().isShowAboveFold() && !dsfv.getDatasetField().isHasParent() && dsfv.getDatasetField().getMetadataBlock().equals(mdb)) {
                addList.add(dsfv);
                if (dsfv.getChildDatasetFieldValues() != null && !dsfv.getChildDatasetFieldValues().isEmpty()) {
                    for (DatasetFieldValue dsfcvo : datasetVersion.getDatasetFieldValues()) {
                        if (dsfcvo.getParentDatasetFieldValue() != null
                                && dsfcvo.getParentDatasetFieldValue().getDatasetField().getName().equals(dsfv.getDatasetField().getName())) {
                            addList.add(dsfcvo);
                        }
                    }
                }
             }*/
                 
                // saves child vals of compoud fields but doesnt sort right.
                if (!dsfv.getDatasetField().isShowAboveFold()  && dsfv.getDatasetField().getMetadataBlock().equals(mdb)) {
                    addList.add(dsfv);
                }
                }
      Collections.sort(addList, new Comparator<DatasetFieldValue>(){
           public int compare (DatasetFieldValue d1, DatasetFieldValue d2){
               int a = d1.getDatasetField().getDisplayOrder();
               int b = d2.getDatasetField().getDisplayOrder();
               return Integer.valueOf(a).compareTo(Integer.valueOf(b));
           }
       });
                metadataBlockValues.put(mdb, addList);
            }
        }              
    }
}
