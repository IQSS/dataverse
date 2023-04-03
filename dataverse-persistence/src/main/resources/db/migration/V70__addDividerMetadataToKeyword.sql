UPDATE datasetfieldtype
    SET metadata = '{"divider":{"source":"keywordValue", "copy":["keywordVocabulary", "keywordVocabularyURI"]}}'
    WHERE name = 'keyword';