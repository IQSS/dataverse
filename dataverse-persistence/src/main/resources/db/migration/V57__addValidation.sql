ALTER TABLE datasetfieldtype ADD COLUMN validation TEXT DEFAULT '[]' NOT NULL;

UPDATE datasetfieldtype SET validation='[{"name":"standard_input"}]' WHERE fieldtype='TEXT';
UPDATE datasetfieldtype SET validation='[{"name":"standard_number"}]' WHERE fieldtype='FLOAT';
UPDATE datasetfieldtype SET validation='[{"name":"standard_int"}]' WHERE fieldtype='INT';
UPDATE datasetfieldtype SET validation='[{"name":"standard_date"}]' WHERE fieldtype='DATE';
UPDATE datasetfieldtype SET validation='[{"name":"standard_url"}]' WHERE fieldtype='URL';
UPDATE datasetfieldtype SET validation='[{"name":"standard_email"}]' WHERE fieldtype='EMAIL';
UPDATE datasetfieldtype SET validation='[{"name":"standard_input"}]' WHERE validation='[]';

UPDATE datasetfieldtype
SET validation='[{"name":"standard_input","parameters":["format:https://ror.org/0[a-hjkmnp-z0-9]{6}[0-9]{2}"]}]'
WHERE name='authorAffiliationIdentifier';

UPDATE datasetfieldtype
SET validation='[{"name":"standard_input","parameters":["format:https://ror.org/0[a-hjkmnp-z0-9]{6}[0-9]{2}"]}]'
WHERE name='grantNumberAgencyIdentifier';