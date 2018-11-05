ALTER TABLE datasetversion ADD COLUMN archivalcopylocation text;
UPDATE dataverserole SET permissionbits=16383 WHERE id=1;

