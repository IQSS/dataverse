
-- citation metadata

UPDATE datasetfieldtype SET displayFormat='' WHERE name='grantNumber';
UPDATE datasetfieldtype SET displayFormat='– #VALUE' WHERE name='grantNumberProgram';
UPDATE datasetfieldtype SET displayFormat='– #VALUE' WHERE name='grantNumberValue';

UPDATE datasetfieldtype SET
    description='The type of relation between the deposited dataset and the related publication.'
WHERE name='publicationRelationType';


UPDATE datasetfieldtype SET
    description='Producer URL points to the producer''s web presence, if appropriate.'
WHERE name='producerURL';

UPDATE datasetfieldtype SET
    description='URL for the producer''s logo, which points to this producer''s web-accessible logo image.'
WHERE name='producerLogoURL';


UPDATE datasetfieldtype SET
    description='Distributor name.'
WHERE name='distributorName';

UPDATE datasetfieldtype SET
    description='Distributor URL points to the distributor''s web presence, if appropriate.'
WHERE name='distributorURL';

UPDATE datasetfieldtype SET
    description='URL of the distributor''s logo, which points to this distributor''s web-accessible logo image.'
WHERE name='distributorLogoURL';


UPDATE datasetfieldtype SET
    description='The type of relation between the deposited and the related dataset.'
WHERE name='relatedDatasetRelationType';

UPDATE datasetfieldtype SET
    description='The type of digital identifier of the related dataset (e.g., Digital Object Identifier (DOI)).'
WHERE name='relatedDatasetIDType';


UPDATE datasetfieldtype SET
    description='The type of relation between the deposited dataset and the related material.'
WHERE name='relatedMaterialRelationType';

UPDATE datasetfieldtype SET
    description='The type of digital identifier of the related material (e.g., Digital Object Identifier (DOI)).'
WHERE name='relatedMaterialIDType';


-- social metadata

UPDATE datasetfieldtype SET inputRendererOptions='{"renderInTwoColumns": false}' WHERE name='contactAttemptsNumber';
UPDATE datasetfieldtype SET inputRendererOptions='{"renderInTwoColumns": false}' WHERE name='contactAttemptsSameDay';
UPDATE datasetfieldtype SET inputRendererOptions='{"renderInTwoColumns": false}' WHERE name='contactAttemptsDifferentDays';

UPDATE datasetfieldtype SET watermark='Enter an integer...' WHERE name='ageCutOffLower';
UPDATE datasetfieldtype SET watermark='Enter an integer...' WHERE name='ageCutOffUpper';

UPDATE datasetfieldtype SET description='Required Number of Contact Attempts.' WHERE name='contactAttemptsNumber';

UPDATE datasetfieldtype SET
    description='Description of back-check control of completed interviews with respect to the fact and correctness of their fulfillment, e.g. by coordinator.'
WHERE name='interviewsBackcheck';

UPDATE datasetfieldtype SET
    description='Description of data cleaning and logical control procedures.'
WHERE name='cleaningOperations';

UPDATE datasetfieldtype SET
    description='Description of known errors and difficulties that occurred during the study and data processing.'
WHERE name='datasetLevelErrorNotes';

