-- A script for creating a numeric identifier sequence, and an external 
-- stored procedure, for accessing the sequence from inside the application, 
-- in a non-hacky, JPA way. 

-- NOTE:

-- 1. The database user name "dvnapp" is hard-coded here - it may
-- need to be changed to match your database user name;
  
-- 2. In the code below, the sequence starts with 1, but it can be adjusted by
-- changing the MINVALUE as needed. 

CREATE SEQUENCE datasetidentifier_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
CACHE 1;

ALTER TABLE datasetidentifier_seq OWNER TO "dvnapp";

-- And now create a PostgreSQL FUNCTION, for JPA to
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
