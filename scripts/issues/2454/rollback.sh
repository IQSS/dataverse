#!/bin/bash

ENDPOINT=http://localhost:8080/api
DB="psql dvndb -At -c "
ROOT_USER=dataverseAdmin
ROOT_KEY=$($DB "select tokenstring \
							 from authenticateduser au inner join apitoken apt \
							 on au.id=apt.authenticateduser_id \
							 where useridentifier='$ROOT_USER'")

echo $ROOT_USER api key is $ROOT_KEY


# delete DV
curl -X DELETE $ENDPOINT/dataverses/permissionsTestDv?key=$ROOT_KEY
echo
echo dataverses deleted
echo

# delete user
for USER_NICK in anAuthUser anotherAuthUser
do
	echo deleting user $USER_NICK
	QUERY="select id from authenticateduser where useridentifier='$USER_NICK'"
	AUTH_USER_ID=$($DB "$QUERY")
	echo Auth user id is $AUTH_USER_ID
	$DB "delete from apitoken where authenticateduser_id=$AUTH_USER_ID"
	$DB "delete from authenticateduserlookup where authenticateduser_id=$AUTH_USER_ID"
	$DB "delete from authenticateduser where id=$AUTH_USER_ID"
	$DB "delete from builtinuser where id=$AUTH_USER_ID"
done
