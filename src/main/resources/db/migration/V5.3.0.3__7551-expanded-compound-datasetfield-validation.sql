-- update required attribute of parents of compound fields where a subfield is required
UPDATE datasetfieldtype SET required=true 
WHERE id IN 
(SELECT parentdatasetfieldtype_id FROM datasetfieldtype WHERE required=true);


-- add input level rows for parents of compound fields  where a subfield has required input level
-- (first run a delete to clear out any possible existing parent values)
DELETE FROM dataversefieldtypeinputlevel WHERE datasetfieldtype_id in (
SELECT parentdatasetfieldtype_id FROM datasetfieldtype dsft);
  
INSERT INTO dataversefieldtypeinputlevel (include,required,datasetfieldtype_id,dataverse_id)
SELECT DISTINCT true, true, parentdatasetfieldtype_id, dataverse_id FROM
dataversefieldtypeinputlevel dsftil, datasetfieldtype dsft
WHERE dsftil.datasetfieldtype_id = dsft.id
AND parentdatasetfieldtype_id IS NOT null;
