UPDATE datasetfieldtype
    SET validation = '[{"name":"ror_validator","parameters":{"context":["DATASET"]}}]'
    WHERE validation = '[{"name":"ror_validator"}]';
UPDATE datasetfieldtype
    SET validation = '[{"name":"standard_email","parameters":{"context":["DATASET"]}}]'
    WHERE validation = '[{"name":"standard_email"}]';
UPDATE datasetfieldtype
    SET validation = '[{"name":"standard_url","parameters":{"context":["DATASET"]}}]'
    WHERE validation = '[{"name":"standard_url"}]';
UPDATE datasetfieldtype
    SET validation = '[{"name":"standard_date","parameters":{"context":["DATASET"]}},{"name":"date_range","parameters":{"context":["SEARCH"]}}]'
    WHERE validation = '[{"name":"standard_date"}]';
