#!/bin/sh
psql -U postgres -c "CREATE ROLE dvnapp PASSWORD 'secret' SUPERUSER CREATEDB CREATEROLE INHERIT LOGIN" template1
psql -U dvnapp -c 'CREATE DATABASE "dvndb" WITH OWNER = "dvnapp"' template1
