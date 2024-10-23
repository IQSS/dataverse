/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

/**
 *
 * @author skraffmiller
 */
@Named("dfc")
@Dependent
public class DatasetFieldConstant implements java.io.Serializable  {

    public static final String publication = "publication";
    public static final String otherId = "otherId";
    public static final String author =  "author";
    public static final String authorFirstName =  "authorFirstName";
    public static final String authorLastName =  "authorLastName";
    public static final String producer = "producer";
    public static final String software = "software";
    public static final String grantNumber = "grantNumber";
    public static final String distributor = "distributor";
    public static final String datasetContact = "datasetContact";
    public static final String datasetContactEmail = "datasetContactEmail";
    public static final String datasetContactName = "datasetContactName";
    public static final String datasetContactAffiliation = "datasetContactAffiliation";
    public static final String series = "series";
    public static final String datasetVersion = "datasetVersion";

    public static final String description = "dsDescription";
    public static final String keyword = "keyword";
    public static final String topicClassification = "topicClassification";
    public static final String geographicBoundingBox = "geographicBoundingBox";

    public static final String note = "note";

    public static final String publicationRelationType = "publicationRelationType";
    public static final String publicationCitation = "publicationCitation";
    public static final String publicationIDType = "publicationIDType";
    public static final String publicationIDNumber = "publicationIDNumber";
    public static final String publicationURL = "publicationURL";
    public static final String publicationReplicationData = "publicationReplicationData";

    public static final String title = "title";
    public static final String subTitle="subtitle"; //SEK 6-7-2016 to match what is in DB
    public static final String alternativeTitle="alternativeTitle"; //missing from class
    public static final String datasetId = "datasetId";
    public static final String authorName ="authorName";
    public static final String authorAffiliation = "authorAffiliation";
    public static final String authorIdType = "authorIdentifierScheme";
    public static final String authorIdValue = "authorIdentifier";
    public static final String otherIdValue="otherIdValue";
    public static final String otherIdAgency= "otherIdAgency";

    public static final String producerName="producerName";
    public static final String producerURL="producerURL";
    public static final String producerLogo="producerLogoURL";
    public static final String producerAffiliation="producerAffiliation";
    public static final String producerAbbreviation= "producerAbbreviation";
    public static final String productionDate="productionDate";
    public static final String productionPlace="productionPlace";
    public static final String softwareName="softwareName";
    public static final String softwareVersion="softwareVersion";
    public static final String fundingAgency="fundingAgency";
    public static final String grantNumberValue="grantNumberValue";
    public static final String grantNumberAgency="grantNumberAgency";
    public static final String distributorName="distributorName";
    public static final String distributorURL="distributorURL";
    public static final String distributorLogo="distributorLogoURL";
    public static final String distributionDate="distributionDate";
    public static final String distributorContactName="distributorContactName";
    public static final String distributorContactAffiliation="distributorContactAffiliation";
    public static final String distributorContactEmail="distributorContactEmail";
    public static final String distributorAffiliation="distributorAffiliation";
    public static final String distributorAbbreviation="distributorAbbreviation";

    public static final String contributor="contributor"; //SEK added for Dublin Core 6/22
    public static final String contributorType="contributorType";
    public static final String contributorName="contributorName";

