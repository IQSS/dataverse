-- A script for creating a numeric identifier sequence, and an external 
-- stored procedure, for accessing the sequence from inside the application, 
-- in a non-hacky, JPA way. 

-- NOTE:

-- 1. The database user name "dvnapp" is hard-coded here - they will (may)
-- need to change it to match their database setup:

-- 2. This sequence starts with 1, but the MINVALUE may be adjusted, according to the 
-- needs of the specific Dataverse installation. 

CREATE SEQUENCE datasetidentifier_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
CACHE 1;

ALTER TABLE datasetidentifier_seq OWNER TO "dvnapp";

-- And now create a PostgresQL FUNCTION, for JPA to 
-- access as a NamedStoredProcedure:

CREATE OR REPLACE FUNCTION generateIdentifierAsSequentialNumber(
    OUT identifier int)
  RETURNS int AS
$BODY$
BEGIN
    select nextval('datasetidentifier_seq') into identifier;
END;
$BODY$
  LANGUAGE plpgsql;
