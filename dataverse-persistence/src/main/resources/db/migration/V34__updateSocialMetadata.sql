
DELETE FROM datasetfieldtype WHERE name='targetSampleSizeFormula';

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedsearchfieldtype,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('socialScienceTopicClassification', 'Topic Classification for Social Science', 'Topic classification for the deposited social science data. Broader topic terms (in capital letters) should only be used if no narrower terms are applicable.', '', 'TEXT', 0, '', 
        TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, 
        NULL, 'VOCABULARY_SELECT', '{}', 3, NULL);

UPDATE datasetfieldtype SET fieldType='TEXT', inputRendererType='VOCABULARY_SELECT',
        displayOrder=1,
        allowControlledVocabulary=TRUE, displayoncreate=TRUE,
        description='Basic unit of analysis or observation that this Dataset describes.'
WHERE name='unitOfAnalysis';
DELETE FROM datasetfield df WHERE df.datasetfieldtype_id=90;

UPDATE datasetfieldtype SET displayOrder=2, facetable=FALSE, displayoncreate=TRUE WHERE name='universe';

UPDATE datasetfieldtype SET inputRendererType='VOCABULARY_SELECT',
        displayOrder=3,
        allowControlledVocabulary=TRUE, allowmultiples=TRUE, displayoncreate=TRUE,
        description='The time method or time dimension of the data collection.'
WHERE name='timeMethod';
DELETE FROM datasetfield df WHERE df.datasetfieldtype_id=92;

UPDATE datasetfieldtype SET displayOrder=4, advancedsearchfieldtype=TRUE, facetable=TRUE, displayoncreate=TRUE WHERE name='dataCollector';
UPDATE datasetfieldtype SET displayOrder=5, fieldType='TEXTBOX', inputRendererType='TEXTBOX' WHERE name='collectorTraining';

UPDATE datasetfieldtype SET displayOrder=6,
        facetable=FALSE, displayoncreate=TRUE,
        description='If the data collected includes more than one point in time, indicate the frequency with which the data was collected; that is, monthly, quarterly, or other.'
WHERE name='frequencyOfDataCollection';

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedsearchfieldtype,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('dataSourceType', 'Data Source Type', 'Data source type for this dataset. ', '', 'TEXT', 7, '', 
        TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, 
        NULL, 'VOCABULARY_SELECT', '{}', 3, NULL);

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedsearchfieldtype,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('samplingDescription', 'Sampling Procedure Description', 'Detailed description of the sample and sample design used to select the survey respondents to represent the population. May include reference to the target sample size and the sampling fraction.', '', 'TEXTBOX', 8, '', 
        FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, 
        NULL, 'TEXTBOX', '{}', 3, NULL);

UPDATE datasetfieldtype SET displayOrder=9,
        fieldType='TEXT', inputRendererType='VOCABULARY_SELECT',
        advancedsearchfieldtype=TRUE, allowControlledVocabulary=TRUE, allowmultiples=TRUE, facetable=TRUE, displayoncreate=TRUE,
        title='Sampling Procedure Type',
        description='Type of sampling procedure used to select the survey respondents to represent the population.'
WHERE name='samplingProcedure';
DELETE FROM datasetfield df WHERE df.datasetfieldtype_id=96;

UPDATE datasetfieldtype SET displayOrder=10,
        displayFormat='#NEWLINE',
        displayoncreate=TRUE,
        title='Sample Size',
        description='Specific information regarding the target sample size and achieved sample size.'
WHERE name='targetSampleSize';


UPDATE datasetfieldtype SET displayOrder=11,
        displayFormat='#NAME: #VALUE',
        advancedsearchfieldtype=TRUE, displayoncreate=TRUE,
        title='Targeted',
        description='Target sample size.'
WHERE name='targetSampleActualSize';

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedsearchfieldtype,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('targetSampleSizeAchieved', 'Achieved', 'Achieved sample size. ', 'Enter an integer...', 'INT', 12, '#NAME: #VALUE', 
        TRUE, FALSE, FALSE, FALSE, TRUE, FALSE, 
        97, 'TEXT', '{}', 3, NULL);

UPDATE datasetfieldtype SET displayOrder=13,
        fieldType='TEXTBOX', inputRendererType='TEXTBOX',
        title='Sample Deviations',
        description='Show discrepancies between the sample and available statistics for the population (age, sex-ratio, marital status, etc.) as a whole.'