    public static final String depositor="depositor";
    public static final String dateOfDeposit="dateOfDeposit";
    public static final String seriesName="seriesName";
    public static final String seriesInformation="seriesInformation";
    public static final String datasetVersionValue="datasetVersionValue";
    public static final String versionDate="versionDate";
    public static final String keywordValue="keywordValue";
    public static final String keywordTermURI="keywordTermURI";
    public static final String keywordVocab="keywordVocabulary";
    public static final String keywordVocabURI="keywordVocabularyURI";
    public static final String topicClassValue="topicClassValue";
    public static final String topicClassVocab="topicClassVocab";
    public static final String topicClassVocabURI="topicClassVocabURI";
    public static final String descriptionText="dsDescriptionValue";
    public static final String descriptionDate="dsDescriptionDate";
    public static final String timePeriodCovered="timePeriodCovered"; // SEK added 6/13/2016
    public static final String timePeriodCoveredStart="timePeriodCoveredStart";
    public static final String timePeriodCoveredEnd="timePeriodCoveredEnd";
    public static final String dateOfCollection="dateOfCollection"; // SEK added 6/13/2016
    public static final String dateOfCollectionStart="dateOfCollectionStart";
    public static final String dateOfCollectionEnd="dateOfCollectionEnd";
    public static final String country="country";
    public static final String geographicCoverage="geographicCoverage";
    public static final String otherGeographicCoverage="otherGeographicCoverage";
    public static final String city="city";  // SEK added 6/13/2016
    public static final String state="state";  // SEK added 6/13/2016
    public static final String geographicUnit="geographicUnit";
    public static final String westLongitude="westLongitude";
    public static final String eastLongitude="eastLongitude";
    public static final String northLatitude="northLatitude";
    public static final String southLatitude="southLatitude";
    public static final String unitOfAnalysis="unitOfAnalysis";
    public static final String universe="universe";
    public static final String kindOfData="kindOfData";
    public static final String timeMethod="timeMethod";
    public static final String dataCollector="dataCollector";
    public static final String collectorTraining="collectorTraining";
    public static final String frequencyOfDataCollection="frequencyOfDataCollection";
    public static final String samplingProcedure="samplingProcedure";
    public static final String targetSampleSize = "targetSampleSize";
    public static final String targetSampleActualSize = "targetSampleActualSize";
    public static final String targetSampleSizeFormula = "targetSampleSizeFormula";
    public static final String deviationsFromSampleDesign="deviationsFromSampleDesign";
    public static final String collectionMode="collectionMode";
    public static final String researchInstrument="researchInstrument";
    public static final String dataSources="dataSources";
    public static final String originOfSources="originOfSources";
    public static final String characteristicOfSources="characteristicOfSources";
    public static final String accessToSources="accessToSources";
    public static final String dataCollectionSituation="dataCollectionSituation";
    public static final String actionsToMinimizeLoss="actionsToMinimizeLoss";
    public static final String controlOperations="controlOperations";
    public static final String weighting="weighting";
    public static final String cleaningOperations="cleaningOperations";
    public static final String datasetLevelErrorNotes="datasetLevelErrorNotes";
    public static final String responseRate="responseRate";
    public static final String samplingErrorEstimates="samplingErrorEstimates";

    public static final String socialScienceNotes = "socialScienceNotes";
    public static final String socialScienceNotesType = "socialScienceNotesType";
    public static final String socialScienceNotesSubject = "socialScienceNotesSubject";
    public static final String socialScienceNotesText = "socialScienceNotesText";

    public static final String otherDataAppraisal="otherDataAppraisal";
    public static final String placeOfAccess="placeOfAccess";
    public static final String originalArchive="originalArchive";
    public static final String availabilityStatus="availabilityStatus";
    public static final String collectionSize="collectionSize";
    public static final String datasetCompletion="datasetCompletion";
    public static final String numberOfFiles="numberOfFiles";
    public static final String confidentialityDeclaration="confidentialityDeclaration";
    public static final String specialPermissions="specialPermissions";
    public static final String restrictions="restrictions";
    @Deprecated
    //Doesn't appear to be used and is not datasetContact
    public static final String contact="contact";
    public static final String citationRequirements="citationRequirements";
    public static final String depositorRequirements="depositorRequirements";
    public static final String conditions="conditions";
    public static final String disclaimer="disclaimer";
    public static final String relatedMaterial="relatedMaterial";
    //public static final String replicationFor="replicationFor";
    //public static final String relatedPublications="relatedPublications";
    public static final String relatedDatasets="relatedDatasets";
    public static final String otherReferences="otherReferences";
    public static final String notesText="notesText";
    public static final String language="language";
    public static final String noteInformationType="noteInformationType";
    public static final String notesInformationSubject="notesInformationSubject";
    public static final String subject="subject";
    /*
     * The following getters are needed so we can use them as properties in JSP 
     */
    
    public String getTitle() {
        return title;
    }
    
    public String getDatasetId() {
        return datasetId;
    }
    
    public String getAuthorName() {
        return authorName;
    }
    
    public String getAuthorLastName() {
        return authorLastName;
    }
    
    public String getAuthorFirstName() {
        return authorFirstName;
    }    
    public String getAuthorAffiliation() {
        return authorAffiliation;
    }
        
