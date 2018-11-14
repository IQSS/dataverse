#!/bin/sh

if [[ $1"x" = "x" || $2"x" = "x" || $3"x" = "x" || $4"x" = "x" || $5"x" = "x" || $6"x" = "x" ]]
then
    echo >&2 "usage: ./upgrade.sh [VERSION_1] [VERSION_2] [PG_HOST] [PG_PORT] [PG_DB] [PG_USER]"
    exit 1
fi

upgrade_from=$1
upgrade_to=$2

pg_host=$3
pg_port=$4
pg_database=$5
pg_user=$6

echo "IMPORTANT!"
echo "Make sure you BACK UP YOUR DATABASE before attempting this upgrade!"
echo 
echo "Hit RETURN to continue; or CTRL-C to exit"

read line

if [ ! -f releases.txt ]
then
    echo >&2 "Cannot locate the file \"releases.txt\" in the current directory!"
    echo >&2 "Are you running the script in the correct directory?"
    exit 1
fi


if ! grep -q '^'${upgrade_from}'$' releases.txt 
then
    echo >&2 "${upgrade_from} is not a valid Dataverse release"
    exit 1
fi

if ! grep -q '^'${upgrade_to}'$' releases.txt 
then
    echo >&2 "${upgrade_to} is not a valid Dataverse release"
    exit 1
fi

command -v psql >/dev/null 2>&1 || { 
    echo >&2 'Cannot locate psql (PostgresQL command line utility)!';
    echo >&2 'Make sure it is in your $PATH.';
    echo >&2 'And if you have multiple versions of PostgresQL installed on your system,'; 
    echo >&2 'make sure the psql in your $PATH is the same version your Dataverse is using.';
    echo >&2 'Aborting...'; 
    exit 1; 
}

echo "Enter the password for your PostgresQL database:"
echo "(hit RETURN if you can access the database without a password)"
read PGPASSWORD; export PGPASSWORD

echo 
echo "OK, let's verify that the PostgresQL credentials you provided are valid:"

if dv_count=`psql -w -d ${pg_database} -U ${pg_user} -h ${pg_host} -p ${pg_port} -t -c "SELECT COUNT(*) FROM dataverse"`
then
    echo ok
else 
    echo >&2
    echo >&2 "Failed to connect to the PostgresQL database!"
    echo >&2 "Please verify your access credentials, and try again."
    exit 1
fi

echo 
echo "This script will attempt to upgrade the database ${pg_database},"
echo "that currently has "`/bin/echo -n ${dv_count}`" dataverses, from version ${upgrade_from}"
echo "to version ${upgrade_to}."
echo "Hit RETURN to continue; or CTRL-C to exit"

read line

upgrade_flag="off"

cat releases.txt | while read version
do

    if [ $upgrade_flag = "on" ]
    then
	# database create script: 
	# (this will create the new tables, that were not present in the previous versions)

	if [ ! -f create/create_${version}.sql ]
	then
	    echo >&2 "Cannot locate the create database script create/create_${version}.sql !"
	    exit 1
	fi

	echo "Attempting to run the script create/create_${version}.sql..."

	if ! psql -w d ${pg_database} -U ${pg_user} -h ${pg_host} -p ${pg_port} -f create/create_${version}.sql >/dev/null 2>&1
	then
	    echo >&2 "Failed to run the create database script for version ${version}"
	    exit 1
	fi

	echo "ok"

	# database ugprade script (if present): 
	# (this will modify the tables that WERE present in the previous versions, 
	# that were changed in this version (for example, if columns were added and/or 
	# removed, if the data type of a certain column has changed, etc.)

	if [ -f upgrades/upgrade_v*_to_${version}.sql ]
	then
	    echo "Attempting to execute the upgrade script:" upgrades/upgrade_v*_to_${version}.sql

	    if ! psql -w -d ${pg_database} -U ${pg_user} -h ${pg_host} -p ${pg_port} -f upgrades/upgrade_v*_to_${version}.sql >/dev/null 2>&1
	    then
		echo >&2 "Failed to run the upgrade database script for version ${version}"
		exit 1
	    fi

	    echo "ok"
	else
	    echo "(there is no database upgrade script for version ${version}...)"
	fi

    fi

    if [ $version = $upgrade_from ]
    then
	upgrade_flag="on"
    fi

    if [ $version = $upgrade_to ]
    then
	echo "OK, DONE."
	echo "Your database has been upgraded to version ${version}"
	exit 0
    fi

done




