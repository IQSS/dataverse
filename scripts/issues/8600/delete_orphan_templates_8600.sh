#!/bin/bash

# begin config
# PostgresQL credentials:
# edit the following lines so that psql can talk to your database
pg_host=localhost
pg_port=5432
pg_user=dvnapp
pg_db=dvndb
#edit this line with the server for the api call to delete orphan templates
SERVER=http://localhost:8080/api
# you can leave the password blank, if Postgres is configured 
# to accept connections without auth:
pg_pass=
# psql executable - add full path, if necessary:
PSQL_EXEC=psql

# end config

# check for orphan template : 

PG_QUERY_0="SELECT count(t.id) FROM template t WHERE dataverse_id is null;"
PG_QUERY_1="SELECT t.id, ',' FROM template t WHERE dataverse_id is null;"

PGPASSWORD=$pg_pass; export PGPASSWORD

echo "Checking the total number of orphan templates in database..."

NUM_TEMPLATES=`${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c "${PG_QUERY_0}"`
if [ $? != 0 ]
then
    echo "FAILED to execute psql! Check the credentials and try again?"
    echo "exiting..."
    echo 
    echo "the command line that failed:"
    echo "${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c \"${PG_QUERY_0}\""
    exit 1
fi

echo $NUM_TEMPLATES total.
echo 


TEMPLATE_ID=`${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c "${PG_QUERY_1}"`

echo $TEMPLATE_ID  to remove.


${PSQL_EXEC}  -h ${pg_host} -U ${pg_user} -d ${pg_db} -t -c "${PG_QUERY_1}" \
    -t \
    --field-separator ' ' \
    --quiet \
| while read -a Record ; do
    id=${Record[0]}

	if [[ "x${id}" = "x" ]]
	then
		echo "nothing to do here"
        else
            curl -X DELETE $SERVER/admin/template/${id}
            echo "template ${id} deleted "	
        fi

done

NUM_TEMPLATES=`${PSQL_EXEC} -h ${pg_host} -U ${pg_user} -d ${pg_db} -tA -F ' ' -c "${PG_QUERY_0}"`

echo $NUM_TEMPLATES orphan templates remaining.


