-- This list of characters also appears in DatasetFieldValidator.java
-- Remove character: form feed (\f)
UPDATE datasetfieldvalue SET value = regexp_replace(value, E'\f', '', 'g');
-- Remove character: start of text (\u0002)
UPDATE datasetfieldvalue SET value = regexp_replace(value, U&'\0002', '', 'g');
