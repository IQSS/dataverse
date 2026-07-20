#! /bin/bash

# This script is an automated test to validate that issue https://github.com/IQSS/dataverse/issues/2454
# has been properly implemented.
# The issue requires that we "Distinguish between "read" and "write" permissions, make the "write" ones apply only to AuthenticatedUsers"
# To test this, we do the following:
# 1. Create a dataverse D in root
# 2. Create a new explicit group G in D, containing :guest and @anAuthUser
# 3. Assign the Admin role to G
# 4. Validation:
#    4.1 `:guest` can view unpublished dataverse, can't manage permissions there
#		 4.2 `@anAuthUser` can do both
#    4.3 `@anotherAuthUSer` can do none

#
# /!\ This script requires jq, psql and curl.
# /!\ You can set turn off the state setup by setting SETUP_NEEDED to anything that's not "yes"
#

#####
# Config - edit this to match your system.
ENDPOINT=http://localhost:8080/api
DB="psql dvndb -At -c "
ROOT_USER=dataverseAdmin
SETUP_NEEDED=yes


#####
# Setup = if needed
#
if [ $SETUP_NEEDED == "yes" ]; then
	echo SETTING UP

	ROOT_KEY=$($DB "select tokenstring \
								 from authenticateduser au inner join apitoken apt \
								 on au.id=apt.authenticateduser_id \
								 where useridentifier='$ROOT_USER'")

	echo $ROOT_USER api key is $ROOT_KEY

	# Create @anAuthUser
	USER_CREATION_KEY=$($DB "SELECT content FROM setting WHERE name=':BuiltinUsersKey'")
	AN_AUTH_USER_KEY=$( curl -s -X POST -d@anAuthUser.json -H"Content-type:application/json" $ENDPOINT/builtin-users?password=XXX\&key=$USER_CREATION_KEY | jq .data.apiToken | tr -d \")
	ANOTHER_AUTH_USER_KEY=$( curl -s -X POST -d@anotherAuthUser.json -H"Content-type:application/json" $ENDPOINT/builtin-users?password=XXX\&key=$USER_CREATION_KEY | jq .data.apiToken | tr -d \")
	echo
	echo user @anAuthUser created with key $AN_AUTH_USER_KEY

	# Create the test dataverses.
	curl -s -X POST -d@dataverse.json -H "Content-type:application/json" $ENDPOINT/dataverses/:root/?key=$ROOT_KEY
	echo
	echo Dataverse created

	# Create the group and add the users
	GROUP_ID=$( curl -s -X POST -d@group.json -H "Content-type:application/json" $ENDPOINT/dataverses/permissionsTestDv/groups/?key=$ROOT_KEY | jq .data.identifier | tr -d \" )
	echo Group created with id $GROUP_ID
	curl -s -X POST -d'[":guest","@anAuthUser"]' -H "Content-type:application/json" $ENDPOINT/dataverses/permissionsTestDv/groups/PTG/roleAssignees?key=$ROOT_KEY
	echo
	echo added users to group

	# Assign the "Admin" role to the group
	ASSIGNMENT="{\"assignee\":\"$GROUP_ID\", \"role\":\"admin\"}"
	curl -s -X POST -d"$ASSIGNMENT" -H "Content-type:application/json" $ENDPOINT/dataverses/permissionsTestDv/assignments/?key=$ROOT_KEY

	echo
	echo SETUP DONE
	echo

else
	echo Skipping setup
	AN_AUTH_USER_KEY=$($DB "select tokenstring \
								 from authenticateduser au inner join apitoken apt \
								 on au.id=apt.authenticateduser_id \
								 where useridentifier='anAuthUser'")
	ANOTHER_AUTH_USER_KEY=$($DB "select tokenstring \
								 from authenticateduser au inner join apitoken apt \
								 on au.id=apt.authenticateduser_id \
								 where useridentifier='anotherAuthUser'")
	echo
	echo Keys
	echo @anAuthUser $AN_AUTH_USER_KEY
	echo @anotherAuthUser $ANOTHER_AUTH_USER_KEY
fi

# Test permissions
echo :guest viewing inner dv ... expecting 200 OK
curl -si  $ENDPOINT/dataverses/permissionsTestDv | head -n 1
echo

echo @anAuthUser viewing inner dv ... expecting 200 OK
curl -si  $ENDPOINT/dataverses/permissionsTestDv?key=$AN_AUTH_USER_KEY | head -n 1
echo

echo @anotherAuthUser viewing inner dv ... expecting 401 Unauthorized
curl -si  $ENDPOINT/dataverses/permissionsTestDv?key=$ANOTHER_AUTH_USER_KEY | head -n 1
echo
# Assign the "Admin" role to the group

echo :guest setting permissions ... Expecting 401 Unauthorized
curl -si -X POST -d@assignment.json -H "Content-type:application/json" $ENDPOINT/dataverses/permissionsTestDv/assignments/ | head -n 1
echo

echo @anotherAuthUser setting permissions ... Expecting 401 Unauthorized
curl -si -X POST -d@assignment.json -H "Content-type:application/json" $ENDPOINT/dataverses/permissionsTestDv/assignments/?key=$ANOTHER_AUTH_USER_KEY | head -n 1
echo

echo @anAuthUser setting permissions ... Expecting 200 OK
curl -si -X POST -d@assignment.json -H "Content-type:application/json" $ENDPOINT/dataverses/permissionsTestDv/assignments/?key=$AN_AUTH_USER_KEY | head -n 1
echo