    public static String getAuthorIdType() {
        return authorIdType;
    }

    public static String getAuthorIdValue() {
        return authorIdValue;
    }
    
    public String getOtherId() {
        return otherId;
    }
    
    public String getOtherIdAgency() {
        return otherIdAgency;
    }
    
    public String getProducerName() {
        return producerName;
    }
    
    public String getProducerURL() {
        return producerURL;
    }
    
    public String getProducerLogo() {
        return producerLogo;
    }
    
    public String getProducerAbbreviation() {
        return producerAbbreviation;
    }
    
    public String getProductionDate() {
        return productionDate;
    }
    
    public String getSoftwareName() {
        return softwareName;
    }
    
    public String getSoftwareVersion() {
        return softwareVersion;
    }
    
    public String getFundingAgency() {
        return fundingAgency;
    }
    
    public String getGrantNumber() {
        return grantNumber;
    }
    
    public String getGrantNumberAgency() {
        return grantNumberAgency;
    }
    
    public String getDistributorName() {
        return distributorName;
    }
    
    public String getDistributorURL() {
        return distributorURL;
    }
    
    public String getDistributorLogo() {
        return distributorLogo;
    }
    
    public String getDistributionDate() {
        return distributionDate;
    }
    
    public String getDistributorContactName() {
        return distributorContactName;
    }
    
    public String getDistributorContactAffiliation() {
        return distributorContactAffiliation;
    }
    
    public String getDistributorContactEmail() {
        return distributorContactEmail;
    }
    
    public String getDepositor() {
        return depositor;
    }
    
    public String getDateOfDeposit() {
        return dateOfDeposit;
    }
    
    public String getSeriesName() {
        return seriesName;
    }
    
    public String getSeriesInformation() {
        return seriesInformation;
    }
    
    public String getDatasetVersion() {
        return datasetVersion;
    }
    
    public String getKeywordValue() {
        return keywordValue;
    }
    
    public String getKeywordVocab() {
        return keywordVocab;
    }
    
    public String getKeywordVocabURI() {
        return keywordVocabURI;
    }
    
    public String getTopicClassValue() {
        return topicClassValue;
    }
    
    public String getTopicClassVocab() {
        return topicClassVocab;
    }
    
    public String getTopicClassVocabURI() {
        return topicClassVocabURI;
    }
    
    public String getDescriptionText() {
        return descriptionText;
    }
    
    public String getDescriptionDate() {
        return descriptionDate;
    }
    
    public String getTimePeriodCoveredStart() {
        return timePeriodCoveredStart;
    }
    
    public String getTimePeriodCoveredEnd() {
        return timePeriodCoveredEnd;
    }
    
    public String getDateOfCollectionStart() {
        return dateOfCollectionStart;
    }
    
    public String getDateOfCollectionEnd() {
        return dateOfCollectionEnd;
    }
    
    public String getCountry() {
        return country;
    }
    
    public String getGeographicCoverage() {
        return geographicCoverage;
    }
    
    public String getGeographicUnit() {
        return geographicUnit;
    }
    
    public String getUnitOfAnalysis() {
        return unitOfAnalysis;
    }
    
    public String getUniverse() {
        return universe;
    }
    
    public String getKindOfData() {
        return kindOfData;
    }
    
    public String getTimeMethod() {
        return timeMethod;
    }
    
    public String getDataCollector() {
        return dataCollector;
    }
    
    public String getFrequencyOfDataCollection() {
        return frequencyOfDataCollection;
    }
    
    public String getSamplingProcedure() {
        return samplingProcedure;
    }
    
    public String getDeviationsFromSampleDesign() {
        return deviationsFromSampleDesign;
    }
    
    public String getCollectionMode() {
        return collectionMode;
    }
    
    public String getResearchInstrument() {
        return researchInstrument;
    }
    
    public String getDataSources() {
        return dataSources;
    }
    
    public String getOriginOfSources() {
        return originOfSources;
    }
    
    public String getCharacteristicOfSources() {
        return characteristicOfSources;
    }
    
    public String getAccessToSources() {
        return accessToSources;
    }
    
    public String getDataCollectionSituation() {
        return dataCollectionSituation;
    }
    