WHERE name='deviationsFromSampleDesign';

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedsearchfieldtype,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('substitutionOfRespondents', 'Substitution of Respondents', 'Information regarding the substitution of respondents.', '', 'NONE', 14, '#NEWLINE', 
        FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, 
        NULL, 'TEXT', '{}', 3, NULL);

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedsearchfieldtype,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
(SELECT 'substitutionOfRespondentsAcceptability', 'Acceptability', 'Acceptability of substitution of respondents.', '', 'TEXT', 15, '#NAME: #VALUE', 
        TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, 
        parent_dsft.id, 'VOCABULARY_SELECT', '{}', 3, NULL
        FROM datasetfieldtype parent_dsft WHERE parent_dsft.name='substitutionOfRespondents');

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedsearchfieldtype,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
(SELECT 'substitutionOfRespondentsDescription', 'Description', 'A detailed description of the substitution of respondents, especially when using reserved sample. ', '', 'TEXTBOX', 16, '#NAME: #VALUE', 
        FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, 
        parent_dsft.id, 'TEXTBOX', '{}', 3, NULL
        FROM datasetfieldtype parent_dsft WHERE parent_dsft.name='substitutionOfRespondents');

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedsearchfieldtype,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('ageCutOff', 'Age Cut-Off', 'Information regarding age cut-off in the sample. ', '', 'NONE', 17, '#NEWLINE', 
        FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, 
        NULL, 'TEXT', '{}', 3, NULL);

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedsearchfieldtype,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
(SELECT 'ageCutOffLower', 'Lower', 'Lower age cut-off.', '', 'INT', 18, '#NAME: #VALUE', 
        TRUE, FALSE, FALSE, FALSE, TRUE, FALSE, 
        parent_dsft.id, 'TEXT', '{}', 3, NULL
        FROM datasetfieldtype parent_dsft WHERE parent_dsft.name='ageCutOff');

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedsearchfieldtype,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
(SELECT 'ageCutOffUpper', 'Upper', 'Upper age cut-off. ', '', 'INT', 19, '#NAME: #VALUE', 
        TRUE, FALSE, FALSE, FALSE, TRUE, FALSE, 
        parent_dsft.id, 'TEXT', '{}', 3, NULL
        FROM datasetfieldtype parent_dsft WHERE parent_dsft.name='ageCutOff');

UPDATE datasetfieldtype SET displayOrder=20,
        fieldType='TEXT', inputRendererType='VOCABULARY_SELECT',
        advancedsearchfieldtype=TRUE, allowControlledVocabulary=TRUE, allowmultiples=TRUE, facetable=TRUE, displayoncreate=TRUE,
        description='Method used to collect the data.'
WHERE name='collectionMode';
DELETE FROM datasetfield df WHERE df.datasetfieldtype_id=101;


INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedsearchfieldtype,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('contactAttempts', 'Contact Attempts', 'Information regarding contact attempts requirements. ', '', 'NONE', 21, '#NEWLINE', 
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, 
        NULL, 'TEXT', '{}', 3, NULL);

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedsearchfieldtype,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
(SELECT 'contactAttemptsNumber', 'Required Number of Contact Attempts', 'Required Number of Contact Attempts', 'Enter an integer...', 'INT', 22, '#NAME: #VALUE', 
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, 
        parent_dsft.id, 'TEXT', '{}', 3, NULL
        FROM datasetfieldtype parent_dsft WHERE parent_dsft.name='contactAttempts');

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedsearchfieldtype,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
(SELECT 'contactAttemptsSameDay', 'Contact attempts at different times of day required', 'Indicate if there was a requirement of contact attempts at different times of day. ', '', 'TEXT', 23, '#NAME: #VALUE', 
        FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, 
        parent_dsft.id, 'VOCABULARY_SELECT', '{}', 3, NULL
        FROM datasetfieldtype parent_dsft WHERE parent_dsft.name='contactAttempts');

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedsearchfieldtype,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
(SELECT 'contactAttemptsDifferentDays', 'Contact attempts on different days of week required', 'Indicate if there was a requirement of contact attempts on different days of week. ', '', 'TEXT', 24, '#NAME: #VALUE', 
        FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, 
        parent_dsft.id, 'VOCABULARY_SELECT', '{}', 3, NULL
        FROM datasetfieldtype parent_dsft WHERE parent_dsft.name='contactAttempts');

