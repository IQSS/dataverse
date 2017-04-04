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

-- handle absence of CREATE OR REPLACE LANGUAGE for postgresql 8.4 or older
-- courtesy of the postgres wiki: https://wiki.postgresql.org/wiki/CREATE_OR_REPLACE_LANGUAGE
CREATE OR REPLACE FUNCTION make_plpgsql()
RETURNS VOID
LANGUAGE SQL
AS $$
CREATE LANGUAGE plpgsql;
$$;
 
SELECT
    CASE
    WHEN EXISTS(
        SELECT 1
        FROM pg_catalog.pg_language
        WHERE lanname='plpgsql'
    )
    THEN NULL
    ELSE make_plpgsql() END;
 
DROP FUNCTION make_plpgsql();

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
