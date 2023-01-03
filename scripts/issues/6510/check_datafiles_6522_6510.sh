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

# first issue, duplicate datafiles: (#6522)

PG_QUERY_0="SELECT COUNT(DISTINCT o.id) FROM datafile f, dataset s, dvobject p, dvobject o WHERE s.id = p.id AND o.id = f.id AND o.owner_id = s.id AND s.harvestingclient_id IS null AND o.storageidentifier IS NOT null"

PG_QUERY_1="SELECT s.id, o.storageidentifier FROM datafile f, dataset s, dvobject o WHERE o.id = f.id AND o.owner_id = s.id AND s.harvestingclient_id IS null AND o.storageidentifier IS NOT null ORDER by o.storageidentifier"

PG_QUERY_2="SELECT p.authority, p.identifier, o.storageidentifier, o.id, o.createdate, f.contenttype FROM datafile f, dvobject p, dvobject o WHERE o.id = f.id AND o.owner_id = p.id AND o.storageidentifier='%s' ORDER by o.id"

PGPASSWORD=$pg_pass; export PGPASSWORD

echo "Checking the number of non-harvested datafiles in the database..."

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

echo "Let's check if any storage identifiers are referenced more than once within the same dataset:" 

${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c "${PG_QUERY_1}" |
uniq -c | 
awk '{if ($1 > 1) print $NF}' > /tmp/storageidentifiers.tmp

NUM_CONFIRMED=`cat /tmp/storageidentifiers.tmp | wc -l`

if [ $NUM_CONFIRMED == 0 ]
then
    echo 
    echo "Good news - it appears that there are NO duplicate DataFile objects in your database."
    echo "Your installation is ready to be upgraded to Dataverse 4.20."
    echo 
else

    echo "The following storage identifiers appear to be referenced from multiple DvObjects:"
    cat /tmp/storageidentifiers.tmp
    echo "(output saved in /tmp/storageidentifiers.tmp)"

    echo "Looking up details for the affected datafiles:"

    cat /tmp/storageidentifiers.tmp | while read si
    do
	PG_QUERY_SI=`printf "${PG_QUERY_2}" $si`
	${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c "${PG_QUERY_SI}"
    done | tee /tmp/duplicates_info.tmp

    echo "(output saved in /tmp/duplicates_info.tmp)"

    echo 
    echo "Please send the output above to Dataverse support at support@dataverse.org." 
    echo "We will assist you in the process of cleaning up the affected files above."
    echo "We apologize for any inconvenience."
    echo 
fi

# second issue, repeated ingests: (issue #6510)

PG_QUERY_3="SELECT COUNT(DISTINCT o.id) FROM datafile f, dataset s, dvobject o, datatable t WHERE o.id = f.id AND o.owner_id = s.id AND t.datafile_id = f.id AND s.harvestingclient_id IS null AND o.storageidentifier IS NOT null"

PG_QUERY_4="SELECT t.id, f.id FROM datafile f, dataset s, dvobject o, datatable t WHERE o.id = f.id AND o.owner_id = s.id AND t.datafile_id = f.id AND s.harvestingclient_id IS null AND o.storageidentifier IS NOT null ORDER by f.id, t.id"

PG_QUERY_5="SELECT p.authority, p.identifier, o.storageidentifier, o.id, t.id, o.createdate, f.contenttype, t.originalfileformat FROM datafile f, dvobject p, dvobject o, datatable t WHERE o.id = f.id AND t.datafile_id = f.id AND o.owner_id = p.id AND o.id='%s' ORDER by t.id"

echo
echo "Checking the number of ingested (\"tabular\") datafiles in the database..."

NUM_DATAFILES=`${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c "${PG_QUERY_3}"`

echo $NUM_DATAFILES total.
echo 

echo "Let's check if any of these ingested files have MORE THAN ONE linked datatable objects:" 

${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c "${PG_QUERY_4}" |
uniq -c -f 1 | 
awk '{if ($1 > 1) print $NF}' > /tmp/datafileids.tmp

NUM_CONFIRMED=`cat /tmp/datafileids.tmp | wc -l`

if [ $NUM_CONFIRMED == 0 ]
then
    echo 
    echo "Good news - it appears that there are no tabular files affected by this issue in your database."
    echo 
    exit 0
fi

echo "The following "${NUM_CONFIRMED}" DataFile ids appear to be referenced from multiple DataTables:"
cat /tmp/datafileids.tmp
echo "(output saved in /tmp/datafileids.tmp)"

echo "Looking up details for the affected tabular files:"

cat /tmp/datafileids.tmp | while read si
do
    PG_QUERY_SI=`printf "${PG_QUERY_5}" $si`
    ${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c "${PG_QUERY_SI}"
done | tee /tmp/multiple_ingests_info.tmp

echo "(output saved in /tmp/multiple_ingests_info.tmp)"

echo 
echo "Please send the output above to Dataverse support at support@dataverse.org." 
echo "We will assist you in fixing this issue in your Dataverse database."
echo "We apologize for any inconvenience."
echo 
