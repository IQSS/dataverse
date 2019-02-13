ALTER TABLE datasetversion ADD COLUMN archivalcopylocation text;
ALTER TABLE externaltool ADD COLUMN contenttype text NOT NULL default 'text/tab-separated-values';

-- universe and weighted are dropped since they are empty in the dataverse
-- these columns will be moved to variablemetadata table
-- issue 5513
ALTER TABLE datavariable
DROP COLUMN  if exists universe,
DROP COLUMN  if exists weighted;
