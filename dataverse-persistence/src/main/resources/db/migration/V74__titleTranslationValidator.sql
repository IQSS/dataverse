UPDATE datasetfieldtype
    SET validation = '[{"name":"required_dependant","parameters":{"context":["DATASET"], "dependantField": "titleTranslationText"}}]'
    WHERE name = 'titleTranslationLanguage';

UPDATE datasetfieldtype
    SET validation = '[{"name":"required_dependant","parameters":{"context":["DATASET"], "dependantField": "titleTranslationLanguage"}}]'
    WHERE name = 'titleTranslationText';
