ALTER TABLE datasetversion ADD COLUMN archivalcopylocation text;
ALTER TABLE externaltool ADD COLUMN contenttype text NOT NULL default 'text/tab-separated-values';

ALTER TABLE datavariable
DROP COLUMN  if exists universe,
DROP COLUMN  if exists weighted;
