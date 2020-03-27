ALTER TABLE datasetfieldtype
    ADD COLUMN inputrenderertype TEXT DEFAULT 'TEXT' NOT NULL;
ALTER TABLE datasetfieldtype
    ADD COLUMN inputrendereroptions TEXT DEFAULT '{}' NOT NULL;

UPDATE datasetfieldtype SET inputrenderertype='HTML_MARKUP' WHERE name='dsDescriptionValue';
UPDATE datasetfieldtype SET inputrenderertype='TEXTBOX' WHERE name='publicationCitation';
UPDATE datasetfieldtype SET inputrenderertype='TEXTBOX' WHERE name='notesText';
UPDATE datasetfieldtype SET inputrenderertype='TEXTBOX' WHERE name='relatedMaterial';
UPDATE datasetfieldtype SET inputrenderertype='TEXTBOX' WHERE name='relatedDatasets';
UPDATE datasetfieldtype SET inputrenderertype='TEXTBOX' WHERE name='dataSources';
UPDATE datasetfieldtype SET inputrenderertype='TEXTBOX' WHERE name='originOfSources';
UPDATE datasetfieldtype SET inputrenderertype='TEXTBOX' WHERE name='characteristicOfSources';
UPDATE datasetfieldtype SET inputrenderertype='TEXTBOX' WHERE name='accessToSources';
UPDATE datasetfieldtype SET inputrenderertype='VOCABULARY_SELECT' WHERE name='subject';
UPDATE datasetfieldtype SET inputrenderertype='VOCABULARY_SELECT' WHERE name='authorIdentifierScheme';
UPDATE datasetfieldtype SET inputrenderertype='VOCABULARY_SELECT' WHERE name='publicationIDType';
UPDATE datasetfieldtype SET inputrenderertype='VOCABULARY_SELECT' WHERE name='language';
UPDATE datasetfieldtype SET inputrenderertype='VOCABULARY_SELECT' WHERE name='contributorType';

UPDATE datasetfieldtype SET inputrenderertype='VOCABULARY_SELECT' WHERE name='country';

UPDATE datasetfieldtype SET inputrenderertype='TEXTBOX' WHERE name='unitOfAnalysis';
UPDATE datasetfieldtype SET inputrenderertype='TEXTBOX' WHERE name='universe';
UPDATE datasetfieldtype SET inputrenderertype='TEXTBOX' WHERE name='samplingProcedure';
UPDATE datasetfieldtype SET inputrenderertype='TEXTBOX' WHERE name='collectionMode';
UPDATE datasetfieldtype SET inputrenderertype='TEXTBOX' WHERE name='dataCollectionSituation';
UPDATE datasetfieldtype SET inputrenderertype='TEXTBOX' WHERE name='weighting';
UPDATE datasetfieldtype SET inputrenderertype='TEXTBOX' WHERE name='responseRate';
UPDATE datasetfieldtype SET inputrenderertype='TEXTBOX' WHERE name='socialScienceNotesText';

UPDATE datasetfieldtype SET inputrenderertype='VOCABULARY_SELECT' WHERE name='astroType';
UPDATE datasetfieldtype SET inputrenderertype='VOCABULARY_SELECT' WHERE name='astroFacility';
UPDATE datasetfieldtype SET inputrenderertype='VOCABULARY_SELECT' WHERE name='coverage.Spectral.Bandpass';

UPDATE datasetfieldtype SET inputrenderertype='VOCABULARY_SELECT' WHERE name='studyDesignType';
UPDATE datasetfieldtype SET inputrenderertype='VOCABULARY_SELECT' WHERE name='studyFactorType';
UPDATE datasetfieldtype SET inputrenderertype='VOCABULARY_SELECT' WHERE name='studyAssayOrganism';
UPDATE datasetfieldtype SET inputrenderertype='VOCABULARY_SELECT' WHERE name='studyAssayMeasurementType';
UPDATE datasetfieldtype SET inputrenderertype='VOCABULARY_SELECT' WHERE name='studyAssayTechnologyType';
UPDATE datasetfieldtype SET inputrenderertype='VOCABULARY_SELECT' WHERE name='studyAssayPlatform';

UPDATE datasetfieldtype SET inputrenderertype='VOCABULARY_SELECT' WHERE name='journalArticleType';


UPDATE datasetfieldtype SET inputrendereroptions='{"buttonActionHandler":"AddReplicationTextActionHandler","buttonActionTextKey":"dataset.AddReplication","actionForOperations":["CREATE_DATASET"]}'
    WHERE name='title';

ALTER TABLE datasetfieldtype ALTER COLUMN inputrenderertype drop default;
ALTER TABLE datasetfieldtype ALTER COLUMN inputrendereroptions drop default;


 