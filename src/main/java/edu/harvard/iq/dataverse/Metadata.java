/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Transient;
import javax.persistence.Version;
import org.hibernate.validator.constraints.URL;

/**
 *
 * @author skraffmiller
 */
@Entity
public class Metadata implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    
    @OneToOne(mappedBy="metadata")
    private DatasetVersion datasetVersion;
    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }
    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }
    
    @OneToOne(mappedBy="metadata")
    private Template template;
    public Template getTemplate() {
        return template;
    }
    public void setTemplate(Template template) {
        this.template = template;
    }
     
    public Metadata () {
    }
    
    private boolean copyField(TemplateField tf, boolean copyHidden, boolean copyDisabled) {
        return (!tf.isHidden() && !tf.isDisabled()) ||
               (copyHidden && tf.isHidden() ) ||
               (copyDisabled && tf.isDisabled());       
    }
    
        
    public  Metadata(Metadata source, boolean copyHidden, boolean copyDisabled ) {
     
        this.setUNF(source.UNF);
        this.setDatasetFieldValues(new ArrayList<DatasetFieldValue>());
        
        Template sourceTemplate = source.getTemplate(); // != null ? source.getTemplate() : source.getDatasetVersion().getDataset().getTemplate();
        
        
        // create a Map so we can look up each template field and check its field input level
        Map<String,TemplateField> tfMap = new HashMap();        
        for (TemplateField tf : sourceTemplate.getTemplateFields()){
            tfMap.put(tf.getDatasetField().getName(), tf);
        }
  
        if( copyField(tfMap.get(DatasetFieldConstant.availabilityStatus), copyHidden, copyDisabled) ) {
             this.setAvailabilityStatus(source.availabilityStatus); 
        }

        if( copyField(tfMap.get(DatasetFieldConstant.citationRequirements), copyHidden, copyDisabled) ) {
             this.setCitationRequirements(source.citationRequirements); 
        }
        
        if( copyField(tfMap.get(DatasetFieldConstant.collectionSize), copyHidden, copyDisabled) ) {
             this.setCollectionSize(source.collectionSize); 
        }

        if( copyField(tfMap.get(DatasetFieldConstant.conditions), copyHidden, copyDisabled) ) {
             this.setConditions(source.conditions); 
        }

        if( copyField(tfMap.get(DatasetFieldConstant.confidentialityDeclaration), copyHidden, copyDisabled) ) {
             this.setConfidentialityDeclaration(source.confidentialityDeclaration); 
        }
        
        if( copyField(tfMap.get(DatasetFieldConstant.contact), copyHidden, copyDisabled) ) {
             this.setContact(source.contact); 
        }

        if( copyField(tfMap.get(DatasetFieldConstant.dateOfDeposit), copyHidden, copyDisabled) ) {
             this.setDateOfDeposit(source.dateOfDeposit); 
        }         

        if( copyField(tfMap.get(DatasetFieldConstant.depositor), copyHidden, copyDisabled) ) {
             this.setDepositor(source.depositor); 
        }          

        if( copyField(tfMap.get(DatasetFieldConstant.depositorRequirements), copyHidden, copyDisabled) ) {
             this.setDepositorRequirements(source.depositorRequirements); 
        } 

        if( copyField(tfMap.get(DatasetFieldConstant.disclaimer), copyHidden, copyDisabled) ) {
             this.setDisclaimer(source.disclaimer); 
        } 

        if( copyField(tfMap.get(DatasetFieldConstant.distributionDate), copyHidden, copyDisabled) ) {
             this.setDistributionDate(source.distributionDate); 
        }
        
        if( copyField(tfMap.get(DatasetFieldConstant.fundingAgency), copyHidden, copyDisabled) ) {
             this.setFundingAgency(source.fundingAgency); 
        }
        
        if (copyField(tfMap.get(DatasetFieldConstant.originalArchive), copyHidden, copyDisabled)) {
            this.setOriginalArchive(source.originalArchive);
        }

        if (copyField(tfMap.get(DatasetFieldConstant.placeOfAccess), copyHidden, copyDisabled)) {
            this.setPlaceOfAccess(source.placeOfAccess);
        }

        if (copyField(tfMap.get(DatasetFieldConstant.productionDate), copyHidden, copyDisabled)) {
            this.setProductionDate(source.productionDate);
        }

        if (copyField(tfMap.get(DatasetFieldConstant.productionPlace), copyHidden, copyDisabled)) {
            this.setProductionPlace(source.productionPlace);
        }

        if (copyField(tfMap.get(DatasetFieldConstant.restrictions), copyHidden, copyDisabled)) {
            this.setRestrictions(source.restrictions);
        }

        if (copyField(tfMap.get(DatasetFieldConstant.specialPermissions), copyHidden, copyDisabled)) {
            this.setSpecialPermissions(source.specialPermissions);
        }

        if (copyField(tfMap.get(DatasetFieldConstant.subTitle), copyHidden, copyDisabled)) {
            this.setSubTitle(source.subTitle);
        }
      
        if (copyField(tfMap.get(DatasetFieldConstant.title), copyHidden, copyDisabled)) {
            this.setTitle(source.title);
        }

        if (copyField(tfMap.get(DatasetFieldConstant.datasetCompletion), copyHidden, copyDisabled)) {
            this.setDatasetCompletion(source.datasetCompletion);
        }
        
        // compound and/or multiple fields
        if( copyField(tfMap.get(DatasetFieldConstant.distributorContact), copyHidden, copyDisabled) ) {
             this.setDistributorContact(source.distributorContact); 
             this.setDistributorContactAffiliation(source.distributorContactAffiliation); 
             this.setDistributorContactEmail(source.distributorContactEmail); 
        }   
        
        if (copyField(tfMap.get(DatasetFieldConstant.series), copyHidden, copyDisabled)) {
            this.setSeriesName(source.seriesName);
            this.setSeriesInformation(source.seriesInformation);
        }        
        
        if (copyField(tfMap.get(DatasetFieldConstant.datasetVersion), copyHidden, copyDisabled)) {
            this.setDatasetVersionText(source.datasetVersionText);
            this.setVersionDate(source.versionDate);            
        }   
               
        if (copyField(tfMap.get(DatasetFieldConstant.author), copyHidden, copyDisabled)) {
            this.setDatasetAuthors(new ArrayList<DatasetAuthor>());
            for (DatasetAuthor author : source.datasetAuthors) {
                DatasetAuthor cloneAuthor = new DatasetAuthor();
                cloneAuthor.setAffiliation(author.getAffiliation());
                cloneAuthor.setDisplayOrder(author.getDisplayOrder());
                cloneAuthor.setMetadata(this);
                cloneAuthor.setName(author.getName());
                this.getDatasetAuthors().add(cloneAuthor);
            }
        }
                
        if (copyField(tfMap.get(DatasetFieldConstant.distributor), copyHidden, copyDisabled)) {
            this.setDatasetDistributors(new ArrayList<DatasetDistributor>());
            for (DatasetDistributor dist : source.datasetDistributors) {
                DatasetDistributor cloneDist = new DatasetDistributor();
                cloneDist.setAbbreviation(dist.getAbbreviation());
                cloneDist.setAffiliation(dist.getAffiliation());
                cloneDist.setDisplayOrder(dist.getDisplayOrder());
                cloneDist.setMetadata(this);
                cloneDist.setLogo(dist.getLogo());
                cloneDist.setName(dist.getName());
                cloneDist.setUrl(dist.getUrl());
                this.getDatasetDistributors().add(cloneDist);
            }
        }
        
        if (copyField(tfMap.get(DatasetFieldConstant.description), copyHidden, copyDisabled)) {
            this.setDatasetAbstracts(new ArrayList<DatasetAbstract>());
            for (DatasetAbstract sa : source.datsetAbstracts) {
                DatasetAbstract cloneAbstract = new DatasetAbstract();
                cloneAbstract.setDate(sa.getDate());
                cloneAbstract.setDisplayOrder(sa.getDisplayOrder());
                cloneAbstract.setMetadata(this);
                cloneAbstract.setText(sa.getText());
                this.getDatasetAbstracts().add(cloneAbstract);
            }
        }
                
        if (copyField(tfMap.get(DatasetFieldConstant.keyword), copyHidden, copyDisabled)) {
            this.setDatasetKeywords(new ArrayList<DatasetKeyword>());
            for (DatasetKeyword key : source.datasetKeywords) {
                DatasetKeyword cloneKey = new DatasetKeyword();
                cloneKey.setDisplayOrder(key.getDisplayOrder());
                cloneKey.setMetadata(this);
                cloneKey.setValue(key.getValue());
                cloneKey.setVocab(key.getVocab());
                cloneKey.setVocabURI(key.getVocabURI());
                this.getDatasetKeywords().add(cloneKey);
            }
        }
                
        if (copyField(tfMap.get(DatasetFieldConstant.note), copyHidden, copyDisabled)) {
            this.setDatasetNotes(new ArrayList<DatasetNote>());
            for (DatasetNote note : source.datasetNotes) {
                DatasetNote cloneNote = new DatasetNote();
                cloneNote.setDisplayOrder(note.getDisplayOrder());
                cloneNote.setMetadata(this);
                cloneNote.setSubject(note.getSubject());
                cloneNote.setText(note.getText());
                cloneNote.setType(note.getType());
                this.getDatasetNotes().add(cloneNote);
            }
        }
        

        if (copyField(tfMap.get(DatasetFieldConstant.otherId), copyHidden, copyDisabled)) {
            this.setDatasetOtherIds(new ArrayList<DatasetOtherId>());
            for (DatasetOtherId id : source.datasetOtherIds) {
                DatasetOtherId cloneId = new DatasetOtherId();
                cloneId.setAgency(id.getAgency());
                cloneId.setDisplayOrder(id.getDisplayOrder());
                cloneId.setMetadata(this);
                cloneId.setOtherId(id.getOtherId());
                this.getDatasetOtherIds().add(cloneId);
            }
        }
               
        if (copyField(tfMap.get(DatasetFieldConstant.otherReferences), copyHidden, copyDisabled)) {
            this.setDatasetOtherRefs(new ArrayList<DatasetOtherRef>());
            for (DatasetOtherRef ref : source.datasetOtherRefs) {
                DatasetOtherRef cloneRef = new DatasetOtherRef();
                cloneRef.setDisplayOrder(ref.getDisplayOrder());
                cloneRef.setMetadata(this);
                cloneRef.setText(ref.getText());
                this.getDatasetOtherRefs().add(cloneRef);
            }
        }       

        if (copyField(tfMap.get(DatasetFieldConstant.producer), copyHidden, copyDisabled)) {
            this.setDatasetProducers(new ArrayList<DatasetProducer>());
            for (DatasetProducer prod : source.datasetProducers) {
                DatasetProducer cloneProd = new DatasetProducer();
                cloneProd.setAbbreviation(prod.getAbbreviation());
                cloneProd.setAffiliation(prod.getAffiliation());
                cloneProd.setDisplayOrder(prod.getDisplayOrder());
                cloneProd.setLogo(prod.getLogo());
                cloneProd.setMetadata(this);
                cloneProd.setName(prod.getName());
                cloneProd.setUrl(prod.getUrl());
                this.getDatasetProducers().add(cloneProd);
            }
        }
               
        if (copyField(tfMap.get(DatasetFieldConstant.relatedMaterial), copyHidden, copyDisabled)) {
            this.setDatasetRelMaterials(new ArrayList<DatasetRelMaterial>());
            for (DatasetRelMaterial rel : source.datasetRelMaterials) {
                DatasetRelMaterial cloneRel = new DatasetRelMaterial();
                cloneRel.setDisplayOrder(rel.getDisplayOrder());
                cloneRel.setMetadata(this);
                cloneRel.setText(rel.getText());
                this.getDatasetRelMaterials().add(cloneRel);
            }
        }
       
        if (copyField(tfMap.get(DatasetFieldConstant.publication), copyHidden, copyDisabled)) {
            this.setDatasetRelPublications(new ArrayList<DatasetRelPublication>());
            for (DatasetRelPublication rel : source.datasetRelPublications) {
                DatasetRelPublication cloneRel = new DatasetRelPublication();
                cloneRel.setDisplayOrder(rel.getDisplayOrder());
                cloneRel.setMetadata(this);
                cloneRel.setText(rel.getText());
                cloneRel.setIdType(rel.getIdType());
                cloneRel.setIdNumber(rel.getIdNumber());
                cloneRel.setUrl(rel.getUrl());
                cloneRel.setReplicationData(rel.isReplicationData());
                this.getDatasetRelPublications().add(cloneRel);
            }
        } 
        /*
        if (copyField(tfMap.get(DatasetFieldConstant.relatedDatasets), copyHidden, copyDisabled)) {
            this.setDatasetRelDatasets(new ArrayList<DatasetRelDataset>());
            for (DatasetRelDataset rel : source.datasetRelDatasets) {
                DatasetRelDataset cloneRel = new DatasetRelDataset();
                cloneRel.setDisplayOrder(rel.getDisplayOrder());
                cloneRel.setMetadata(this);
                cloneRel.setText(rel.getText());
                this.getDatasetRelDatasets().add(cloneRel);
            }
        }  */   
        /*
        if (copyField(tfMap.get(DatasetFieldConstant.relatedDatasets), copyHidden, copyDisabled)) {
            this.setDatasetRelDatasets(new ArrayList<DatasetRelDataset>());
            for (DatasetRelDataset rel : source.datasetRelDatasets) {
                DatasetRelDataset cloneRel = new DatasetRelDataset();
                cloneRel.setDisplayOrder(rel.getDisplayOrder());
                cloneRel.setMetadata(this);
                cloneRel.setText(rel.getText());
                this.getDatasetRelDatasets().add(cloneRel);
            }
        }
        */
        if (copyField(tfMap.get(DatasetFieldConstant.software), copyHidden, copyDisabled)) {
            this.setDatasetSoftware(new ArrayList<DatasetSoftware>());
            for (DatasetSoftware soft : source.datasetSoftware) {
                DatasetSoftware cloneSoft = new DatasetSoftware();
                cloneSoft.setDisplayOrder(soft.getDisplayOrder());
                cloneSoft.setMetadata(this);
                cloneSoft.setName(soft.getName());
                cloneSoft.setSoftwareVersion(soft.getSoftwareVersion());
                this.getDatasetSoftware().add(cloneSoft);
            }
        }
        
        if (copyField(tfMap.get(DatasetFieldConstant.grantNumber), copyHidden, copyDisabled)) {
            this.setDatasetGrants(new ArrayList<DatasetGrant>());
            for (DatasetGrant grant : source.datasetGrants) {
                DatasetGrant cloneGrant = new DatasetGrant();
                cloneGrant.setAgency(grant.getAgency());
                cloneGrant.setDisplayOrder(grant.getDisplayOrder());
                cloneGrant.setMetadata(this);
                cloneGrant.setNumber(grant.getNumber());
                this.getDatasetGrants().add(cloneGrant);
            }
        }


        
        
        if (copyField(tfMap.get(DatasetFieldConstant.topicClassification), copyHidden, copyDisabled)) {

            this.setDatasetTopicClasses(new ArrayList<DatasetTopicClass>());
            for (DatasetTopicClass topic : source.datasetTopicClasses) {
                DatasetTopicClass cloneTopic = new DatasetTopicClass();
                cloneTopic.setDisplayOrder(topic.getDisplayOrder());
                cloneTopic.setMetadata(this);
                cloneTopic.setValue(topic.getValue());
                cloneTopic.setVocab(topic.getVocab());
                cloneTopic.setVocabURI(topic.getVocabURI());
                this.getDatasetTopicClasses().add(cloneTopic);
            }
        }
        

/*

        if (copyField(tfMap.get(DatasetFieldConstant.geographicBoundingBox), copyHidden, copyDisabled)) {
            this.setStudyGeoBoundings(new ArrayList<StudyGeoBounding>());
            for (StudyGeoBounding geo : source.studyGeoBoundings) {
                StudyGeoBounding cloneGeo = new StudyGeoBounding();
                cloneGeo.setDisplayOrder(geo.getDisplayOrder());
                cloneGeo.setMetadata(this);
                cloneGeo.setEastLongitude(geo.getEastLongitude());
                cloneGeo.setNorthLatitude(geo.getNorthLatitude());
                cloneGeo.setSouthLatitude(geo.getSouthLatitude());
                cloneGeo.setWestLongitude(geo.getWestLongitude());
                this.getStudyGeoBoundings().add(cloneGeo);
            }
        }


        */
        // custom values
        for (DatasetField dsf : source.getDatasetFields()) {                     
            if( copyField(tfMap.get(dsf.getName()), copyHidden, copyDisabled) ) {
                for (DatasetFieldValue dsfv: dsf.getDatasetFieldValues()){
                    DatasetFieldValue cloneSfv = new DatasetFieldValue();
                    cloneSfv.setDisplayOrder(dsfv.getDisplayOrder());
                    cloneSfv.setDatasetField(dsfv.getDatasetField());
                    cloneSfv.setStrValue(dsfv.getStrValue());
                    cloneSfv.setMetadata(this);
                    this.getDatasetFieldValues().add(cloneSfv);    
                }
            }            
        }                
    }
    
    // This constructor is for an exact clone, regarldes of field input levels
    public Metadata(Metadata source ) {
        this.setTemplate(source.getTemplate());
        this.setUNF(source.UNF);
        this.setAvailabilityStatus(source.availabilityStatus);
        this.setCitationRequirements(source.citationRequirements);
        this.setCollectionSize(source.collectionSize);
        this.setConditions(source.conditions);
        this.setConfidentialityDeclaration(source.confidentialityDeclaration);
        this.setContact(source.contact);
        this.setDateOfDeposit(source.dateOfDeposit);
        this.setDepositor(source.depositor);
        this.setDepositorRequirements(source.depositorRequirements);
        this.setDisclaimer(source.disclaimer);
        this.setDistributionDate(source.distributionDate);
        this.setDistributorContact(source.distributorContact);
        this.setDistributorContactAffiliation(source.distributorContactAffiliation);
        this.setDistributorContactEmail(source.distributorContactEmail);
        this.setFundingAgency(source.fundingAgency);
        this.setOriginalArchive(source.originalArchive);
        this.setPlaceOfAccess(source.placeOfAccess);
        this.setProductionDate(source.productionDate);
        this.setProductionPlace(source.productionPlace);
        this.setRestrictions(source.restrictions);
        this.setSeriesInformation(source.seriesInformation);
        this.setSeriesName(source.seriesName);
        this.setSpecialPermissions(source.specialPermissions);
        this.setDatasetVersionText(source.datasetVersionText);
        this.setSubTitle(source.subTitle);
        this.setTitle(source.title);
        this.setVersionDate(source.versionDate);
        this.setDatasetCompletion(source.datasetCompletion);
 
        this.setDatasetAuthors(new ArrayList<DatasetAuthor>());
        for (DatasetAuthor author: source.datasetAuthors) {
            DatasetAuthor cloneAuthor = new DatasetAuthor();
            cloneAuthor.setAffiliation(author.getAffiliation());
            cloneAuthor.setDisplayOrder(author.getDisplayOrder());
            cloneAuthor.setMetadata(this);
            cloneAuthor.setName(author.getName());
            this.getDatasetAuthors().add(cloneAuthor);
        }
       
        this.setDatasetDistributors(new ArrayList<DatasetDistributor>());
        for (DatasetDistributor dist: source.datasetDistributors){
            DatasetDistributor cloneDist = new DatasetDistributor();
            cloneDist.setAbbreviation(dist.getAbbreviation());
            cloneDist.setAffiliation(dist.getAffiliation());
            cloneDist.setDisplayOrder(dist.getDisplayOrder());
            cloneDist.setMetadata(this);
            cloneDist.setLogo(dist.getLogo());
            cloneDist.setName(dist.getName());
            cloneDist.setUrl(dist.getUrl());
            this.getDatasetDistributors().add(cloneDist);
        }
        
        this.setDatasetAbstracts(new ArrayList<DatasetAbstract>());
        for(DatasetAbstract sa: source.datsetAbstracts) {
            DatasetAbstract cloneAbstract = new DatasetAbstract();
            cloneAbstract.setDate(sa.getDate());
            cloneAbstract.setDisplayOrder(sa.getDisplayOrder());
            cloneAbstract.setMetadata(this);
            cloneAbstract.setText(sa.getText());
            this.getDatasetAbstracts().add(cloneAbstract);
        }
        
        this.setDatasetKeywords(new ArrayList<DatasetKeyword>());
        for(DatasetKeyword key: source.datasetKeywords) {
            DatasetKeyword cloneKey = new DatasetKeyword();
            cloneKey.setDisplayOrder(key.getDisplayOrder());
            cloneKey.setMetadata(this);
            cloneKey.setValue(key.getValue());
            cloneKey.setVocab(key.getVocab());
            cloneKey.setVocabURI(key.getVocabURI());
            this.getDatasetKeywords().add(cloneKey);
        }
               
       this.setDatasetNotes(new ArrayList<DatasetNote>());
       for(DatasetNote note: source.datasetNotes) {
            DatasetNote cloneNote = new DatasetNote();
            cloneNote.setDisplayOrder(note.getDisplayOrder());
            cloneNote.setMetadata(this);
            cloneNote.setSubject(note.getSubject());
            cloneNote.setText(note.getText());
            cloneNote.setType(note.getType());
            this.getDatasetNotes().add(cloneNote);
        }
       
        this.setDatasetOtherIds(new ArrayList<DatasetOtherId>());
        for(DatasetOtherId id: source.datasetOtherIds) {
            DatasetOtherId cloneId = new DatasetOtherId();
            cloneId.setAgency(id.getAgency());
            cloneId.setDisplayOrder(id.getDisplayOrder());
            cloneId.setMetadata(this);
            cloneId.setOtherId(id.getOtherId());
            this.getDatasetOtherIds().add(cloneId);
        }
        
        this.setDatasetOtherRefs(new ArrayList<DatasetOtherRef>());
        for(DatasetOtherRef ref: source.datasetOtherRefs) {
            DatasetOtherRef cloneRef = new DatasetOtherRef();
            cloneRef.setDisplayOrder(ref.getDisplayOrder());
            cloneRef.setMetadata(this);
            cloneRef.setText(ref.getText());
            this.getDatasetOtherRefs().add(cloneRef);
        }
        
        this.setDatasetProducers(new ArrayList<DatasetProducer>());
        for(DatasetProducer prod: source.datasetProducers) {
            DatasetProducer cloneProd = new DatasetProducer();
            cloneProd.setAbbreviation(prod.getAbbreviation());
            cloneProd.setAffiliation(prod.getAffiliation());
            cloneProd.setDisplayOrder(prod.getDisplayOrder());
            cloneProd.setLogo(prod.getLogo());
            cloneProd.setMetadata(this);
            cloneProd.setName(prod.getName());
            cloneProd.setUrl(prod.getUrl());
            this.getDatasetProducers().add(cloneProd);
        }
        
        
       this.setDatasetRelMaterials(new ArrayList<DatasetRelMaterial>());
       for(DatasetRelMaterial rel: source.datasetRelMaterials) {
            DatasetRelMaterial cloneRel = new DatasetRelMaterial();
            cloneRel.setDisplayOrder(rel.getDisplayOrder());
            cloneRel.setMetadata(this);
            cloneRel.setText(rel.getText());
            this.getDatasetRelMaterials().add(cloneRel);
        }
       
       this.setDatasetRelPublications(new ArrayList<DatasetRelPublication>());
        for(DatasetRelPublication rel: source.datasetRelPublications){
            DatasetRelPublication cloneRel = new DatasetRelPublication();
            cloneRel.setDisplayOrder(rel.getDisplayOrder());
            cloneRel.setMetadata(this);
            cloneRel.setText(rel.getText());
            cloneRel.setIdType(rel.getIdType());
            cloneRel.setIdNumber(rel.getIdNumber());
            cloneRel.setUrl(rel.getUrl());
            cloneRel.setReplicationData(rel.isReplicationData());            
            this.getDatasetRelPublications().add(cloneRel);
        }
        
        this.setDatasetRelDatasets(new ArrayList<DatasetRelDataset>());
        for(DatasetRelDataset rel: source.datasetRelDatasets){
            DatasetRelDataset cloneRel = new DatasetRelDataset();
            cloneRel.setDisplayOrder(rel.getDisplayOrder());
            cloneRel.setMetadata(this);
            cloneRel.setText(rel.getText());
            this.getDatasetRelDatasets().add(cloneRel);
        }
        
        this.setDatasetSoftware(new ArrayList<DatasetSoftware>());
        for(DatasetSoftware soft: source.datasetSoftware){
            DatasetSoftware cloneSoft = new DatasetSoftware();
            cloneSoft.setDisplayOrder(soft.getDisplayOrder());
            cloneSoft.setMetadata(this);
            cloneSoft.setName(soft.getName());
            cloneSoft.setSoftwareVersion(soft.getSoftwareVersion());
            this.getDatasetSoftware().add(cloneSoft);
        }



        this.setDatasetGrants(new ArrayList<DatasetGrant>());
        for(DatasetGrant grant: source.datasetGrants) {
            DatasetGrant cloneGrant = new DatasetGrant();
            cloneGrant.setAgency(grant.getAgency());
            cloneGrant.setDisplayOrder(grant.getDisplayOrder());
            cloneGrant.setMetadata(this);
            cloneGrant.setNumber(grant.getNumber());
            this.getDatasetGrants().add(cloneGrant);
        }


        this.setDatasetTopicClasses(new ArrayList<DatasetTopicClass>());
        for (DatasetTopicClass topic: source.datasetTopicClasses){
            DatasetTopicClass cloneTopic = new DatasetTopicClass();
            cloneTopic.setDisplayOrder(topic.getDisplayOrder());
            cloneTopic.setMetadata(this);
            cloneTopic.setValue(topic.getValue());
            cloneTopic.setVocab(topic.getVocab());
            cloneTopic.setVocabURI(topic.getVocabURI());
            this.getDatasetTopicClasses().add(cloneTopic);
        }
        

        this.setDatasetFieldValues(new ArrayList<DatasetFieldValue>());
        for (DatasetFieldValue dsfv: source.getDatasetFieldValues()){
            DatasetFieldValue cloneDsfv = new DatasetFieldValue();
            cloneDsfv.setDisplayOrder(dsfv.getDisplayOrder());
            cloneDsfv.setDatasetField(dsfv.getDatasetField());
            
            cloneDsfv.setStrValue(dsfv.getStrValue());
            cloneDsfv.setMetadata(this);
            this.getDatasetFieldValues().add(cloneDsfv);
        }
       
    } 
       
    public String getAuthorsStr() {
        return getAuthorsStr(true);
    }

    public String getAuthorsStr(boolean affiliation) {
        String str="";
        for (DatasetAuthor sa : getDatasetAuthors()){
            if (str.trim().length()>1) {
                str+="; ";
            }
            str += sa.getName();
            if (affiliation) {
                if (!StringUtil.isEmpty(sa.getAffiliation())) {
                    str+=" ("+sa.getAffiliation()+")";
                }
            }           
        }
        return str;       
    }

    public String getDistributorNames() {
        String str="";
        for (DatasetDistributor sd : this.getDatasetDistributors()){
        if (str.trim().length()>1) {
                str+=";";
            }
            str += sd.getName();   
        }
        return str;       
    }
   
    @Column(columnDefinition="TEXT")
    private String UNF;
  
    public String getUNF() {
        return UNF;
    }
    
    public void setUNF(String UNF) {
        this.UNF=UNF;
    }
    
    @Column(columnDefinition="TEXT")
    private String title;
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    /* DatasetFieldValues - Holds list of values for domain specific metadata
    */
    @OneToMany (mappedBy="metadata", cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST})
    @OrderBy ("displayOrder")
    private List<DatasetFieldValue> datasetFieldValues;
    public List<DatasetFieldValue> getDatasetFieldValues() {
        return datasetFieldValues;
    }
    public void setDatasetFieldValues(List<DatasetFieldValue> datasetFieldValues) {
        this.datasetFieldValues = datasetFieldValues;
    }
    
    @OneToMany(mappedBy="metadata", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder")
    private java.util.List<DatasetAuthor> datasetAuthors;
    public java.util.List<DatasetAuthor> getDatasetAuthors() {
        return datasetAuthors;
    }    
    public void setDatasetAuthors(java.util.List<DatasetAuthor> datasetAuthors) {
        this.datasetAuthors = datasetAuthors;
    }
    
    @OneToMany(mappedBy="metadata", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder")
    private java.util.List<DatasetKeyword> datasetKeywords;
    
    public java.util.List<DatasetKeyword> getDatasetKeywords() {
        return datasetKeywords;
    }    
    public void setDatasetKeywords(java.util.List<DatasetKeyword> datasetKeywords) {
        this.datasetKeywords = datasetKeywords;
    }

    @Version
    private Long version;    
    public Long getVersion() {
        return this.version;
    }
    public void setVersion(Long version) {
        this.version = version;
    }

    @OneToMany(mappedBy="metadata", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder")
    private List<DatasetProducer> datasetProducers;

    public List<DatasetProducer> getDatasetProducers() {
        return this.datasetProducers;
    }

    public void setDatasetProducers(List<DatasetProducer> datasetProducers) {
        this.datasetProducers = datasetProducers;
    }

    @OneToMany(mappedBy="metadata", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder")
    private List<DatasetSoftware> datasetSoftware;
   
    public List<DatasetSoftware> getDatasetSoftware() {
        return this.datasetSoftware;
    }

    public void setDatasetSoftware(List<DatasetSoftware> datasetSoftware) {
        this.datasetSoftware = datasetSoftware;
    }



    @OneToMany(mappedBy="metadata", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder")
    private List<DatasetDistributor> datasetDistributors;
    public List<DatasetDistributor> getDatasetDistributors() {
        return this.datasetDistributors;
    }  
    public void setDatasetDistributors(List<DatasetDistributor> datasetDistributors) {
        this.datasetDistributors = datasetDistributors;
    }    
    
    @OneToMany(mappedBy="metadata", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder")
    private List<DatasetAbstract> datsetAbstracts;
    public List<DatasetAbstract> getDatasetAbstracts() {
        return this.datsetAbstracts;
    }
    public void setDatasetAbstracts(List<DatasetAbstract> datsetAbstracts) {
        this.datsetAbstracts = datsetAbstracts;
    }
    


    @OneToMany(mappedBy="metadata", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder")
    private List<DatasetTopicClass> datasetTopicClasses;

    public List<DatasetTopicClass> getDatasetTopicClasses() {
        return this.datasetTopicClasses;
    }

    public void setDatasetTopicClasses(List<DatasetTopicClass> datasetTopicClasses) {
        this.datasetTopicClasses = datasetTopicClasses;
    }



    @OneToMany(mappedBy="metadata", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder")
    private List<DatasetNote> datasetNotes;

    public List<DatasetNote> getDatasetNotes() {
        return this.datasetNotes;
    }

    public void setDatasetNotes(List<DatasetNote> datasetNotes) {
        this.datasetNotes = datasetNotes;
    }   

    @Column(columnDefinition="TEXT")    
    private String fundingAgency;
    public String getFundingAgency() {
        return this.fundingAgency;
    }
    public void setFundingAgency(String fundingAgency) {
        this.fundingAgency = fundingAgency;
    }

    @Column(columnDefinition="TEXT")   
    private String seriesName;
    public String getSeriesName() {
        return this.seriesName;
    }
    public void setSeriesName(String seriesName) {
        this.seriesName = seriesName;
    }    

    @Column(columnDefinition="TEXT")
    private String seriesInformation;
    public String getSeriesInformation() {
        return this.seriesInformation;
    }
    public void setSeriesInformation(String seriesInformation) {
        this.seriesInformation = seriesInformation;
    }
    
    @Column(name="datasetVersion",columnDefinition="TEXT")
    private String datasetVersionText;
    public String getDatasetVersionText() {
        return this.datasetVersionText;
    }
    public void setDatasetVersionText(String studyVersionText) {
        this.datasetVersionText = datasetVersionText;
    }    

    @Column(columnDefinition="TEXT")
    private String placeOfAccess;
    public String getPlaceOfAccess() {
        return this.placeOfAccess;
    }
    public void setPlaceOfAccess(String placeOfAccess) {
        this.placeOfAccess = placeOfAccess;
    }
    
    @Column(columnDefinition="TEXT")
    private String originalArchive;
    public String getOriginalArchive() {
        return this.originalArchive;
    }
    public void setOriginalArchive(String originalArchive) {
        this.originalArchive = originalArchive;
    }
    
    @Column(columnDefinition="TEXT")
    private String availabilityStatus;
    public String getAvailabilityStatus() {
        return this.availabilityStatus;
    }
    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }
    
    @Column(columnDefinition="TEXT")
    private String collectionSize;
    public String getCollectionSize() {
        return this.collectionSize;
    }
    public void setCollectionSize(String collectionSize) {
        this.collectionSize = collectionSize;
    }

    @Column(columnDefinition="TEXT")
    private String datasetCompletion;
    public String getDatasetCompletion() {
        return this.datasetCompletion;
    }
    public void setDatasetCompletion(String datasetCompletion) {
        this.datasetCompletion = datasetCompletion;
    }
    
    @Column(columnDefinition="TEXT")
    private String specialPermissions;
    public String getSpecialPermissions() {
        return this.specialPermissions;
    }
    public void setSpecialPermissions(String specialPermissions) {
        this.specialPermissions = specialPermissions;
    }    

    @Column(columnDefinition="TEXT")
    private String restrictions;
    public String getRestrictions() {
        return this.restrictions;
    }
    public void setRestrictions(String restrictions) {
        this.restrictions = restrictions;
    }
    
    @Column(columnDefinition="TEXT")
    private String contact;
    public String getContact() {
        return this.contact;
    }
    public void setContact(String contact) {
        this.contact = contact;
    }

    @Column(columnDefinition="TEXT")
    private String citationRequirements;
    public String getCitationRequirements() {
        return this.citationRequirements;
    }
    public void setCitationRequirements(String citationRequirements) {
        this.citationRequirements = citationRequirements;
    }

    @Column(columnDefinition="TEXT")
    private String depositorRequirements;
    public String getDepositorRequirements() {
        return this.depositorRequirements;
    }
    public void setDepositorRequirements(String depositorRequirements) {
        this.depositorRequirements = depositorRequirements;
    }

    @Column(columnDefinition="TEXT")
    private String conditions;
    public String getConditions() {
        return this.conditions;
    }
    public void setConditions(String conditions) {
        this.conditions = conditions;
    }
    
    @Column(columnDefinition="TEXT")
    private String disclaimer;
    public String getDisclaimer() {
        return this.disclaimer;
    }
    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    @Column(columnDefinition="TEXT")
    private String productionDate;
    public String getProductionDate() {
        return this.productionDate;
    }
    public void setProductionDate(String productionDate) {
        this.productionDate = productionDate;
    }
    
    //TODO - make sure getCitation works
    private String getYearForCitation(String dateString){
        //get date to first dash only
        if (dateString.indexOf("-") > -1){
            return dateString.substring( 0 , dateString.indexOf("-"));
        }
        return dateString;
    }

    @Column(columnDefinition="TEXT")
    private String productionPlace;
    public String getProductionPlace() {
        return this.productionPlace;
    }
    public void setProductionPlace(String productionPlace) {
        this.productionPlace = productionPlace;
    }

    @Column(columnDefinition="TEXT")
    private String confidentialityDeclaration;
    public String getConfidentialityDeclaration() {
        return this.confidentialityDeclaration;
    }
    public void setConfidentialityDeclaration(String confidentialityDeclaration) {
        this.confidentialityDeclaration = confidentialityDeclaration;
    }
    
    @OneToMany(mappedBy="metadata", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder")
    private List<DatasetGrant> datasetGrants;

    public List<DatasetGrant> getDatasetGrants() {
        return this.datasetGrants;
    }

    public void setDatasetGrants(List<DatasetGrant> datasetGrants) {
        this.datasetGrants = datasetGrants;
    }

    
    @Column(columnDefinition="TEXT")
    private String distributionDate;
    public String getDistributionDate() {
        return this.distributionDate;
    }
    public void setDistributionDate(String distributionDate) {
        this.distributionDate = distributionDate;
    }

    @Column(columnDefinition="TEXT")
    private String distributorContact;
    public String getDistributorContact() {
        return this.distributorContact;
    }
    public void setDistributorContact(String distributorContact) {
        this.distributorContact = distributorContact;
    }

    @Column(columnDefinition="TEXT")
    private String distributorContactAffiliation;
    public String getDistributorContactAffiliation() {
        return this.distributorContactAffiliation;
    }
    public void setDistributorContactAffiliation(String distributorContactAffiliation) {
        this.distributorContactAffiliation = distributorContactAffiliation;
    }
    
    @Column(columnDefinition="TEXT")
    private String distributorContactEmail;
    public String getDistributorContactEmail() {
        return this.distributorContactEmail;
    }
    public void setDistributorContactEmail(String distributorContactEmail) {
        this.distributorContactEmail = distributorContactEmail;
    }

    @Column(columnDefinition="TEXT")
    private String depositor;
    public String getDepositor() {
        return this.depositor;
    }
    public void setDepositor(String depositor) {
        this.depositor = depositor;
    }

    @Column(columnDefinition="TEXT")
    private String dateOfDeposit;
    public String getDateOfDeposit() {
        return this.dateOfDeposit;
    }
    public void setDateOfDeposit(String dateOfDeposit) {
        this.dateOfDeposit = dateOfDeposit;
    }

    @OneToMany(mappedBy="metadata", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder")
    private List<DatasetOtherId> datasetOtherIds;
    public List<DatasetOtherId> getDatasetOtherIds() {
        return this.datasetOtherIds;
    }
    public void setDatasetOtherIds(List<DatasetOtherId> datasetOtherIds) {
        this.datasetOtherIds = datasetOtherIds;
    }
 
    @Column(columnDefinition="TEXT")
    private String versionDate;
    public String getVersionDate() {
        return this.versionDate;
    }
    public void setVersionDate(String versionDate) {
        this.versionDate = versionDate;
    }
    
    @Column(columnDefinition="TEXT")
    private String subTitle;
    public String getSubTitle() {
        return this.subTitle;
    }
    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }
    
    @OneToMany(mappedBy="metadata", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder")
    private List<DatasetOtherRef> datasetOtherRefs;
    public List<DatasetOtherRef> getDatasetOtherRefs() {
        return this.datasetOtherRefs;
    }
    public void setDatasetOtherRefs(List<DatasetOtherRef> datasetOtherRefs) {
        this.datasetOtherRefs = datasetOtherRefs;
    }

    @OneToMany(mappedBy="metadata", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder")
    private List<DatasetRelMaterial> datasetRelMaterials;
    public List<DatasetRelMaterial> getDatasetRelMaterials() {
        return this.datasetRelMaterials;
    }
    public void setDatasetRelMaterials(List<DatasetRelMaterial> datasetRelMaterials) {
        this.datasetRelMaterials = datasetRelMaterials;
    }
    
    @OneToMany(mappedBy="metadata", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder")
    private List<DatasetRelPublication> datasetRelPublications;
    public List<DatasetRelPublication> getDatasetRelPublications() {
        return this.datasetRelPublications;
    }
    public void setDatasetRelPublications(List<DatasetRelPublication> datasetRelPublications) {
        this.datasetRelPublications = datasetRelPublications;
    }
    
    @OneToMany(mappedBy="metadata", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder")
    private List<DatasetRelDataset> datasetRelDatasets;
    public List<DatasetRelDataset> getDatasetRelDatasets() {
        return this.datasetRelDatasets;
    }
    public void setDatasetRelDatasets(List<DatasetRelDataset> datasetRelDatasets) {
        this.datasetRelDatasets = datasetRelDatasets;
    }
    

     public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Metadata)) {
            return false;
        }
        Metadata other = (Metadata)object;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) return false;
        return true;
    }

  
    
    private String harvestHoldings;

    public String getHarvestHoldings() {
        return harvestHoldings;
    }

    public void setHarvestHoldings(String harvestHoldings) {
        this.harvestHoldings = harvestHoldings;
    }



    @Column(columnDefinition="TEXT")
    private String harvestDVTermsOfUse;

    @Column(columnDefinition="TEXT")
    private String harvestDVNTermsOfUse;

    public String getHarvestDVTermsOfUse() {
        return harvestDVTermsOfUse;
    }

    public void setHarvestDVTermsOfUse(String harvestDVTermsOfUse) {
        this.harvestDVTermsOfUse = harvestDVTermsOfUse;
    }

    public String getHarvestDVNTermsOfUse() {
        return harvestDVNTermsOfUse;
    }

    public void setHarvestDVNTermsOfUse(String harvestDVNTermsOfUse) {
        this.harvestDVNTermsOfUse = harvestDVNTermsOfUse;
    }

    private String getCitation() {
        return getCitation(true);
    }

    public String getTextCitation() {
        return getCitation(false);
    }

    public String getWebCitation() {
        return getCitation(true);
    }
    
    public String getCitation(boolean isOnlineVersion) {

        Dataset dataset = getDataset();

        String str = "";
        /*
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
        if (!StringUtil.isEmpty(getTitle())) {
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            str += "\"" + getTitle() + "\"";
        }
        if (!StringUtil.isEmpty(study.getStudyId())) {
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            if (isOnlineVersion) {
                str += "<a href=\"" + study.getPersistentURL() + "\">" + study.getGlobalId() + "</a>";
            } else {
                str += study.getPersistentURL();
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

        if (getStudyVersion().getVersionNumber() != null) {
            str += " V" + getStudyVersion().getVersionNumber();
            str += " [Version]";
        }
*/
        return str;
    }

    public boolean isTermsOfUseEnabled() {
        // we might make this a true boolean stored in the db at some point;
        // for now, just check if any of the "terms of use" fields are not empty

        // terms of use fields are those from the "use statement" part of the ddi
        if ( !StringUtil.isEmpty(getConfidentialityDeclaration()) ) { return true; }
        if ( !StringUtil.isEmpty(getSpecialPermissions()) ) { return true; }
        if ( !StringUtil.isEmpty(getRestrictions()) ) { return true; }
        if ( !StringUtil.isEmpty(getContact()) ) { return true; }
        if ( !StringUtil.isEmpty(getCitationRequirements()) ) { return true; }
        if ( !StringUtil.isEmpty(getDepositorRequirements()) ) { return true; }
        if ( !StringUtil.isEmpty(getConditions()) ) { return true; }
        if ( !StringUtil.isEmpty(getDisclaimer()) ) { return true; }
        if ( !StringUtil.isEmpty(getHarvestDVNTermsOfUse()) ) { return true; }
        if ( !StringUtil.isEmpty(getHarvestDVTermsOfUse()) ) { return true; }

        return false;
    }
    
    // Return all the Terms of Use-related metadata fields concatenated as 
    // one string, if available: 
    
    public String getTermsOfUseAsString() {
        String touString = ""; 
        
        if ( !StringUtil.isEmpty(getConfidentialityDeclaration()) ) { 
            touString = touString.concat(getConfidentialityDeclaration());
        }
        if ( !StringUtil.isEmpty(getSpecialPermissions()) ) { 
            touString = touString.concat(getSpecialPermissions()); 
        }
        if ( !StringUtil.isEmpty(getRestrictions()) ) { 
            touString = touString.concat(getRestrictions()); 
        }
        if ( !StringUtil.isEmpty(getContact()) ) { 
            touString = touString.concat(getContact()); 
        }
        if ( !StringUtil.isEmpty(getCitationRequirements()) ) { 
            touString = touString.concat(getCitationRequirements()); 
        }
        if ( !StringUtil.isEmpty(getDepositorRequirements()) ) { 
            touString = touString.concat(getDepositorRequirements());
        }
        if ( !StringUtil.isEmpty(getConditions()) ) { 
            touString = touString.concat(getConditions());
        }
        if ( !StringUtil.isEmpty(getDisclaimer()) ) { 
            touString = touString.concat(getDisclaimer()); 
        }
        
        return !StringUtil.isEmpty(touString) ? touString : null; 
    }

    public Dataset getDataset() {
        return getDatasetVersion().getDataset();
    }

    /**
     *  This method populates the dependent collections with at least one element.
     *  It's necessary to do this before displaying the metadata in a form, because
     *  we need empty elements for the users to enter data into.
     */
    public void initCollections() {  
        
        if ( this.getDatasetAuthors()==null || this.getDatasetAuthors().isEmpty()) {
            List authors = new ArrayList();
            DatasetAuthor anAuthor = new DatasetAuthor();
            anAuthor.setMetadata(this);
            authors.add(anAuthor);
            this.setDatasetAuthors(authors);
        }
        
        if (this.getDatasetDistributors()==null || this.getDatasetDistributors().isEmpty()) {
            List distributors = new ArrayList();
            DatasetDistributor elem = new DatasetDistributor();
            elem.setMetadata(this);
            distributors.add(elem);
            this.setDatasetDistributors(distributors);
        }        
        
        if (this.getDatasetKeywords()==null || this.getDatasetKeywords().size()==0 ) {
            List keywords = new ArrayList();
            DatasetKeyword elem = new DatasetKeyword();
            elem.setMetadata(this);
            keywords.add(elem);
            this.setDatasetKeywords(keywords);
        }
        
        if (this.getDatasetNotes()==null || this.getDatasetNotes().size()==0) {
            List notes = new ArrayList();
            DatasetNote elem = new DatasetNote();
            elem.setMetadata(this);
            notes.add(elem);
            this.setDatasetNotes(notes);
        }
        
        if ( this.getDatasetOtherIds()==null || this.getDatasetOtherIds().size()==0) {
            DatasetOtherId elem = new DatasetOtherId();
            elem.setMetadata(this);
            List otherIds = new ArrayList();
            otherIds.add(elem);
            this.setDatasetOtherIds(otherIds);
        }
        
        if (this.getDatasetOtherRefs()==null || this.getDatasetOtherRefs().size()==0) {
            List list = new ArrayList();
            DatasetOtherRef elem = new DatasetOtherRef();
            elem.setMetadata(this);
            list.add(elem);
            this.setDatasetOtherRefs(list);
        }

        if (this.getDatasetProducers()==null || this.getDatasetProducers().size()==0) {
            List producers = new ArrayList();
            DatasetProducer elem = new DatasetProducer();
            elem.setMetadata(this);
            producers.add(elem);
            this.setDatasetProducers(producers);
        }
        
        if (this.getDatasetRelMaterials()==null || this.getDatasetRelMaterials().size()==0) {
            List mats = new ArrayList();
            DatasetRelMaterial elem = new DatasetRelMaterial();
            elem.setMetadata(this);
            mats.add(elem);
            this.setDatasetRelMaterials(mats);
        }
        
        
        if (this.getDatasetRelPublications()==null || this.getDatasetRelPublications().size()==0) {
            List list = new ArrayList();
            DatasetRelPublication elem = new DatasetRelPublication();
            elem.setMetadata(this);
            list.add(elem);
            this.setDatasetRelPublications(list);
        }
        
        if (this.getDatasetRelDatasets()==null || this.getDatasetRelDatasets().size()==0) {
            List list = new ArrayList();
            DatasetRelDataset elem = new DatasetRelDataset();
            elem.setMetadata(this);
            list.add(elem);
            this.setDatasetRelDatasets(list);
        }
        
        if (this.getDatasetTopicClasses()==null || this.getDatasetTopicClasses().size()==0 ) {
            List topicClasses = new ArrayList();
            DatasetTopicClass elem = new DatasetTopicClass();
            elem.setMetadata(this);
            topicClasses.add(elem);
            this.setDatasetTopicClasses(topicClasses);
        }
        
        if (this.getDatasetSoftware()==null || this.getDatasetSoftware().size()==0) {
            List software = new ArrayList();
            DatasetSoftware elem = new DatasetSoftware();
            elem.setMetadata(this);
            software.add(elem);
            this.setDatasetSoftware(software);
        }
                
       if ( this.getDatasetAbstracts()==null || this.getDatasetAbstracts().size()==0) {
            List abstracts = new ArrayList();
            DatasetAbstract elem = new DatasetAbstract();
            elem.setMetadata(this);
            abstracts.add(elem);
            this.setDatasetAbstracts(abstracts);
        }





        if (this.getDatasetGrants()==null || this.getDatasetGrants().size()==0) {
            List grants = new ArrayList();
            DatasetGrant elem = new DatasetGrant();
            elem.setMetadata(this);
            grants.add(elem);
            this.setDatasetGrants(grants);
        }

        
        // custom fields
        for (DatasetField sf : this.getDatasetFields()) {
            if (sf.getDatasetFieldValues()==null || sf.getDatasetFieldValues().size()==0) {
                List list = new ArrayList();
                DatasetFieldValue elem = new DatasetFieldValue();
                elem.setDatasetField(sf);
                elem.setMetadata(this);
                list.add(elem);
                sf.setDatasetFieldValues(list);
            }            
        }
               
    }
    
    
   public void setDisplayOrders() {
       
        int i = 0;
        for (DatasetAuthor elem : this.getDatasetAuthors()) {
            elem.setDisplayOrder(i++);
        }
                
        i = 0;
        for (DatasetDistributor elem : this.getDatasetDistributors()) {
            elem.setDisplayOrder(i++);
        }
        
        i = 0;
        for (DatasetKeyword elem : this.getDatasetKeywords()) {
            elem.setDisplayOrder(i++);
        }
    
        i = 0;
        for (DatasetNote elem : this.getDatasetNotes()) {
            elem.setDisplayOrder(i++);
        }
        
        i = 0;
        for (DatasetOtherId elem : this.getDatasetOtherIds()) {
            elem.setDisplayOrder(i++);
        }
        
        i = 0;
        for (DatasetOtherRef elem : this.getDatasetOtherRefs()) {
            elem.setDisplayOrder(i++);
        }
                
        i = 0;
        for (DatasetProducer elem : this.getDatasetProducers()) {
            elem.setDisplayOrder(i++);
        }
                
        i = 0;
        for (DatasetRelMaterial elem : this.getDatasetRelMaterials()) {
            elem.setDisplayOrder(i++);
        }
        
        
        i = 0;
        for (DatasetRelPublication elem : this.getDatasetRelPublications()) {
            elem.setDisplayOrder(i++);
        }       
        
        i = 0;
        for (DatasetRelDataset elem : this.getDatasetRelDatasets()) {
            elem.setDisplayOrder(i++);
        }
        
        i = 0;
        for (DatasetTopicClass elem : this.getDatasetTopicClasses()) {
            elem.setDisplayOrder(i++);
        }
        
        i = 0;
        for (DatasetSoftware elem : this.getDatasetSoftware()) {
            elem.setDisplayOrder(i++);
        }
        
        i = 0;
        for (DatasetAbstract elem : this.getDatasetAbstracts()) {
            elem.setDisplayOrder(i++);
        }
        
        for (DatasetField datasetField : this.getDatasetFields()) {
            i = 0;
            for (DatasetFieldValue elem : datasetField.getDatasetFieldValues()) {
                elem.setDisplayOrder(i++);
            }
        }
        
                i = 0;
        for (DatasetGrant elem : this.getDatasetGrants()) {
            elem.setDisplayOrder(i++);
        }
/*
       i = 0;
        for (StudyGeoBounding elem : this.getStudyGeoBoundings()) {
            elem.setDisplayOrder(i++);
        }

  
        */
    }
    
    // this is a transient list of the study fields, so we can initialize it on the first get and then store it here
    @Transient
    List<DatasetField> datasetFields;

    public List<DatasetField> getDatasetFields() {
        if (datasetFields == null || datasetFields.size() == 0) {
            datasetFields = new ArrayList();
            Template templateIn = this.getTemplate(); // != null ? this.getTemplate() : this.getDatasetVersion().getDataset().getTemplate();
            
            for (TemplateField tf : templateIn.getTemplateFields()) {
                DatasetField sf = tf.getDatasetField();
                if (sf.isCustomField()) {
                    List sfvList = new ArrayList();
                    // now iterate through values and map accordingly
                    if (datasetFieldValues != null){
                        for (DatasetFieldValue sfv : datasetFieldValues) {
                            if (sf.equals(sfv.getDatasetField())) {
                                sfvList.add(sfv);
                            }
                        }
                    }
                    sf.setDatasetFieldValues(sfvList);
                    datasetFields.add(sf);
                }
            }           
        }                
        return datasetFields;
    }
        
}
