-- In a migration scenario, "START" should be changed from "1" to the highest id value plus 1.
CREATE SEQUENCE datasetid_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
CACHE 1;
-- With the database user "dvnApp" hard-coded here, we are dictating the usename, which is bad, since the installer lets you pick your username.
ALTER TABLE datasetid_seq OWNER TO "dvnApp";
