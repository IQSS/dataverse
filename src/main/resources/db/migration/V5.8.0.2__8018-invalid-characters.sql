DO $$
BEGIN
-- Run on UTF8 only. It's known to fail on SQL_ASCII encoding.
IF EXISTS (SELECT 1 FROM information_schema.character_sets WHERE character_set_name='UTF8') THEN
  -- This list of characters also appears in DatasetFieldValidator.java
  -- Remove character: form feed (\f)
  UPDATE datasetfieldvalue SET value = regexp_replace(value, E'\f', '', 'g');
  -- Remove character: start of text (\u0002)
  UPDATE datasetfieldvalue SET value = regexp_replace(value, U&'\0002', '', 'g');
  -- Remove character: not a character (\ufffe)
  UPDATE datasetfieldvalue SET value = regexp_replace(value, U&'\FFFE', '', 'g');
END IF;
END
$$
