#!/bin/bash
echo "Installing prov system..."
echo "Building CPL library"
sudo yum install -y centos-release-scl
sudo yum install -y git vim-enhanced redhat-lsb-core devtoolset-7 boost-devel unixODBC-devel
git clone https://github.com/ProvTools/prov-cpl.git
cd /home/vagrant/prov-cpl
wget --quiet https://github.com/nlohmann/json/releases/download/v3.0.1/json.hpp
mv json.hpp include
source /opt/rh/devtoolset-7/enable
sudo make install
echo "Building Python bindings for CPL"
echo "Install Swig 3.x (2.x from yum is too old), requires prce-devel"
sudo yum install -y pcre-devel
cd
mkdir swig
cd swig
wget http://prdownloads.sourceforge.net/swig/swig-3.0.12.tar.gz
tar xvfz swig-3.0.12.tar.gz
cd swig-3.0.12
./configure
make
sudo make install
echo "Done building and installing Swig"
cd /home/vagrant/prov-cpl/bindings/python
sudo yum install -y python-devel
make release
sudo make install

echo "Setup REST service to CPL..."
echo "Configure ODBC (direct access to PostgreSQL is not supported)..."
sudo yum install -y postgresql-odbc
# Note that /etc/odbcinst.ini seems fine as-is.
CPL_SERVICE_USER=cplservice
POSTGRES_DATABASE=cpl
POSTGRES_USER_PASSWORD=cplcplcpl
cat << ODBC_CONTENT | sudo tee /etc/odbc.ini
[CPL]
Description     = PostgreSQL Core Provenance Library
Driver          = PostgreSQL
Server          = localhost
Database        = $POSTGRES_DATABASE
Port            =
Socket          =
Option          =
Stmt            =
User            = $CPL_SERVICE_USER
Password        = $POSTGRES_USER_PASSWORD
ODBC_CONTENT
cat /etc/odbc.ini
echo "Install and configure PostgreSQL..."
sudo yum install -y postgresql-server
sudo postgresql-setup initdb
cat << PG_HBA_CONTENT | sudo tee /var/lib/pgsql/data/pg_hba.conf
# Dev only! don't set to trust on a production system!
local   all             all                                     trust
host    all             all             127.0.0.1/32            trust
host    all             all             ::1/128                 trust
PG_HBA_CONTENT
sudo cat /var/lib/pgsql/data/pg_hba.conf
#FIXME: restart Glassfish after restarting Postgres?
sudo systemctl restart postgresql
echo "Create database..."
POSTGRES_USER=$CPL_SERVICE_USER
# FIXME: This line has been a bit problematic...
sudo psql -U postgres postgres -v db_name=$POSTGRES_DATABASE -v user_name=$POSTGRES_USER -v user_password=$POSTGRES_USER_PASSWORD < /home/vagrant/prov-cpl/scripts/postgresql-setup-conf.sql

echo "Create user to run CPL REST service as: $CPL_SERVICE_USER"
sudo useradd $CPL_SERVICE_USER
echo "Install dependencies for CPL REST service..."
sudo yum install -y python-flask
echo "Downloading CPL REST service script..."
sudo bash -c "su -l $CPL_SERVICE_USER -c 'wget https://raw.githubusercontent.com/ProvTools/prov-cpl/8150ee315abc21712b49da2bf4cfdbf308eef1d7/bindings/python/RestAPI/cpl-rest.py'"
echo "Starting CPL REST service..."
sudo bash -c "su -l $CPL_SERVICE_USER -c 'LD_LIBRARY_PATH=/usr/local/lib python cpl-rest.py --host=0.0.0.0 &'"
echo "Checking version of CPL REST service..."
curl http://localhost:5000/provapi/version
echo "Configuring Dataverse to use CPL REST service URL..."
curl -X PUT -d 'http://localhost:5000' http://localhost:8080/api/admin/settings/:ProvServiceUrl
