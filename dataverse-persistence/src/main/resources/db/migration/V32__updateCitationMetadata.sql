
UPDATE controlledvocabularyvalue SET displaygroup=NULL WHERE datasetfieldtype_id=80 AND strvalue='Nauru';
UPDATE datasetfieldtype SET inputRendererType='TEXT' WHERE name='astroFacility';
UPDATE datasetfieldtype SET inputRendererType='TEXT' WHERE name='coverage.Spectral.Bandpass';


UPDATE datasetfieldtype SET inputRendererOptions='{}' WHERE name='title';
UPDATE datasetfieldtype SET
    description='In cases where a Dataset contains more than one description (for example, one might be supplied by the data producer and another prepared by the data repository where the data are deposited), the date attribute is used to distinguish between the two descriptions. Expected date formats are YYYY, YYYY-MM or YYYY-MM-DD.',
    watermark='YYYY, YYYY-MM or YYYY-MM-DD' WHERE name='dsDescriptionDate';
UPDATE datasetfieldtype SET watermark='http://... or https://...' WHERE name='keywordVocabularyURI';
UPDATE datasetfieldtype SET watermark='http://... or https://...' WHERE name='topicClassVocabURI';


UPDATE datasetfieldtype SET description='Publications related to this Dataset.' WHERE name='publication';

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedSearchFieldType,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('publicationRelationType', 'Relation Type', 'Type of relation between this dataset and related publication.', '', 'TEXT', 30, '(#VALUE.)', 
        TRUE, FALSE, FALSE, FALSE, TRUE, FALSE, 
        29, 'VOCABULARY_SELECT', '{}', 1, NULL);

UPDATE datasetfieldtype SET displayorder=31 WHERE name='publicationIDType';
UPDATE datasetfieldtype SET displayorder=32 WHERE name='publicationIDNumber';
UPDATE datasetfieldtype SET displayorder=33, displayOnCreate=TRUE, watermark='http://... or https://...'  WHERE name='publicationUrl';
UPDATE datasetfieldtype SET displayorder=34 WHERE name='notesText';

UPDATE datasetfieldtype SET displayorder=35, displayoncreate = true, inputRendererOptions='{"sortByLocalisedStringsOrder" : "true"}' WHERE name='language';

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedSearchFieldType,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('languageOfMetadata', 'Language of Metadata', 'Language of metadata describing this dataset.', '', 'TEXT', 36, '', 
        TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, 
        NULL, 'VOCABULARY_SELECT', '{"sortByLocalisedStringsOrder" : "true"}', 1, 'http://purl.org/dc/terms/language');

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedSearchFieldType,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('languageOfData', 'Language of Data Files', 'Language of data files within this dataset.', '', 'TEXT', 37, '', 
        TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, 
        NULL, 'VOCABULARY_SELECT', '{"sortByLocalisedStringsOrder" : "true"}', 1, 'http://purl.org/dc/terms/language');

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedSearchFieldType,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('languageOfDocumentation', 'Language of Documentation', 'Language in which the documentation is written.', '', 'TEXT', 38, '', 
        TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, 
        NULL, 'VOCABULARY_SELECT', '{"sortByLocalisedStringsOrder" : "true"}', 1, 'http://purl.org/dc/terms/language');

UPDATE datasetfieldtype SET displayorder=39 WHERE name='producer';
UPDATE datasetfieldtype SET displayorder=40 WHERE name='producerName';
UPDATE datasetfieldtype SET displayorder=41 WHERE name='producerAffiliation';
UPDATE datasetfieldtype SET displayorder=42 WHERE name='producerAbbreviation';
UPDATE datasetfieldtype SET displayorder=43, watermark='http://... or https://...' WHERE name='producerURL';
UPDATE datasetfieldtype SET displayorder=44, watermark='http://... or https://...' WHERE name='producerLogoURL';
UPDATE datasetfieldtype SET
    displayorder=45,
    description='Date when the data collection or other materials were produced (not distributed, published or archived). Expected date formats are YYYY, YYYY-MM or YYYY-MM-DD.',
    watermark='YYYY, YYYY-MM or YYYY-MM-DD' WHERE name='productionDate';

UPDATE datasetfieldtype SET displayorder=46 WHERE name='productionPlace';
UPDATE datasetfieldtype SET displayorder=47 WHERE name='contributor';


