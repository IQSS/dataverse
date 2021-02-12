-- update parents of compound fields where a subfield is required

UPDATE datasetfieldtype SET required=true WHERE id IN 
(SELECT parentdatasetfieldtype_id FROM datasetfieldtype WHERE required=true);


-- add input level rows for parents of compound fields  where a subfield has required input level

INSERT INTO DataverseFieldTypeInputLevel (include,required,datasetfieldtype_id,dataverse_id)
SELECT true, true, parentdatasetfieldtype_id, dataverse_id FROM
DataverseFieldTypeInputLevel dsftil, datasetfieldtype dsft
WHERE dsftil.datasetfieldtype_id = dsft.id
AND parentdatasetfieldtype_id IS NOT null;