UPDATE datasetfieldtype SET displayOrder=25,
        fieldType='TEXT', inputRendererType='VOCABULARY_SELECT',
        advancedsearchfieldtype=TRUE, allowControlledVocabulary=TRUE, allowmultiples=TRUE, facetable=TRUE, displayoncreate=TRUE,
        description='Type of used data collection instrument.'
WHERE name='researchInstrument';
DELETE FROM datasetfield df WHERE df.datasetfieldtype_id=102;

UPDATE datasetfieldtype SET displayOrder=26,
        description='Description of the context of the study, that could have an impact on the collected data.'
WHERE name='dataCollectionSituation';

UPDATE datasetfieldtype SET fieldType='TEXT', inputRendererType='VOCABULARY_SELECT',
        displayOrder=27,
        advancedsearchfieldtype=TRUE, allowControlledVocabulary=TRUE, allowmultiples=TRUE, facetable=TRUE, displayoncreate=TRUE,
        title='Weight',
        description='Select type of weight applied to produce accurate statistical results.'
WHERE name='weighting';
DELETE FROM datasetfield df WHERE df.datasetfieldtype_id=106;

UPDATE datasetfieldtype SET displayOrder=28, fieldType='TEXTBOX', inputRendererType='TEXTBOX' WHERE name='actionsToMinimizeLoss';

UPDATE datasetfieldtype SET fieldType='TEXTBOX', inputRendererType='TEXTBOX',
        displayOrder=29,
        description='Methods to facilitate data control performed by the primary investigator or by the data archive.'
WHERE name='controlOperations';

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedsearchfieldtype,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
VALUES ('interviewsBackcheck', 'Interviews'' Back-Check', 'Description of back-check control of completed interviews with respect to the fact and correctness of their fulfillment, eg. by coordinator.', '', 'NONE', 30, '#NEWLINE', 
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, 
        NULL, 'TEXT', '{}', 3, NULL);

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedsearchfieldtype,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
(SELECT 'interviewsBackcheckPercentage', 'Percentage of Interviews', 'Approximate percentage of back-checked interviews.', 'Enter an integer...', 'INT', 31, '#NAME: #VALUE', 
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, 
        parent_dsft.id, 'TEXT', '{}', 3, NULL
        FROM datasetfieldtype parent_dsft WHERE parent_dsft.name='interviewsBackcheck');

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
        advancedsearchfieldtype,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
        parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri) 
(SELECT 'interviewsBackcheckProcedureDescription', 'Back-Check Procedure Description', 'A detailed description of back-check procedure.', '', 'TEXTBOX', 32, '#NAME: #VALUE', 
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, 
        parent_dsft.id, 'TEXTBOX', '{}', 3, NULL
        FROM datasetfieldtype parent_dsft WHERE parent_dsft.name='interviewsBackcheck');

UPDATE datasetfieldtype SET fieldType='TEXTBOX', inputRendererType='TEXTBOX',
        displayOrder=33,
        description='Description of data cleaning and logical control procedures.'
WHERE name='cleaningOperations';

UPDATE datasetfieldtype SET fieldType='TEXTBOX', inputRendererType='TEXTBOX',
        displayOrder=34,
        description='Description of known errors and difficulties that occured during the study and data processing.'
WHERE name='datasetLevelErrorNotes';

UPDATE datasetfieldtype SET fieldType='FLOAT', inputRendererType='TEXT',
        displayOrder=35,
        advancedsearchfieldtype=FALSE, facetable=FALSE,
        description='Response rate according to AAPOR definition.',
        watermark='Enter a floating-point number...'
WHERE name='responseRate';

UPDATE datasetfieldtype SET displayOrder=36, fieldType='TEXTBOX', inputRendererType='TEXTBOX' WHERE name='samplingErrorEstimates';
UPDATE datasetfieldtype SET displayOrder=37, fieldType='TEXTBOX', inputRendererType='TEXTBOX' WHERE name='otherDataAppraisal';
UPDATE datasetfieldtype SET displayOrder=38, displayFormat='#NEWLINE' WHERE name='socialScienceNotes';
UPDATE datasetfieldtype SET displayOrder=39, displayFormat='#NAME: #VALUE' WHERE name='socialScienceNotesType';
UPDATE datasetfieldtype SET displayOrder=40, displayFormat='#NAME: #VALUE' WHERE name='socialScienceNotesSubject';
UPDATE datasetfieldtype SET displayOrder=41 WHERE name='socialScienceNotesText';


INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Individual', 0, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'unitOfAnalysis');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Organization', 1, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'unitOfAnalysis');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Family', 2, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'unitOfAnalysis');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Family.HouseholdFamily', 3, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'unitOfAnalysis');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Household', 4, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'unitOfAnalysis');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'HousingUnit', 5, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'unitOfAnalysis');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'EventOrProcess', 6, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'unitOfAnalysis');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'GeographicUnit', 7, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'unitOfAnalysis');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'TimeUnit', 8, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'unitOfAnalysis');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'TextUnit', 9, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'unitOfAnalysis');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Group', 10, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'unitOfAnalysis');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Object', 11, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'unitOfAnalysis');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Other', 12, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'unitOfAnalysis');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Longitudinal', 0, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'timeMethod');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Longitudinal.CohortEventBased', 1, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'timeMethod');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Longitudinal.TrendRepeatedCrossSection', 2, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'timeMethod');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Longitudinal.Panel', 3, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'timeMethod');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Longitudinal.Panel.Continuous', 4, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'timeMethod');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Longitudinal.Panel.Interval', 5, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'timeMethod');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'TimeSeries', 6, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'timeMethod');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'TimeSeries.Continuous', 7, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'timeMethod');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'TimeSeries.Discrete', 8, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'timeMethod');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'CrossSection', 9, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'timeMethod');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'CrossSectionAdHocFollowUp', 10, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'timeMethod');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Other', 11, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'timeMethod');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'TotalUniverseCompleteEnumeration', 0, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'samplingProcedure');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Probability', 1, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'samplingProcedure');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Probability.SimpleRandom', 2, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'samplingProcedure');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Probability.SystematicRandom', 3, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'samplingProcedure');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Probability.Stratified', 4, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'samplingProcedure');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Probability.Stratified.Proportional', 5, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'samplingProcedure');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Probability.Stratified.Disproportional', 6, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'samplingProcedure');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Probability.Cluster', 7, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'samplingProcedure');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Probability.Cluster.SimpleRandom', 8, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'samplingProcedure');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Probability.Cluster.StratifiedRandom', 9, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'samplingProcedure');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Probability.Multistage', 10, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'samplingProcedure');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Nonprobability', 11, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'samplingProcedure');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Nonprobability.Availability', 12, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'samplingProcedure');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Nonprobability.Purposive', 13, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'samplingProcedure');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Nonprobability.Quota', 14, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'samplingProcedure');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Nonprobability.RespondentAssisted', 15, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'samplingProcedure');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'MixedProbabilityNonprobability', 16, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'samplingProcedure');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Other', 17, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'samplingProcedure');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Allowed', 0, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'substitutionOfRespondentsAcceptability');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Not allowed', 1, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'substitutionOfRespondentsAcceptability');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Interview', 0, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Interview.FaceToFace', 1, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Interview.FaceToFace.CAPIorCAMI', 2, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Interview.FaceToFace.PAPI', 3, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Interview.Telephone', 4, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Interview.Telephone.CATI', 5, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Interview.Email', 6, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Interview.WebBased', 7, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SelfAdministeredQuestionnaire', 8, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SelfAdministeredQuestionnaire.Email', 9, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SelfAdministeredQuestionnaire.Paper', 10, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SelfAdministeredQuestionnaire.SMSorMMS', 11, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SelfAdministeredQuestionnaire.CAWI', 12, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SelfAdministeredQuestionnaire.CASI', 13, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'FocusGroup', 14, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'FocusGroup.FaceToFace', 15, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'FocusGroup.Telephone', 16, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'FocusGroup.Online', 17, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SelfAdministeredWritingsAndDiaries', 18, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SelfAdministeredWritingsAndDiaries.Email', 19, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SelfAdministeredWritingsAndDiaries.Paper', 20, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SelfAdministeredWritingsAndDiaries.WebBased', 21, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Observation', 22, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Observation.Field', 23, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Observation.Field.Participant', 24, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Observation.Field.Nonparticipant', 25, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Observation.Laboratory', 26, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Observation.Laboratory.Participant', 27, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Observation.Laboratory.Nonparticipant', 28, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Observation.ComputerBased', 29, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Experiment', 30, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Experiment.Laboratory', 31, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Experiment.FieldIntervention', 32, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Experiment.WebBased', 33, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Recording', 34, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'ContentCoding', 35, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Transcription', 36, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'CompilationSynthesis', 37, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Summary', 38, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Aggregation', 39, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Simulation', 40, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'MeasurementsAndTests', 41, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'MeasurementsAndTests.Educational', 42, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'MeasurementsAndTests.Physical', 43, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'MeasurementsAndTests.Psychological', 44, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Other', 45, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'collectionMode');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'RegistersRecordsAccounts', 0, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'RegistersRecordsAccounts.Administrative', 1, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'RegistersRecordsAccounts.Historical', 2, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'RegistersRecordsAccounts.Legal', 3, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'RegistersRecordsAccounts.MedicalClinical', 4, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'RegistersRecordsAccounts.AcademicAptitude', 5, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'RegistersRecordsAccounts.EconomicFinancial', 6, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'RegistersRecordsAccounts.Personal', 7, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'RegistersRecordsAccounts.VotingResults', 8, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'EventsInteractions', 9, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Processes', 10, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Processes.Workflows', 11, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Communications', 12, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Communications.Public', 13, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Communications.Interpersonal', 14, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'ResearchData', 15, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'ResearchData.Published', 16, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'ResearchData.Unpublished', 17, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'PopulationGroup', 18, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'GeographicArea', 19, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'PhysicalObjects', 20, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'BiologicalSamples', 21, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Other', 22, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'dataSourceType');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Demography', 0, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Demography.MorbidityAndMortality', 1, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Demography.Censuses', 2, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Demography.Migration', 3, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Economics', 4, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Economics.EconomicConditionsAndIndicators', 5, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Economics.ConsumptionAndConsumerBehaviour', 6, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Economics.EconomicPolicyPublicExpenditureAndRevenue', 7, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Economics.IncomePropertyAndInvestment', 8, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Economics.SystemsAndDevelopment', 9, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Education', 10, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Education.CompulsoryAndPreschool', 11, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Education.HigherAndFurther', 12, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Education.VocationalAndTraining', 13, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Education.LifelongContinuing', 14, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Education.EducationalPolicy', 15, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Health', 16, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Health.HealthCareServicesAndPolicies', 17, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Health.SpecificDiseasesDisordersAndMedicalConditions', 18, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Health.SignsAndSymptomsPathologicalConditions', 19, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Health.WoundsAndInjuries', 20, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Health.MedicationAndTreatment', 21, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Health.GeneralHealthAndWellbeing', 22, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Health.Public', 23, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Health.Reproductive', 24, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Health.Occupational', 25, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Health.HealthBehaviour', 26, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'History', 27, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'HousingAndLandUse', 28, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'HousingAndLandUse.Housing', 29, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'HousingAndLandUse.LandUseAndPlanning', 30, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'LabourAndEmployment', 31, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'LabourAndEmployment.Employment', 32, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'LabourAndEmployment.Unemployment', 33, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'LabourAndEmployment.Retirement', 34, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'LabourAndEmployment.EmployeeTraining', 35, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'LabourAndEmployment.LabourRelationsConflict', 36, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'LabourAndEmployment.WorkingConditions', 37, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'LabourAndEmployment.LabourAndEmploymentPolicy', 38, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'LawCrimeAndLegalSystems', 39, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'LawCrimeAndLegalSystems.LegislationAndLegalSystems', 40, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'LawCrimeAndLegalSystems.CrimeAndLawEnforcement', 41, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'MediaCommunicationAndLanguage', 42, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'MediaCommunicationAndLanguage.Media', 43, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'MediaCommunicationAndLanguage.PublicRelations', 44, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'MediaCommunicationAndLanguage.InformationSociety', 45, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'MediaCommunicationAndLanguage.LanguageAndLinguistics', 46, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'NaturalEnvironment', 47, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'NaturalEnvironment.EnvironmentAndConservation', 48, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'NaturalEnvironment.EnergyAndNaturalResources', 49, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'NaturalEnvironment.PlantsAndAnimals', 50, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'NaturalEnvironment.NaturalLandscapes', 51, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Politics', 52, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Politics.PoliticalBehaviourAndAttitudes', 53, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Politics.Elections', 54, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Politics.PoliticalIdeology', 55, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Politics.GovernmentPoliticalSystemsAndOrganisations', 56, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Politics.InternationalPoliticsAndOrganisations', 57, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Politics.ConflictSecurityAndPeace', 58, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Psychology', 59, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'ScienceAndTechnology', 60, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'ScienceAndTechnology.InformationTechnology', 61, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'ScienceAndTechnology.Biotechnology', 62, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocialStratificationAndGroupings', 63, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocialStratificationAndGroupings.EqualityAndInequality', 64, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocialStratificationAndGroupings.SocialAndOccupationalMobility', 65, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocialStratificationAndGroupings.GenderAndGenderRoles', 66, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocialStratificationAndGroupings.FamilyLifeAndMarriage', 67, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocialStratificationAndGroupings.Youth', 68, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocialStratificationAndGroupings.Elderly', 69, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocialStratificationAndGroupings.Children', 70, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocialStratificationAndGroupings.Minorities', 71, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocialStratificationAndGroupings.ElitesAndLeadership', 72, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocialWelfarePolicyAndSystems', 73, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocialWelfarePolicyAndSystems.SocialWelfarePolicy', 74, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocialWelfarePolicyAndSystems.SocialWelfareSystemsStructures', 75, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocialWelfarePolicyAndSystems.SpecificSocialServices', 76, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocietyAndCulture', 77, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocietyAndCulture.SocialBehaviourAndAttitudes', 78, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocietyAndCulture.SocialConditionsAndIndicators', 79, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocietyAndCulture.CulturalActivitiesAndParticipation', 80, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocietyAndCulture.CulturalAndNationalIdentity', 81, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocietyAndCulture.LeisureTourismSport', 82, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocietyAndCulture.ReligionAndValues', 83, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocietyAndCulture.CommunityUrbanAndRuralLife', 84, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocietyAndCulture.TimeUse', 85, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'SocietyAndCulture.SocialChange', 86, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'TradeIndustryAndMarkets', 87, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'TradeIndustryAndMarkets.BusinessIndustrialManagementAndOrganisation', 88, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'TradeIndustryAndMarkets.AgricultureAndRuralIndustry', 89, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'TradeIndustryAndMarkets.ForeignTrade', 90, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'TransportAndTravel', 91, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Other', 92, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'socialScienceTopicClassification');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Questionnaire', 0, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'researchInstrument');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Questionnaire.Structured', 1, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'researchInstrument');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Questionnaire.Semistructured', 2, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'researchInstrument');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Questionnaire.Unstructured', 3, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'researchInstrument');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'InterviewSchemeAndThemes', 4, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'researchInstrument');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'DataCollectionGuidelines', 5, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'researchInstrument');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'DataCollectionGuidelines.ObservationGuide', 6, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'researchInstrument');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'DataCollectionGuidelines.DiscussionGuide', 7, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'researchInstrument');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'DataCollectionGuidelines.SelfAdministeredWritingsGuide', 8, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'researchInstrument');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'DataCollectionGuidelines.SecondaryDataCollectionGuide', 9, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'researchInstrument');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'ParticipantTasks', 10, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'researchInstrument');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'TechnicalInstruments', 11, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'researchInstrument');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'ProgrammingScript', 12, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'researchInstrument');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Other', 13, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'researchInstrument');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'PostStratification', 0, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'weighting');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Design', 1, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'weighting');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'PopulationSize', 2, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'weighting');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'MixedPostStratificationDesign', 3, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'weighting');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Other', 4, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'weighting');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'None', 5, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'weighting');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Yes', 0, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'contactAttemptsSameDay');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'No', 1, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'contactAttemptsSameDay');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'Yes', 0, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'contactAttemptsDifferentDays');
INSERT INTO controlledvocabularyvalue (strvalue, displayorder, datasetfieldtype_id) (SELECT 'No', 1, dsft.id FROM datasetfieldtype dsft WHERE dsft.name =  'contactAttemptsDifferentDays');


