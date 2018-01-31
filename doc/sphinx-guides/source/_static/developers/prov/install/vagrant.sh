#!/bin/sh
echo "Installing Prov system (dev mode for Dataverse developers)..."

echo "Building CPL library..."
sudo apt-get update
sudo apt-get install -y make g++ libboost-dev unixodbc-dev
cd /prov-cpl
wget --quiet https://github.com/nlohmann/json/releases/download/v3.0.1/json.hpp
mv json.hpp include
sudo make install

echo "Build dependencies for Python REST service..."
cd /prov-cpl/bindings/python
sudo apt-get install -y swig python-dev
# 1 GB was too little memory for `make release`.
make release
sudo make install

echo "Setup REST service to CPL..."
echo "Configure ODBC (direct access to PostgreSQL is not supported)..."
sudo apt-get install -y odbc-postgresql
sudo cat << ODBCINST_CONTENT > /etc/odbcinst.ini
[PostgreSQL]
Description = PostgreSQL ODBC driver (Unicode version)
Driver = psqlodbcw.so
ODBCINST_CONTENT
cat /etc/odbcinst.ini
sudo cat << ODBC_CONTENT > /etc/odbc.ini
[CPL]
Description     = PostgreSQL Core Provenance Library
Driver          = PostgreSQL
Server          = localhost
Database        = cpl
Port            =
Socket          =
Option          =
Stmt            =
User            = cpl
Password        = cplcplcpl
ODBC_CONTENT
cat /etc/odbc.ini

echo "Install and configure PostgreSQL..."
sudo apt-get install -y postgresql
cp /etc/postgresql/9.5/main/pg_hba.conf /prov-cpl
#FIXME: delete this
#local   all             postgres                                peer
sudo cat << PG_HBA_CONTENT > /etc/postgresql/9.5/main/pg_hba.conf
# Dev only! don't set to trust on a production system!
local   all             all                                     trust
host    all             all             127.0.0.1/32            trust
host    all             all             ::1/128                 trust
PG_HBA_CONTENT
sudo cat /etc/postgresql/9.5/main/pg_hba.conf
sudo /etc/init.d/postgresql start

echo "Create database..."
sudo psql -U postgres postgres < /prov-cpl/scripts/postgresql-setup-default.sql

echo "Install dependencies for CPL REST service..."
sudo apt-get install -y python-pip
sudo pip install flask
cd /prov-cpl/bindings/python/RestAPI
REST_SERVICE_USER=postgres # FIXME: create a "cplrest" user?
su $REST_SERVICE_USER -s /bin/sh -c "python cpl-rest.py"
