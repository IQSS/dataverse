-- universe is dropped since it is empty in the dataverse
-- this column will be moved to variablemetadata table
-- issue 5513
ALTER TABLE datavariable
DROP COLUMN  if exists universe;
