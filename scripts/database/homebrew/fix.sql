CREATE SEQUENCE identifier_id_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 10000
  CACHE 1;
ALTER TABLE identifier_id_seq OWNER TO "dvnApp";

UPDATE dataverse SET authority = '10.5072/FK2', protocol = 'doi';
UPDATE dataverse SET doiprovider = 'EZID';
UPDATE dataverse SET doishouldercharacter = '/';
UPDATE dataset SET doishouldercharacter = '/';
