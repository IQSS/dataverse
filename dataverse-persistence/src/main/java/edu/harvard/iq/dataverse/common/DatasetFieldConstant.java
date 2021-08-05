package edu.harvard.iq.dataverse.common;

/**
 * @author skraffmiller
 */
public class DatasetFieldConstant {

    public final static String publication = "publication";
    public final static String otherId = "otherId";
    public final static String author = "author";
    public final static String authorFirstName = "authorFirstName";
    public final static String authorLastName = "authorLastName";
    public final static String producer = "producer";
    public final static String software = "software";
    public final static String grantNumber = "grantNumber";
    public final static String distributor = "distributor";
    public final static String datasetContact = "datasetContact";
    public final static String datasetContactEmail = "datasetContactEmail";
    public final static String datasetContactName = "datasetContactName";
    public final static String datasetContactAffiliation = "datasetContactAffiliation";
    public final static String series = "series";
    public final static String datasetVersion = "datasetVersion";

    public final static String description = "dsDescription";
    public final static String keyword = "keyword";
    public final static String topicClassification = "topicClassification";
    public final static String geographicBoundingBox = "geographicBoundingBox";

    public final static String note = "note";

    public final static String publicationCitation = "publicationCitation";
    public final static String publicationIDType = "publicationIDType";
    public final static String publicationIDNumber = "publicationIDNumber";
    public final static String publicationURL = "publicationURL";
    public final static String publicationReplicationData = "publicationReplicationData";
    public final static String publicationRelationType = "publicationRelationType";


    public final static String title = "title";
    public final static String subTitle = "subtitle"; //SEK 6-7-2016 to match what is in DB
    public final static String alternativeTitle = "alternativeTitle"; //missing from class
    public final static String datasetId = "datasetId";
    public final static String authorName = "authorName";
    public final static String authorAffiliation = "authorAffiliation";
    public final static String authorAffiliationIdentifier = "authorAffiliationIdentifier";
    public final static String authorIdType = "authorIdentifierScheme";
    public final static String authorIdValue = "authorIdentifier";
    public final static String otherIdValue = "otherIdValue";
    public final static String otherIdAgency = "otherIdAgency";

    public final static String producerName = "producerName";
    public final static String producerURL = "producerURL";
    public final static String producerLogo = "producerLogoURL";
    public final static String producerAffiliation = "producerAffiliation";
    public final static String producerAbbreviation = "producerAbbreviation";
    public final static String productionDate = "productionDate";
    public final static String productionPlace = "productionPlace";
    public final static String softwareName = "softwareName";
    public final static String softwareVersion = "softwareVersion";
    public final static String fundingAgency = "fundingAgency";
    public final static String grantNumberValue = "grantNumberValue";
    public final static String grantNumberAgency = "grantNumberAgency";
    public final static String grantNumberAgencyIdentifier = "grantNumberAgencyIdentifier";
    public final static String grantNumberAgencyShortName = "grantNumberAgencyShortName";
    public final static String grantNumberProgram = "grantNumberProgram";
    public final static String distributorName = "distributorName";
    public final static String distributorURL = "distributorURL";
    public final static String distributorLogo = "distributorLogoURL";
    public final static String distributionDate = "distributionDate";
    public final static String distributorContactName = "distributorContactName";
    public final static String distributorContactAffiliation = "distributorContactAffiliation";
    public final static String distributorContactEmail = "distributorContactEmail";
    public final static String distributorAffiliation = "distributorAffiliation";
    public final static String distributorAbbreviation = "distributorAbbreviation";

    public final static String contributor = "contributor"; //SEK added for Dublin Core 6/22
    public final static String contributorType = "contributorType";
    public final static String contributorName = "contributorName";

