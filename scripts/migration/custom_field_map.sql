
delete from customfieldmap;

COPY customfieldmap( sourcetemplate, sourcedatasetfield, targetdatasetfield) FROM '<git project path>/scripts/migration/HarvardCustomFields.csv' DELIMITER ',' CSV HEADER;


