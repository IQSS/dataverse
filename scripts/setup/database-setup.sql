-- Database setup for Dataverse 4.
-- Names here have to be synchronized with the names in the asadmin-setup.sh script.
-- run using psql database-setup.sql (this script).

-- create user
CREATE USER dvnapp WITH PASSWORD 'dvnAppPass';

-- create database
CREATE DATABASE dvndb WITH OWNER dvnApp ENCODING 'UTF8';

-- grant everything (NOT what you want to do in production.)
GRANT ALL ON DATABASE dvndb TO dvnApp;