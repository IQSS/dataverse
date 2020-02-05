#!/bin/sh

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
# psql executable, add full path if necessary:
PSQL_EXEC=psql

# end config

PG_QUERY_0="SELECT COUNT(DISTINCT o.id) FROM datafile f, dataset s, dvobject p, dvobject o WHERE s.id = p.id AND o.id = f.id AND o.owner_id = s.id AND s.harvestingclient_id IS null AND o.storageidentifier IS NOT null"

PG_QUERY_1="SELECT COUNT(DISTINCT (o.owner_id,o.storageidentifier)) FROM datafile f, dataset s, dvobject p, dvobject o WHERE s.id = p.id AND o.id = f.id AND o.owner_id = s.id AND s.harvestingclient_id IS null AND o.storageidentifier IS NOT null"

PG_QUERY_2="SELECT s.id, o.storageidentifier FROM datafile f, dataset s, dvobject o WHERE o.id = f.id AND o.owner_id = s.id AND s.harvestingclient_id IS null AND o.storageidentifier IS NOT null ORDER by o.storageidentifier"

PG_QUERY_3="SELECT p.authority, p.identifier, o.storageidentifier, o.id, o.createdate, f.contenttype FROM datafile f, dvobject p, dvobject o WHERE o.id = f.id AND o.owner_id = p.id AND o.storageidentifier='%s' ORDER by o.id"

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

echo "Checking the number of unique storage identifiers, within unique datasets..."

NUM_STORAGEIDENTIFIERS=`${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c "${PG_QUERY_1}"`

echo $NUM_STORAGEIDENTIFIERS total.


if [ $NUM_DATAFILES == $NUM_STORAGEIDENTIFIERS ]
then
    echo 
    echo "Good news - the numbers check out!"
    echo "It looks like there are no duplicate dvObjects in your database."
    echo "Your installation is ready to be upgraded to Dataverse 4.20."
    echo 
    exit 0
fi

echo 
echo "A (potential) mismatch is detected!"
echo "Some cleanup may be required before your installation can be upgraded to Dataverse 4.20."
echo 

echo "The following storage identifiers appear to be referenced from multiple DvObjects:"

${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c "${PG_QUERY_2}" |
uniq -c | 
awk '{if ($1 > 1) print $NF}' | tee /tmp/storageidentifiers.tmp

echo "(output saved in /tmp/storageidentifiers.tmp)"

NUM_CONFIRMED=`cat /tmp/storageidentifiers.tmp | wc -l`

if [ $NUM_CONFIRMED == 0 ]
then
    echo 
    echo "Good news - on a closer look, it appears that "
    echo "there are NO duplicate dvObjects in your database."
    echo "Your installation is ready to be upgraded to Dataverse 4.20."
    echo 
    exit 0
fi


echo "Looking up details for the affected datafiles:"

cat /tmp/storageidentifiers.tmp | while read si
do
    PG_QUERY_SI=`printf "${PG_QUERY_3}" $si`
    ${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c "${PG_QUERY_SI}"
done | tee /tmp/duplicates_info.tmp

echo "(output saved in /tmp/duplicates_info.tmp)"

echo 
echo "Please send the output above to Dataverse support." 
echo "We will assist you in the database cleanup that needs to happen "
echo "before your installation can be upgraded to Dataverse 4.20."
echo "We apologize for any inconvenience."
echo 
