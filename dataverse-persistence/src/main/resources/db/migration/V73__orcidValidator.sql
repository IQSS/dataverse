UPDATE datasetfieldtype
    SET validation = '[{"name":"orcid_validator","parameters":{"context":["DATASET"], "authorIdentifierScheme": "ORCID"}}]'
    WHERE name = 'authorIdentifier';
