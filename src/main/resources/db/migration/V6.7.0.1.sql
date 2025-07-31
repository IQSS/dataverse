-- remove dataversesubjects entries with a controlledvocabularyvalue_id matching "N/A"
DELETE FROM dataversesubjects WHERE controlledvocabularyvalue_id = 1 ;
