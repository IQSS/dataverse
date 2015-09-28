UPDATE dvobject 
SET publicationdate = x.releasetime 
FROM (SELECT f.id, f.filesystemname, min(v.releasetime) as releasetime
FROM datafile f, dvobject d, datasetversion v, filemetadata m
WHERE f.id = d.id
AND d.publicationdate IS null
AND m.datafile_id = f.id 
AND m.datasetversion_id = v.id 
AND v.versionstate = 'RELEASED'
-- AND (NOT f.filesystemname IS null AND NOT f.filesystemname LIKE 'http%')
GROUP BY f.id, f.filesystemname) x WHERE x.id = dvobject.id;