    public String getActionsToMinimizeLoss() {
        return actionsToMinimizeLoss;
    }
    
    public String getControlOperations() {
        return controlOperations;
    }
    
    public String getWeighting() {
        return weighting;
    }
    
    public String getCleaningOperations() {
        return cleaningOperations;
    }
    
    public String getDatasetLevelErrorNotes() {
        return datasetLevelErrorNotes;
    }
    
    public String getResponseRate() {
        return responseRate;
    }
    
    public String getSamplingErrorEstimates() {
        return samplingErrorEstimates;
    }
    
    public String getOtherDataAppraisal() {
        return otherDataAppraisal;
    }
    
    public String getPlaceOfAccess() {
        return placeOfAccess;
    }
    
    public String getOriginalArchive() {
        return originalArchive;
    }
    
    public String getAvailabilityStatus() {
        return availabilityStatus;
    }
    
    public String getCollectionSize() {
        return collectionSize;
    }
    
    public String getDatasetCompletion() {
        return datasetCompletion;
    }
    
    public String getConfidentialityDeclaration() {
        return confidentialityDeclaration;
    }
    
    public String getSpecialPermissions() {
        return specialPermissions;
    }
    
    public String getRestrictions() {
        return restrictions;
    }
    
    @Deprecated
    //Appears to not be used
    public String getContact() {
        return contact;
    }
    
    public String getCitationRequirements() {
        return citationRequirements;
    }
    
    public String getDepositorRequirements() {
        return depositorRequirements;
    }
    
    public String getConditions() {
        return conditions;
    }
    
    public String getDisclaimer() {
        return disclaimer;
    }
    
    public String getRelatedMaterial() {
        return relatedMaterial;
    }
    
    /*
    public String getRelatedPublications() {
        return relatedPublications;
    }*/
    
    public String getRelatedDatasets() {
        return relatedDatasets;
    }
    
    public String getOtherReferences() {
        return otherReferences;
    }
    
    public String getNotesText() {
        return notesText;
    }
    
    public String getNotesInformationType() {
        return noteInformationType;
    }
    
    public String getNotesInformationSubject() {
        return notesInformationSubject;
    }
    
    public String getProducerAffiliation() {
        return producerAffiliation;
    }
    
    public String getProductionPlace() {
        return productionPlace;
    }
    
    public String getDistributorAbbreviation() {
        return distributorAbbreviation;
    }

    public String getDistributorAffiliation() {
        return distributorAffiliation;
    }

    public String getVersionDate() {
        return versionDate;
    }

    public String getSubTitle() {
        return subTitle;
    }

    /*
    public String getReplicationFor() {
        return replicationFor;
    }*/

    public String getWestLongitude() {
        return westLongitude;
    }

    public String getEastLongitude() {
        return eastLongitude;
    }

    public String getNorthLatitude() {
        return northLatitude;
    }

    public String getSouthLatitude() {
        return southLatitude;
    }

    public String getNumberOfFiles() {
        return numberOfFiles;
    }

    public String getAuthor() {
        return author;
    }

    public String getDistributorContact() {
        return datasetContact;
    }

    public String getDescription() {
        return description;
    }

    public String getDistributor() {
        return distributor;
    }

    public String getGeographicBoundingBox() {
        return geographicBoundingBox;
    }

    public String getGrantNumberValue() {
        return grantNumberValue;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getNote() {
        return note;
    }

    public String getOtherIdValue() {
        return otherIdValue;
    }

    public String getProducer() {
        return producer;
    }

    public String getPublication() {
        return publication;
    }

    public String getPublicationCitation() {
        return publicationCitation;
    }

    public String getPublicationIDNumber() {
        return publicationIDNumber;
    }

    public String getPublicationIDType() {
        return publicationIDType;
    }

    public String getPublicationReplicationData() {
        return publicationReplicationData;
    }

    public String getPublicationURL() {
        return publicationURL;
    }

    public String getSeries() {
        return series;
    }

    public String getSoftware() {
        return software;
    }

    public String getTopicClassification() {
        return topicClassification;
    }

    public String getDatasetVersionValue() {
        return datasetVersionValue;
    }
    
    public String getSubject() {
        return subject;
    }
    
}
