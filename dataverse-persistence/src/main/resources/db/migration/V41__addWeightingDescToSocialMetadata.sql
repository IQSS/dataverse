UPDATE datasetfieldtype
    SET displayorder = displayorder + 1
WHERE displayorder > 27 AND metadatablock_id = 3;

UPDATE datasetfieldtype
    SET description = 'Select the type of weight provided with the dataset and available to the user.'
WHERE metadatablock_id = 3 AND name = 'weighting';

INSERT INTO datasetfieldtype(name,title,description,watermark,fieldType,displayOrder,displayFormat,
                             advancedSearchFieldType,allowControlledVocabulary,allowmultiples,facetable,displayoncreate,required,
                             parentDatasetFieldType_id,inputRendererType,inputRendererOptions,metadatablock_id,uri)
VALUES ('weightingDescription', 'Description of the Weight', 'Description of the weight and the way it was constructed: its components, algorithm used for its calculation e.g. IPF, sum to marginal distributions, regression based etc., calibration (normalization) if used.',
        '', 'TEXTBOX', 28, '',
        FALSE, FALSE, FALSE, FALSE, TRUE, FALSE,
        NULL, 'TEXTBOX', '{}', 3, NULL);