UPDATE datasetfieldtype SET displayorder=48, inputRendererOptions='{"sortByLocalisedStringsOrder" : "true"}' WHERE name='contributorType';

UPDATE datasetfieldtype SET displayorder=49 WHERE name='contributorName';

UPDATE datasetfieldtype SET
    description='Information regarding grant/funding.',
    fieldType='NONE',
    displayOrder=50,
    displayFormat=':',
    displayOnCreate=TRUE,
    inputRendererType='TEXT'
    WHERE name='grantNumber'; 

UPDATE datasetfieldtype SET
    description='Full name of the grant agency. For instance: European Commission, National Science Centre (Poland), National Centre for Research and Development (Poland), Wellcome Trust.',
    fieldType='TEXT',
    displayOrder=51,
    displayOnCreate=TRUE,
    inputRendererType='SUGGESTION_TEXT',
    inputRendererOptions='{suggestionFilteredBy:[], suggestionSourceClass:"GrantSuggestionHandler", suggestionSourceField:"grantNumberAgency"}'
    WHERE name='grantNumberAgency'; 

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedSearchFieldType,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('grantNumberAgencyShortName', 'Grant Agency Short Name', 'Short name of the grant agency, usually the acronym of its official full name. For instance: EC for European Commission, WT for Wellcome Trust, NCN for National Science Centre (Poland), NCBR for National Centre for Research and Development (Poland).', '', 'TEXT', 52, '(#VALUE)', 
        TRUE, FALSE, FALSE, TRUE, TRUE, FALSE, 
        47, 'SUGGESTION_TEXT', '{suggestionFilteredBy:["grantNumberAgency"], suggestionSourceClass:"GrantSuggestionHandler", suggestionSourceField:"grantNumberAgencyShortName"}', 1, NULL);

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedSearchFieldType,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('grantNumberProgram', 'Grant Program ', 'Funding program name. For instance: FP7, H2020, Sonata.', '', 'TEXT', 53, '#VALUE', 
        TRUE, FALSE, FALSE, TRUE, TRUE, FALSE, 
        47, 'SUGGESTION_TEXT', '{suggestionFilteredBy:["grantNumberAgency"], suggestionSourceClass:"GrantSuggestionHandler", suggestionSourceField:"grantNumberProgram"}', 1, NULL);

UPDATE datasetfieldtype SET
    description='Unique identifier in the scope of the grant agency, usually a grant or agreement number of the project.',
    displayOrder=54,
    displayOnCreate=TRUE
    WHERE name='grantNumberValue'; 

UPDATE datasetfieldtype SET displayorder=55 WHERE name='distributor';
UPDATE datasetfieldtype SET displayorder=56 WHERE name='distributorName';
UPDATE datasetfieldtype SET displayorder=57 WHERE name='distributorAffiliation';
UPDATE datasetfieldtype SET displayorder=58 WHERE name='distributorAbbreviation';
UPDATE datasetfieldtype SET displayorder=59, watermark='http://... or https://...' WHERE name='distributorURL';
UPDATE datasetfieldtype SET displayorder=60, watermark='http://... or https://...' WHERE name='distributorLogoURL';
UPDATE datasetfieldtype SET
    displayorder=61,
    description='Date that the work was made available for distribution/presentation. Expected date formats are YYYY, YYYY-MM or YYYY-MM-DD.',
    watermark='YYYY, YYYY-MM or YYYY-MM-DD' WHERE name='distributionDate';

UPDATE datasetfieldtype SET displayorder=62 WHERE name='depositor';
UPDATE datasetfieldtype SET
    displayorder=63,
    description='Date that the Dataset was deposited into the repository. Expected date formats are YYYY, YYYY-MM or YYYY-MM-DD.',
    watermark='YYYY, YYYY-MM or YYYY-MM-DD' WHERE name='dateOfDeposit';

UPDATE datasetfieldtype SET displayorder=64 WHERE name='timePeriodCovered';
UPDATE datasetfieldtype SET
    displayorder=65,
    description='Start date which reflects the time period covered by the data, not the dates of coding or making documents machine-readable or the dates the data were collected. Expected date formats are YYYY, YYYY-MM or YYYY-MM-DD.',
    watermark='YYYY, YYYY-MM or YYYY-MM-DD' WHERE name='timePeriodCoveredStart';
