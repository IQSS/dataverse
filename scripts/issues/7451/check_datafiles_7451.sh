#!/bin/bash

# begin config
# PostgresQL credentials:
# edit the following lines so that psql can talk to your database
pg_host=localhost
pg_port=5432
pg_user=dvnapp
pg_db=dvndb
# you can leave the password blank, if Postgres is configured 
# to accept connections without auth:
pg_pass=
# psql executable - add full path, if necessary:
PSQL_EXEC=psql

# end config

# check for duplicate storageidentifiers in harvested datafiles: 

PG_QUERY_0="SELECT COUNT(DISTINCT o.id) FROM datafile f, dataset s, dvobject p, dvobject o WHERE s.id = p.id AND o.id = f.id AND o.owner_id = s.id AND s.harvestingclient_id IS NOT null AND o.storageidentifier IS NOT null"

PG_QUERY_1="SELECT s.id, o.storageidentifier FROM datafile f, dataset s, dvobject o WHERE o.id = f.id AND o.owner_id = s.id AND s.harvestingclient_id IS NOT null AND o.storageidentifier IS NOT null ORDER by s.id, o.storageidentifier"

PG_QUERY_FIX_0="UPDATE dvobject SET storageidentifier=NULL WHERE dtype='DataFile' AND (storageidentifier='file://' OR storageidentifier='http://' OR storageidentifier='s3://')"

PG_QUERY_FIX_1="UPDATE dvobject SET storageidentifier=CONCAT(storageidentifier, ' ', id) WHERE owner_id = %d AND storageidentifier='%s'"

PGPASSWORD=$pg_pass; export PGPASSWORD

echo "Checking the total number of storageidentifiers in harvested datafiles..."

NUM_DATAFILES=`${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c "${PG_QUERY_0}"`
if [ $? != 0 ]
then
    echo "FAILED to execute psql! Check the credentials and try again?"
    echo "exiting..."
    echo 
    echo "the command line that failed:"
    echo "${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c \"${PG_QUERY_0}\""
    exit 1
fi

echo $NUM_DATAFILES total.
echo 

# Before we do anything else, reset the storageidentifiers of the datafiles (harvested and otherwise) that 
# may have ended up set to invalid, prefix-only values like "file://" back to NULL:

${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -q -c "${PG_QUERY_FIX_0}" 


echo "Let's check if any harvested storage identifiers are referenced more than once within the same dataset:" 

${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c "${PG_QUERY_1}" |
uniq -c | 
awk '{if ($1 > 1) print $0}' | sort -u > /tmp/harvestedidentifiers.tmp

NUM_CONFIRMED=`cat /tmp/harvestedidentifiers.tmp | wc -l`

if [ $NUM_CONFIRMED == 0 ]
then
    echo 
    echo "Good news - it appears that there are NO duplicate storageidentifiers in your harvested datafiles;"
    echo "nothing to fix."
    echo 
else

    echo "Found ${NUM_CONFIRMED} harvested files with identical storageidentifiers; fixing in place..."

    cat /tmp/harvestedidentifiers.tmp | sed 's:\\:\\\\:g' | while read howmany dataset storageidentifier
    do
	# Harvard prod. db had a few harvested storage identifiers consisting of a single space (" "),
	# which would confuse the shell. Extremely unlikely to be found in any other installation.
	if [[ "x${storageidentifier}" = "x" ]]
	then
		storageidentifier=" "
	fi

	PG_QUERY_SI=`printf "${PG_QUERY_FIX_1}" $dataset "$storageidentifier"`
	${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c "${PG_QUERY_SI}"
    done 

    echo "... done."
    echo

    echo -n "Let's confirm that all these dupes have been fixed... "
    ${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c "${PG_QUERY_1}" |
	uniq -c | 
	awk '{if ($1 > 1) print $0}' | sort -u > /tmp/harvestedidentifiers.tmp

    NUM_CONFIRMED=`cat /tmp/harvestedidentifiers.tmp | wc -l`

    if [ $NUM_CONFIRMED == 0 ]
    then
	echo "Looks good."
	echo 
    else
	echo "Oops!"
	echo "Unfortunately, the script failed to fix some of the harvested duplicates."
	echo "Please send the contents of the file /tmp/harvestedidentifiers.tmp"
	echo "to Dataverse support at support@dataverse.org."
	echo "Apologies for the extra trouble..."
	echo
	exit 1
    fi    

fi


# now, check for duplicate storageidentifiers in local datafiles: 

PG_QUERY_3="SELECT COUNT(DISTINCT o.id) FROM datafile f, dataset s, dvobject p, dvobject o WHERE s.id = p.id AND o.id = f.id AND o.owner_id = s.id AND s.harvestingclient_id IS null AND o.storageidentifier IS NOT null"

PG_QUERY_4="SELECT s.id, o.storageidentifier FROM datafile f, dataset s, dvobject o WHERE o.id = f.id AND o.owner_id = s.id AND s.harvestingclient_id IS null AND o.storageidentifier IS NOT null ORDER by s.id, o.storageidentifier"

PG_QUERY_5="SELECT p.authority, p.identifier, o.storageidentifier, o.id, o.createdate, f.contenttype FROM datafile f, dvobject p, dvobject o WHERE o.id = f.id AND o.owner_id = p.id AND p.id = %d AND o.storageidentifier='%s' ORDER by o.id"

echo "Checking the number of non-harvested datafiles in the database..."

NUM_DATAFILES=`${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c "${PG_QUERY_3}"`
echo $NUM_DATAFILES total.
echo 

echo "Let's check if any storage identifiers are referenced more than once within the same dataset:" 

${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c "${PG_QUERY_4}" |
uniq -c | 
awk '{if ($1 > 1) print $0}' > /tmp/storageidentifiers.tmp

NUM_CONFIRMED=`cat /tmp/storageidentifiers.tmp | wc -l`

if [ $NUM_CONFIRMED == 0 ]
then
    echo 
    echo "Good news - it appears that there are NO duplicate DataFile objects in your database."
    echo "Your installation is ready to be upgraded to Dataverse 5.5"
    echo 
else

    echo "The following storage identifiers appear to be referenced from multiple non-harvested DvObjects:"
    cat /tmp/storageidentifiers.tmp
    echo "(output saved in /tmp/storageidentifiers.tmp)"

    echo "Looking up details for the affected datafiles:"

    cat /tmp/storageidentifiers.tmp | while read howmany dataset storageidentifier
    do
	PG_QUERY_SI=`printf "${PG_QUERY_5}" $dataset "$storageidentifier"`
	${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c "${PG_QUERY_SI}"
    done | tee /tmp/duplicates_info.tmp

    echo "(output saved in /tmp/duplicates_info.tmp)"

    echo 
    echo "Please send the output above to Dataverse support at support@dataverse.org." 
    echo "We will assist you in the process of cleaning up the affected files above."
    echo "We apologize for any inconvenience."
    echo 
fi


