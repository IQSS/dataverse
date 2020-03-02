
ALTER TABLE datasetfield ADD COLUMN datasetfieldparent_id BIGINT;
ALTER TABLE datasetfield ADD COLUMN displayorder INT NOT NULL DEFAULT 0;

ALTER TABLE datasetfield ADD COLUMN tmp_createdfromcompoundvalue_id BIGINT;

INSERT INTO datasetfield(datasetfieldtype_id, datasetversion_id, template_id, displayorder, datasetfieldparent_id, tmp_createdfromcompoundvalue_id)
SELECT parentDsf.datasetfieldtype_id, parentDsf.datasetversion_id, parentDsf.template_id, comp.displayorder, NULL, comp.id
FROM datasetfieldcompoundvalue comp INNER JOIN datasetfield parentDsf ON comp.parentdatasetfield_id = parentDsf.id;

ALTER TABLE datasetfieldcompoundvalue DROP CONSTRAINT fk_datasetfieldcompoundvalue_parentdatasetfield_id;

UPDATE datasetfield field
SET datasetfieldparent_id = parentField.id
FROM datasetfield parentField WHERE field.parentdatasetfieldcompoundvalue_id = parentField.tmp_createdfromcompoundvalue_id;

DELETE FROM datasetfield parentDsf
WHERE EXISTS(SELECT * FROM datasetfieldcompoundvalue comp WHERE comp.parentdatasetfield_id = parentDsf.id);

ALTER TABLE datasetfield DROP COLUMN parentdatasetfieldcompoundvalue_id;
ALTER TABLE datasetfield DROP COLUMN tmp_createdfromcompoundvalue_id;

DROP TABLE datasetfieldcompoundvalue;


ALTER TABLE datasetfield ADD COLUMN fieldvalue TEXT;

INSERT INTO datasetfield(datasetfieldtype_id, datasetversion_id, template_id, fieldvalue, displayorder, datasetfieldparent_id)
SELECT parentDsf.datasetfieldtype_id, parentDsf.datasetversion_id, parentDsf.template_id, dsfv.value, dsfv.displayorder, parentDsf.datasetfieldparent_id
FROM datasetfieldvalue dsfv INNER JOIN datasetfield parentDsf ON dsfv.datasetfield_id = parentDsf.id;

ALTER TABLE datasetfieldvalue DROP CONSTRAINT fk_datasetfieldvalue_datasetfield_id;

DELETE FROM datasetfield parentDsf
WHERE EXISTS(SELECT * FROM datasetfieldvalue dsfv WHERE dsfv.datasetfield_id = parentDsf.id);

DROP TABLE datasetfieldvalue;


ALTER TABLE datasetfield ADD CONSTRAINT
    fk_datasetfield_datasetfieldparent_id
    FOREIGN KEY (datasetfieldparent_id)
    REFERENCES datasetfield (id);

