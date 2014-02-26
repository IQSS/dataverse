/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author skraffmiller
 */
public class DatasetVersionUI {
    
    public DatasetVersionUI() {
    }
       
    public DatasetVersionUI(DatasetVersion datasetVersion) {
        setDatasetVersion(datasetVersion);
        this.setDatasetAuthors(new ArrayList());
        this.setDatasetKeywords(new ArrayList());
        for (DatasetFieldValue dsfv : datasetVersion.getDatasetFieldValues()) {
            if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.title)) {
                setTitle(dsfv);
            }
            if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.descriptionText)) {
                setDescription(dsfv);
            }
            if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.author)) {
                Collection childVals = dsfv.getChildDatasetFieldValues();
                DatasetAuthor datasetAuthor = new DatasetAuthor();
                if (childVals != null){
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
                    
                } else {
                    for (DatasetField dsfl: dsfv.getDatasetField().getChildDatasetFields()){
                        if(dsfl.getName().equals(DatasetFieldConstant.authorName) ){
                            for (DatasetFieldValue dsfcv : datasetVersion.getDatasetFieldValues()){
                                if(dsfl.equals(dsfcv.getDatasetField())){
                                    datasetAuthor.setName(dsfcv);
                                }
                            } 
                        }
                        if(dsfl.getName().equals(DatasetFieldConstant.authorLastName) ){
                            for (DatasetFieldValue dsfcv : datasetVersion.getDatasetFieldValues()){
                                if(dsfl.equals(dsfcv.getDatasetField())){
                                    datasetAuthor.setLastName(dsfcv);
                                }
                            } 
                        }
                        if(dsfl.getName().equals(DatasetFieldConstant.authorFirstName) ){
                            for (DatasetFieldValue dsfcv : datasetVersion.getDatasetFieldValues()){
                                if(dsfl.equals(dsfcv.getDatasetField())){
                                    datasetAuthor.setFirstName(dsfcv);
                                }
                            } 
                        }
                        if(dsfl.getName().equals(DatasetFieldConstant.authorAffiliation) ){
                            for (DatasetFieldValue dsfcv : datasetVersion.getDatasetFieldValues()){
                                if(dsfl.equals(dsfcv.getDatasetField())){
                                    datasetAuthor.setAffiliation(dsfcv);
                                }
                            } 
                        }
                    }
                }

                this.getDatasetAuthors().add(datasetAuthor);
            }
            if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.keyword)) {
                Collection childVals = dsfv.getChildDatasetFieldValues();
                DatasetKeyword datasetKeyword = new DatasetKeyword();
                if (childVals != null){
                    for (Object cv : childVals) {
                        DatasetFieldValue cvo = (DatasetFieldValue) cv;
                        if (cvo.getDatasetField().getName().equals(DatasetFieldConstant.keywordValue)) {
                            datasetKeyword.setValue(cvo);
                        }
                    }
                } else {
                    for (DatasetField dsfl: dsfv.getDatasetField().getChildDatasetFields()){
                        if(dsfl.getName().equals(DatasetFieldConstant.keywordValue) ){
                            for (DatasetFieldValue dsfcv : datasetVersion.getDatasetFieldValues()){
                                if(dsfl.equals(dsfcv.getDatasetField())){
                                    datasetKeyword.setValue(dsfcv);
                                }
                            } 
                        }
                    }
                    
                }
                this.getDatasetKeywords().add(datasetKeyword);
            }
            if (dsfv.getDatasetField().getName().equals(DatasetFieldConstant.topicClassification)) {
                Collection childVals = dsfv.getChildDatasetFieldValues();
                DatasetTopicClass datasetTopicClass = new DatasetTopicClass();
                if (childVals != null){
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
                } else {
                    for (DatasetField dsfl: dsfv.getDatasetField().getChildDatasetFields()){
                        if(dsfl.getName().equals(DatasetFieldConstant.topicClassValue) ){
                            for (DatasetFieldValue dsfcv : datasetVersion.getDatasetFieldValues()){
                                if(dsfl.equals(dsfcv.getDatasetField())){
                                    datasetTopicClass.setValue(dsfcv);
                                }
                            } 
                        }
                        if(dsfl.getName().equals(DatasetFieldConstant.topicClassVocab) ){
                            for (DatasetFieldValue dsfcv : datasetVersion.getDatasetFieldValues()){
                                if(dsfl.equals(dsfcv.getDatasetField())){
                                    datasetTopicClass.setVocab(dsfcv);
                                }
                            } 
                        }
                        if(dsfl.getName().equals(DatasetFieldConstant.topicClassVocabURI) ){
                            for (DatasetFieldValue dsfcv : datasetVersion.getDatasetFieldValues()){
                                if(dsfl.equals(dsfcv.getDatasetField())){
                                    datasetTopicClass.setVocabURI(dsfcv);
                                }
                            } 
                        }
                    }
                    
                }
                this.getDatasetTopicClasses().add(datasetTopicClass);
            }
        }
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
    
    public String getUNF() {
        //todo get dist date from datasetfieldvalue table
        return "UNF";
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
        
        if (!StringUtil.isEmpty(getDatasetVersion().getDataset().getOwner().getName())) {
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            str += " " + getDatasetVersion().getDataset().getOwner().getName() + " ";
        }
        
        if (this.getDatasetVersion().getVersionNumber() != null) {
            str += " V" + this.getDatasetVersion().getVersionNumber();
            str += " [Version]";
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
            if (sa.getVocab() != null && !sa.getVocab().getStrValue().isEmpty() ){
                 str += sa.getVocab().getStrValue() + ": "; 
            }
            if (sa.getVocabURI() != null && !sa.getVocabURI().getStrValue().isEmpty() ){
                 str += "<a href='" + sa.getVocabURI().getStrValue() + "'>"; 
            }
            if (sa.getValue() != null && !sa.getValue().getStrValue().isEmpty() ){
                 str += sa.getValue().getStrValue() + "</a>";
            }
        }
        return str;
    }

    public String getProductionDate() {
        //todo get "Production Date" from datasetfieldvalue table
        return "Production Date";
    }

    public String getDistributionDate() {
        //todo get dist date from datasetfieldvalue table
        return "Distribution Date";
    }

    public List<DatasetDistributor> getDatasetDistributors() {
        //todo get distributors from DatasetfieldValues
        return new ArrayList();
    }

    public String getDistributorNames() {
        String str = "";
        for (DatasetDistributor sd : this.getDatasetDistributors()) {
            if (str.trim().length() > 1) {
                str += ";";
            }
            str += sd.getName().getStrValue();
        }
        return str;
    }
}