UPDATE datasetfieldtype SET
    displayorder=66,
    description='End date which reflects the time period covered by the data, not the dates of coding or making documents machine-readable or the dates the data were collected. Expected date formats are YYYY, YYYY-MM or YYYY-MM-DD.',
    watermark='YYYY, YYYY-MM or YYYY-MM-DD' WHERE name='timePeriodCoveredEnd';

UPDATE datasetfieldtype SET displayorder=67 WHERE name='dateOfCollection';
UPDATE datasetfieldtype SET
    displayorder=68,
    description='Date when the data collection started. Expected date formats are YYYY, YYYY-MM or YYYY-MM-DD.',
    watermark='YYYY, YYYY-MM or YYYY-MM-DD' WHERE name='dateOfCollectionStart';
UPDATE datasetfieldtype SET
    displayorder=69,
    description='Date when the data collection ended. Expected date formats are YYYY, YYYY-MM or YYYY-MM-DD.',
    watermark='YYYY, YYYY-MM or YYYY-MM-DD' WHERE name='dateOfCollectionEnd';

UPDATE datasetfieldtype SET
    displayorder=70, allowControlledVocabulary=TRUE, inputRendererType='VOCABULARY_SELECT', uri=NULL,
    description='General type of the data in the dataset.'
    WHERE name='kindOfData';
DELETE FROM datasetfield df WHERE df.datasetfieldtype_id=65;

UPDATE datasetfieldtype SET displayorder=71 WHERE name='series';
UPDATE datasetfieldtype SET displayorder=72 WHERE name='seriesName';
UPDATE datasetfieldtype SET displayorder=73 WHERE name='seriesInformation';
UPDATE datasetfieldtype SET displayorder=74 WHERE name='software';
UPDATE datasetfieldtype SET displayorder=75, allowControlledVocabulary=FALSE WHERE name='softwareName';
UPDATE datasetfieldtype SET displayorder=76 WHERE name='softwareVersion';

UPDATE datasetfieldtype SET
    name='relatedDataset',
    title='Related Dataset',
    fieldType='NONE',
    displayOrder=77,
    displayoncreate=TRUE,
    inputRendererType='TEXT'
    WHERE name='relatedDatasets';

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedSearchFieldType,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('relatedDatasetCitation', 'Citation', 'The full bibliographic citation for this related dataset.', '', 'TEXTBOX', 78, '#VALUE', 
        TRUE, FALSE, FALSE, FALSE, TRUE, FALSE, 
        73, 'TEXTBOX', '{}', 1, 'http://purl.org/dc/terms/bibliographicCitation');

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedSearchFieldType,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('relatedDatasetRelationType', 'Relation Type', 'Type of relation between the deposited and related datasets.', '', 'TEXT', 79, '(#VALUE.)', 
        TRUE, FALSE, FALSE, FALSE, TRUE, FALSE, 
        73, 'VOCABULARY_SELECT', '{}', 1, NULL);

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedSearchFieldType,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('relatedDatasetIDType', 'ID Type', 'The type of digital identifier of this related datset (e.g., Digital Object Identifier (DOI)).', '', 'TEXT', 80, '#VALUE:', 
        TRUE, TRUE, FALSE, FALSE, TRUE, FALSE, 
        73, 'VOCABULARY_SELECT', '{}', 1, 'http://purl.org/spar/datacite/ResourceIdentifierScheme');

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedSearchFieldType,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('relatedDatasetIDNumber', 'ID Number', 'The identifier for the selected ID type.', '', 'TEXT', 81, '#VALUE', 
        TRUE, FALSE, FALSE, FALSE, TRUE, FALSE, 
        73, 'TEXT', '{}', 1, 'http://purl.org/spar/datacite/ResourceIdentifier');

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedSearchFieldType,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('relatedDatasetURL', 'URL', 'Link to the web page where the related dataset is available.', 'http://... or https://...', 'URL', 82, '<a href="#VALUE" target="_blank">#VALUE</a>', 
        FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, 
        73, 'TEXT', '{}', 1, NULL);

