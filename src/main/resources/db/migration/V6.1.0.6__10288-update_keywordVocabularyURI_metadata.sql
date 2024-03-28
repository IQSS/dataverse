-- update of the "keywordVocabularyURI" metadata to make it consistent with its name "Controlled Vocabulary URL"
UPDATE datasetfieldtype SET name = 'keywordVocabularyURL' 
WHERE name = 'keywordVocabularyURI' 
AND parentdatasetfieldtype_id = (
	SELECT id FROM datasetfieldtype WHERE name = 'keyword') ;