-- An aggregated timestamp which is the latest of the availability dates of any embargoed files in the first published version, if present 
ALTER TABLE dataset ADD COLUMN IF NOT EXISTS embargoCitationDate timestamp without time zone;
-- ... and an update query that will populate this column for all the published datasets with embargoed files in the first released version:
UPDATE dataset SET embargocitationdate=o.embargocitationdate
FROM (SELECT d.id, MAX(e.dateavailable) AS embargocitationdate
FROM embargo e, dataset d, datafile f, datasetversion v, filemetadata m
WHERE v.dataset_id = d.id
AND v.versionstate = 'RELEASED'
AND v.versionnumber = 1
AND v.minorversionnumber = 0
AND f.embargo_id = e.id
AND m.datasetversion_id = v.id
AND m.datafile_id = f.id GROUP BY d.id) o WHERE o.id = dataset.id;
-- (the query follows the logic that used to be in the method Dataset.getCitationDate() that calculated this adjusted date in real time). 