UPDATE datasetfieldtype SET
    description='Any material related to the deposited dataset.',
    fieldType='NONE',
    displayOrder=83,
    inputRendererType='TEXT',
    uri='http://purl.org/dc/terms/relation'
    WHERE name='relatedMaterial';

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedSearchFieldType,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('relatedMaterialCitation', 'Citation', 'The full bibliographic citation for this related material.', '', 'TEXTBOX', 84, '#VALUE', 
        TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, 
        72, 'TEXTBOX', '{}', 1, 'http://purl.org/dc/terms/bibliographicCitation');

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedSearchFieldType,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('relatedMaterialRelationType', 'Relation Type', 'Type of relation between the deposited dataset and the related material.', '', 'TEXT', 85, '(#VALUE.)', 
        TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, 
        72, 'VOCABULARY_SELECT', '{}', 1, NULL);

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedSearchFieldType,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('relatedMaterialIDType', 'ID Type', 'The type of digital identifier used for the related material (e.g., Digital Object Identifier (DOI)).', '', 'TEXT', 86, '#VALUE:', 
        TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, 
        72, 'VOCABULARY_SELECT', '{}', 1, 'http://purl.org/spar/datacite/ResourceIdentifierScheme');

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedSearchFieldType,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('relatedMaterialIDNumber', 'ID Number', 'The identifier for the selected ID type.', '', 'TEXT', 87, '#VALUE', 
        TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, 
        72, 'TEXT', '{}', 1, 'http://purl.org/spar/datacite/ResourceIdentifier');

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedSearchFieldType,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('relatedMaterialURL', 'URL', 'Link to the web page where the related material is available.', 'http://... or https://...', 'URL', 88, '<a href="#VALUE" target="_blank">#VALUE</a>', 
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, 
        72, 'TEXT', '{}', 1, NULL);

UPDATE datasetfieldtype SET displayorder=89 WHERE name='otherReferences';
UPDATE datasetfieldtype SET displayorder=90 WHERE name='dataSources';
UPDATE datasetfieldtype SET displayorder=91 WHERE name='originOfSources';
UPDATE datasetfieldtype SET displayorder=92 WHERE name='characteristicOfSources';
UPDATE datasetfieldtype SET displayorder=93 WHERE name='accessToSources';

-- update order in publicationIDType vocabulary (doi first)
update controlledvocabularyvalue set displayorder=3 WHERE id = 18;
update controlledvocabularyvalue set displayorder=2 WHERE id = 17;
update controlledvocabularyvalue set displayorder=1 WHERE id = 16;
update controlledvocabularyvalue set displayorder=0 WHERE id = 19;

INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Numeric', 0, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'kindOfData');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Text', 1, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'kindOfData');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'StillImage', 2, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'kindOfData');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Geospatial', 3, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'kindOfData');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Audio', 4, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'kindOfData');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Video', 5, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'kindOfData');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Software', 6, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'kindOfData');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'InteractiveResource', 7, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'kindOfData');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'ThreeD', 8, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'kindOfData');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Other', 9, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'kindOfData');

INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'doi', 0, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'ark', 1, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'arXiv', 2, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'bibcode', 3, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'ean13', 4, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'eissn', 5, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'handle', 6, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'isbn', 7, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'issn', 8, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'istc', 9, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'lissn', 10, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'lsid', 11, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'pmid', 12, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'purl', 13, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'upc', 14, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'url', 15, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'urn', 16, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetIDType');

INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsCitedBy', 0, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'publicationRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsSupplementTo', 1, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'publicationRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsSupplementedBy', 2, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'publicationRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsPartOf', 3, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'publicationRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsReferencedBy', 4, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'publicationRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsDocumentedBy', 5, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'publicationRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Documents', 6, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'publicationRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsReviewedBy', 7, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'publicationRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsSourceOf', 8, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'publicationRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsCitedBy', 0, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Cites', 1, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsSupplementTo', 2, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsSupplementedBy', 3, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsContinuedBy', 4, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Continues', 5, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsNewVersionOf', 6, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsPreviousVersionOf', 7, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsPartOf', 8, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsReferencedBy', 9, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'References', 10, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsVariantFormOf', 11, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsOriginalFormOf', 12, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsIdenticalTo', 13, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsDerivedFrom', 14, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsSourceOf', 15, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedDatasetRelationType');

INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'doi', 0, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'ark', 1, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'arXiv', 2, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'bibcode', 3, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'ean13', 4, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'eissn', 5, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'handle', 6, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'isbn', 7, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'issn', 8, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'istc', 9, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'lissn', 10, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'lsid', 11, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'pmid', 12, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'purl', 13, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'upc', 14, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'url', 15, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialIDType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'urn', 16, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialIDType');

INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsCitedBy', 0, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Cites', 1, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsSupplementTo', 2, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsSupplementedBy', 3, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsContinuedBy', 4, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Continues', 5, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsNewVersionOf', 6, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsPreviousVersionOf', 7, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsPartOf', 8, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsReferencedBy', 9, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'References', 10, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsVariantFormOf', 11, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsOriginalFormOf', 12, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsIdenticalTo', 13, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsDerivedFrom', 14, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialRelationType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'IsSourceOf', 15, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'relatedMaterialRelationType');

INSERT INTO controlledvocabularyvalue (strvalue, displayorder, displayGroup, datasetfieldtype_id)
(SELECT cvv.strvalue, cvv.displayorder, cvv.displayGroup, dsft.id
    FROM controlledvocabularyvalue cvv JOIN datasetfieldtype dsft ON dsft.name = 'languageOfMetadata' WHERE cvv.datasetfieldtype_id=35);

INSERT INTO controlledvocabularyvalue (strvalue, displayorder, displayGroup, datasetfieldtype_id)
(SELECT cvv.strvalue, cvv.displayorder, cvv.displayGroup, dsft.id
    FROM controlledvocabularyvalue cvv JOIN datasetfieldtype dsft ON dsft.name = 'languageOfData' WHERE cvv.datasetfieldtype_id=35);

INSERT INTO controlledvocabularyvalue (strvalue, displayorder, displayGroup, datasetfieldtype_id)
(SELECT cvv.strvalue, cvv.displayorder, cvv.displayGroup, dsft.id
    FROM controlledvocabularyvalue cvv JOIN datasetfieldtype dsft ON dsft.name = 'languageOfDocumentation' WHERE cvv.datasetfieldtype_id=35);




INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Academy of Finland','AKA','','Academy of Finland','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Australian Research Council','ARC','ARC Centres of Excellences','Australian Research Council','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Australian Research Council','ARC','Discovery Early Career Researcher Award','Australian Research Council','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Australian Research Council','ARC','Discovery Projects','Australian Research Council','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Australian Research Council','ARC','Future Fellowships','Australian Research Council','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Australian Research Council','ARC','Industrial Transformation Research Hubs','Australian Research Council','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Australian Research Council','ARC','Linkage Projects','Australian Research Council','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Australian Research Council','ARC','Special Research Initiative (Antarctic)','Australian Research Council','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Austrian Science Fund','FWF','Einzelprojekte','Austrian Science Fund','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Austrian Science Fund','FWF','Internationale Projekte','Austrian Science Fund','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Austrian Science Fund','FWF','Open Research Data','Austrian Science Fund','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Austrian Science Fund','FWF','P','Austrian Science Fund','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('European Commission','EC','FP6','European Commission','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('European Commission','EC','FP7','European Commission','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('European Commission','EC','H2020','European Commission','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('European Commission','EC','FP6','Komisja Europejska','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('European Commission','EC','FP7','Komisja Europejska','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('European Commission','EC','H2020','Komisja Europejska','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Fundação para a Ciência e a Tecnologia','FCT','3599-PPCDT','Fundação para a Ciência e a Tecnologia','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Fundação para a Ciência e a Tecnologia','FCT','5646-ICCMS','Fundação para a Ciência e a Tecnologia','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Fundação para a Ciência e a Tecnologia','FCT','5876','Fundação para a Ciência e a Tecnologia','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Fundação para a Ciência e a Tecnologia','FCT','5876-PPCDTI','Fundação para a Ciência e a Tecnologia','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Fundação para a Ciência e a Tecnologia','FCT','COMPETE','Fundação para a Ciência e a Tecnologia','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Fundação para a Ciência e a Tecnologia','FCT','POCI','Fundação para a Ciência e a Tecnologia','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Fundação para a Ciência e a Tecnologia','FCT','SFRH','Fundação para a Ciência e a Tecnologia','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Ministry of Education, Science and Technological Development of Republic of Serbia','MESTD','Basic Research (BR or ON)','Ministry of Education, Science and Technological Development of Republic of Serbia','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Ministry of Education, Science and Technological Development of Republic of Serbia','MESTD','Integrated and Interdisciplinary Research (IIR or III)','Ministry of Education, Science and Technological Development of Republic of Serbia','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Ministry of Education, Science and Technological Development of Republic of Serbia','MESTD','Technological Development (TD or TR)','Ministry of Education, Science and Technological Development of Republic of Serbia','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Health and Medical Research Council Statistics','NHMRC','Career Development Fellowships','National Health and Medical Research Council Statistics','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Health and Medical Research Council Statistics','NHMRC','Early Career Fellowships','National Health and Medical Research Council Statistics','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Health and Medical Research Council Statistics','NHMRC','NHMRC Enabling Grants','National Health and Medical Research Council Statistics','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Health and Medical Research Council Statistics','NHMRC','NHMRC Project Grants','National Health and Medical Research Council Statistics','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Health and Medical Research Council Statistics','NHMRC','NHMRC Research Fellowships','National Health and Medical Research Council Statistics','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Health and Medical Research Council Statistics','NHMRC','Targeted Calls','National Health and Medical Research Council Statistics','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','AGENCY FOR HEALTHCARE RESEARCH AND QUALITY','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','DIVISION OF CANCER COLNTROL &POPULATION SCIENCE','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','DIVISION OF EPIDEMIOLOGY AND CLINICAL APPLICATIONS','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','EUNICE KENNEDY SHRIVER NATIONAL INSTITUTE OF CHILD HEALTH & HUMAN DEVELOPMENT','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','FOGARTY INTERNATIONAL CENTER','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','FOOD AND DRUG ADMINISTRATION','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL CANCER INSTITUTE','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL CENTER FOR ADVANCING TRANSLATIONAL SCIENCES','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL CENTER FOR CHRONIC DISEASE PREV AND HEALTH PROMO','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL CENTER FOR COMPLEMENTARY & ALTERNATIVE MEDICINE','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL CENTER FOR INJURY PREVENTION AND CONTROL','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL CENTER FOR RESEARCH RESOURCES','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL CENTER ON MINORITY HEALTH AND HEALTH DISPARITIES','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL EYE INSTITUTE','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL HEART, LUNG AND BLOOD INSTITUTE','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL HUMAN GENOME RESEARCH INSTITUTE','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL INSTITUTE OF ALLERGY AND INFECTIOUS DESEASES','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL INSTITUTE OF ARTHRITIS AND MUSCULOSKELETAL AND SKIN DISEASES','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL INSTITUTE OF BIOMEDICAL IMAGING AND BIOENGINEERING','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL INSTITUTE OF DENTAL & CRANIOFACIAL RESEARCH','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL INSTITUTE OF DIABETES AND DIGESTIVE AND KIDNEY DISEASES','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL INSTITUTE OF ENVIRONMENTAL HEALTH SCIENCES','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL INSTITUTE OF GENERAL MEDICAL SCIENCES','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL INSTITUTE OF MENTAL HEALTH','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL INSTITUTE OF NEUROLOGICAL DISORDERS AND STROKE','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL INSTITUTE OF NURSING RESEARCH','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL INSTITUTE ON AGING','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL INSTITUTE ON DEAFNESS AND OTHER COMMUNICATION DISORDERS','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL INSTITUTE ON DRUG ABUSE','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','NATIONAL INSTITUTE ON MINORITY HEALTH AND HEALTH DISPARITIES','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Institutes of Health','NIH','OFFICE OF THE DIRECTOR, NATIONAL INSTITUTES OF HEALTH','National Institutes of Health','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Foundation','NSF','','National Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Foundation','NSF','BIO/OAD','National Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Foundation','NSF','CISE/OAD','National Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Foundation','NSF','Directorate for Biological Sciences','National Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Foundation','NSF','Directorate for Mathematical & Physical Sciences','National Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Foundation','NSF','Directorate for Social, Behavioral & Economic Sciences','National Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Foundation','NSF','EHR/OAD','National Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Foundation','NSF','END/OAD','National Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Foundation','NSF','GEO/OAD','National Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Foundation','NSF','MPS/OAD','National Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Foundation','NSF','OD','National Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Foundation','NSF','SBE/OAD','National Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Netherlands Organisation for Scientific Research','NWO','','Netherlands Organisation for Scientific Research','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Research Council UK','RCUK','BBSRC','Research Council UK','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Research Council UK','RCUK','EPSRC','Research Council UK','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Research Council UK','RCUK','Innovative UK','Research Council UK','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Research Council UK','RCUK','MRC','Research Council UK','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Research Council UK','RCUK','NERC','Research Council UK','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Research Council UK','RCUK','STFC','Research Council UK','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Science Foundation Ireland','SFI','SFI Strategic Research Cluster','Science Foundation Ireland','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Swiss National Science Foundation','SNSF','Careers','Swiss National Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Swiss National Science Foundation','SNSF','Careers; Fellowships','Swiss National Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Swiss National Science Foundation','SNSF','Programmes','Swiss National Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Swiss National Science Foundation','SNSF','Programmes; National Research Programmes (NRPs)','Swiss National Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Swiss National Science Foundation','SNSF','Project funding','Swiss National Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Swiss National Science Foundation','SNSF','Project funding; Project funding (special)','Swiss National Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Tara Expeditions Foundation','Tara','','Tara Expeditions Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Türkiye Bilimsel ve Teknolojik Araştırma Kurumu','Tubitak','1001-Araştırma','Türkiye Bilimsel ve Teknolojik Araştırma Kurumu','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Türkiye Bilimsel ve Teknolojik Araştırma Kurumu','Tubitak','Uluslararasi','Türkiye Bilimsel ve Teknolojik Araştırma Kurumu','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Canadian Institutes of Health Research','CIHR','','Canadian Institutes of Health Research','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Natural Sciences and Engineering Research Council of Canada','NSERC','','Natural Sciences and Engineering Research Council of Canada','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Social Sciences and Humanities Research Council','SSHRC','','Social Sciences and Humanities Research Council','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Wellcome Trust','WT','Cell and Developmental Biology','Wellcome Trust','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Wellcome Trust','WT','Celluar and Molecular Neuroscience','Wellcome Trust','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Wellcome Trust','WT','Cognitive Neuroscience and Mental Health','Wellcome Trust','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Wellcome Trust','WT','Genetic & Molecular Sciences','Wellcome Trust','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Wellcome Trust','WT','Genetics, Genomics and Population Research','Wellcome Trust','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Wellcome Trust','WT','Immune System in Health and Disease','Wellcome Trust','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Wellcome Trust','WT','Immunology and Infectious Disease','Wellcome Trust','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Wellcome Trust','WT','Infection and Immuno-Biology','Wellcome Trust','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Wellcome Trust','WT','Innovations','Wellcome Trust','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Wellcome Trust','WT','Molecular Basis of Cell Function','Wellcome Trust','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Wellcome Trust','WT','Molecules, Genes and Cells','Wellcome Trust','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Wellcome Trust','WT','Neuroscience and Mental Health','Wellcome Trust','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Wellcome Trust','WT','Neuroscience and Mental Health, Molecular and Cellular Neurosciences, Cognitive and Higher Systems','Wellcome Trust','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Wellcome Trust','WT','Pathogen Biology and Disease Transmission','Wellcome Trust','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Wellcome Trust','WT','Population Health','Wellcome Trust','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Wellcome Trust','WT','Population and Public Health','Wellcome Trust','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Wellcome Trust','WT','Technology Transfer Division','Wellcome Trust','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Croatian Science Foundation (CSF)','HRZZ','','Croatian Science Foundation (CSF)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Ministry of Science, Education and Sports of the Republic of Croatia (MSES)','MZOS','','Ministry of Science, Education and Sports of the Republic of Croatia (MSES)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','OPUS','Narodowe Centrum Nauki','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','PRELUDIUM','Narodowe Centrum Nauki','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','PRELUDIUM BIS','Narodowe Centrum Nauki','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','SONATINA','Narodowe Centrum Nauki','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','SONATA','Narodowe Centrum Nauki','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','SONATA BIS','Narodowe Centrum Nauki','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','HARMONIA','Narodowe Centrum Nauki','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','MAESTRO','Narodowe Centrum Nauki','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','SYMFONIA','Narodowe Centrum Nauki','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','MINIATURA','Narodowe Centrum Nauki','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','ETIUDA','Narodowe Centrum Nauki','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','FUGA','Narodowe Centrum Nauki','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','UWERTURA','Narodowe Centrum Nauki','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','MINIATURA','Narodowe Centrum Nauki','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','OPUS','National Science Centre (Poland)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','PRELUDIUM','National Science Centre (Poland)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','PRELUDIUM BIS','National Science Centre (Poland)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','SONATINA','National Science Centre (Poland)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','SONATA','National Science Centre (Poland)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','SONATA BIS','National Science Centre (Poland)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','HARMONIA','National Science Centre (Poland)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','MAESTRO','National Science Centre (Poland)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','SYMFONIA','National Science Centre (Poland)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','MINIATURA','National Science Centre (Poland)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','ETIUDA','National Science Centre (Poland)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','FUGA','National Science Centre (Poland)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','UWERTURA','National Science Centre (Poland)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Science Centre (Poland)','NCN','MINIATURA','National Science Centre (Poland)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Centre for Research and Development (Poland)','NCBR','BIOSTRATEG','Narodowe Centrum Badań i Rozwoju','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('National Centre for Research and Development (Poland)','NCBR','BIOSTRATEG','National Centre for Research and Development (Poland)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Ministry of Science and Higher Education (Poland)','MNiSW','Narodowy Program Rozwoju Humanistyki','Ministerstwo Nauki i Szkolnictwa Wyższego','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Ministry of Science and Higher Education (Poland)','MNiSW','Narodowy Program Rozwoju Humanistyki','Ministry of Science and Higher Education (Poland)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Ministry of Science and Higher Education (Poland)','MNiSW','Diamentowy Grant','Ministerstwo Nauki i Szkolnictwa Wyższego','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Ministry of Science and Higher Education (Poland)','MNiSW','Diamentowy Grant','Ministry of Science and Higher Education (Poland)','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','TEAM-NET','Fundacja na rzecz Nauki Polskiej','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','TEAM','Fundacja na rzecz Nauki Polskiej','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','TEAM-TECH','Fundacja na rzecz Nauki Polskiej','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','TEAM-TECH Core Facility i Core Facility Plus','Fundacja na rzecz Nauki Polskiej','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','FIRST TEAM','Fundacja na rzecz Nauki Polskiej','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','HOMING','Fundacja na rzecz Nauki Polskiej','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','POWROTY','Fundacja na rzecz Nauki Polskiej','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','MISTRZ','Fundacja na rzecz Nauki Polskiej','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','START','Fundacja na rzecz Nauki Polskiej','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','WYJAZDOWE STYPENDIA NAUKOWE dla laureatów programu MISTRZ','Fundacja na rzecz Nauki Polskiej','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','Międzynarodowe Agendy Badawcze','Fundacja na rzecz Nauki Polskiej','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','Międzynarodowe Agendy Badawcze PLUS','Fundacja na rzecz Nauki Polskiej','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','IDEE DLA POLSKI','Fundacja na rzecz Nauki Polskiej','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','Polskie Honorowe Stypendium Naukowe im. Aleksandra von Humboldta','Fundacja na rzecz Nauki Polskiej','pl');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','TEAM-NET','Polish Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','TEAM','Polish Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','TEAM-TECH','Polish Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','TEAM-TECH Core Facility i Core Facility Plus','Polish Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','FIRST TEAM','Polish Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','HOMING','Polish Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','POWROTY','Polish Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','MISTRZ','Polish Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','START','Polish Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','WYJAZDOWE STYPENDIA NAUKOWE dla laureatów programu MISTRZ','Polish Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','Międzynarodowe Agendy Badawcze','Polish Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','Międzynarodowe Agendy Badawcze PLUS','Polish Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','IDEE DLA POLSKI','Polish Science Foundation','en');
INSERT INTO grantSuggestion(grantAgency, grantAgencyAcronym, fundingProgram, suggestionName, suggestionNameLocale) VALUES ('Polish Science Foundation','FNP','Polskie Honorowe Stypendium Naukowe im. Aleksandra von Humboldta','Polish Science Foundation','en');