    public final static String depositor = "depositor";
    public final static String dateOfDeposit = "dateOfDeposit";
    public final static String seriesName = "seriesName";
    public final static String seriesInformation = "seriesInformation";
    public final static String datasetVersionValue = "datasetVersionValue";
    public final static String versionDate = "versionDate";
    public final static String keywordValue = "keywordValue";
    public final static String keywordVocab = "keywordVocabulary"; //SEK 6/10/2016 to match what is in the db
    public final static String keywordVocabURI = "keywordVocabularyURI"; //SEK 6/10/2016 to match what is in the db
    public final static String topicClassValue = "topicClassValue";
    public final static String topicClassVocab = "topicClassVocab";
    public final static String topicClassVocabURI = "topicClassVocabURI";
    public final static String descriptionText = "dsDescriptionValue";
    public final static String descriptionDate = "dsDescriptionDate";
    public final static String timePeriodCovered = "timePeriodCovered"; // SEK added 6/13/2016
    public final static String timePeriodCoveredStart = "timePeriodCoveredStart";
    public final static String timePeriodCoveredEnd = "timePeriodCoveredEnd";
    public final static String dateOfCollection = "dateOfCollection"; // SEK added 6/13/2016
    public final static String dateOfCollectionStart = "dateOfCollectionStart";
    public final static String dateOfCollectionEnd = "dateOfCollectionEnd";
    public final static String country = "country";
    public final static String geographicCoverage = "geographicCoverage";
    public final static String otherGeographicCoverage = "otherGeographicCoverage";
    public final static String city = "city";  // SEK added 6/13/2016
    public final static String state = "state";  // SEK added 6/13/2016
    public final static String geographicUnit = "geographicUnit";
    public final static String westLongitude = "westLongitude";
    public final static String eastLongitude = "eastLongitude";
    public final static String northLatitude = "northLongitude"; //Changed to match DB - incorrectly entered into DB
    public final static String southLatitude = "southLongitude"; //Incorrect in DB
    public final static String unitOfAnalysis = "unitOfAnalysis";
    public final static String universe = "universe";
    public final static String kindOfData = "kindOfData";
    public final static String timeMethod = "timeMethod";
    public final static String dataCollector = "dataCollector";
    public final static String collectorTraining = "collectorTraining";
    public final static String frequencyOfDataCollection = "frequencyOfDataCollection";
    public final static String samplingProcedure = "samplingProcedure";
    public final static String targetSampleSize = "targetSampleSize";
    public final static String targetSampleActualSize = "targetSampleActualSize";
    /**
     * Field is not in the current social science metadata block
     * (it was replaced by {@value #targetSampleSizeAchieved}) which
     * have different meaning
     */
    @Deprecated
    public final static String targetSampleSizeFormula = "targetSampleSizeFormula";
    public final static String targetSampleSizeAchieved = "targetSampleSizeAchieved";
    public final static String deviationsFromSampleDesign = "deviationsFromSampleDesign";
    public final static String collectionMode = "collectionMode";
    public final static String researchInstrument = "researchInstrument";
    public final static String dataSources = "dataSources";
    public final static String originOfSources = "originOfSources";
    public final static String characteristicOfSources = "characteristicOfSources";
    public final static String accessToSources = "accessToSources";
    public final static String dataCollectionSituation = "dataCollectionSituation";
    public final static String actionsToMinimizeLoss = "actionsToMinimizeLoss";
    public final static String controlOperations = "controlOperations";
    public final static String weighting = "weighting";
    public final static String cleaningOperations = "cleaningOperations";
    public final static String datasetLevelErrorNotes = "datasetLevelErrorNotes";
    public final static String responseRate = "responseRate";
    public final static String samplingErrorEstimates = "samplingErrorEstimates";

    public final static String socialScienceNotes = "socialScienceNotes";
    public final static String socialScienceNotesType = "socialScienceNotesType";
    public final static String socialScienceNotesSubject = "socialScienceNotesSubject";
    public final static String socialScienceNotesText = "socialScienceNotesText";

    public final static String otherDataAppraisal = "otherDataAppraisal";
    public final static String placeOfAccess = "placeOfAccess";
    public final static String originalArchive = "originalArchive";
    public final static String availabilityStatus = "availabilityStatus";
    public final static String collectionSize = "collectionSize";
    public final static String datasetCompletion = "datasetCompletion";
    public final static String numberOfFiles = "numberOfFiles";
    public final static String confidentialityDeclaration = "confidentialityDeclaration";
    public final static String specialPermissions = "specialPermissions";
    public final static String restrictions = "restrictions";
    public final static String contact = "contact";
    public final static String citationRequirements = "citationRequirements";
    public final static String depositorRequirements = "depositorRequirements";
    public final static String conditions = "conditions";
    public final static String disclaimer = "disclaimer";
    public final static String relatedDataset = "relatedDataset";
    public final static String relatedDatasetCitation = "relatedDatasetCitation";
    public final static String relatedDatasetIDType = "relatedDatasetIDType";
    public final static String relatedDatasetIDNumber = "relatedDatasetIDNumber";
    public final static String relatedDatasetURL = "relatedDatasetURL";
    public final static String relatedDatasetRelationType = "relatedDatasetRelationType";
    public final static String relatedMaterial = "relatedMaterial";
    public final static String relatedMaterialCitation = "relatedMaterialCitation";
    public final static String relatedMaterialIDType = "relatedMaterialIDType";
    public final static String relatedMaterialIDNumber = "relatedMaterialIDNumber";
    public final static String relatedMaterialURL = "relatedMaterialURL";
    public final static String relatedMaterialRelationType = "relatedMaterialRelationType";
    //public final static String replicationFor="replicationFor";
    //public final static String relatedPublications="relatedPublications";
    public final static String relatedDatasets = "relatedDataset";
    public final static String otherReferences = "otherReferences";
    public final static String notesText = "notesText";
    public final static String language = "language";
    public final static String noteInformationType = "noteInformationType";
    public final static String notesInformationSubject = "notesInformationSubject";
    public final static String subject = "subject";


}
