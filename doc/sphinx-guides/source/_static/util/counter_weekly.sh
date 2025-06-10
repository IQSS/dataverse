#!/bin/sh
#counter_weekly.sh

# This script iterates through all published Datasets in all Dataverses and calls the Make Data Count API to update their citations from DataCite
# Note: Requires curl and jq for parsing JSON responses form curl

# A recursive method to process each Dataverse
processDV () {
echo "Running counter_weekly.sh on $(date)"
echo "Processing Dataverse ID#: $1"

#Call the Dataverse API to get the contents of the Dataverse (without credentials, this will only list published datasets and dataverses
DVCONTENTS=$(curl -s http://localhost:8080/api/dataverses/$1/contents)

# Iterate over all datasets, pulling the value of their DOIs (as part of the persistentUrl) from the json returned
for subds in $(echo "${DVCONTENTS}" | jq -r '.data[] | select(.type == "dataset") | .persistentUrl'); do

#The authority/identifier are preceded by a protocol/host, i.e. https://doi.org/
DOI=`expr "$subds" : '.*:\/\/\doi\.org\/\(.*\)'`

# Call the Dataverse API for this dataset and get the response
RESULT=$(curl -s -X POST "http://localhost:8080/api/admin/makeDataCount/:persistentId/updateCitationsForDataset?persistentId=doi:$DOI" )
# Parse the status and number of citations found from the response
STATUS=$(echo "$RESULT" | jq -j '.status' )
CITATIONS=$(echo "$RESULT" | jq -j '.data.citationCount')

# The status for a call that worked
OK='OK'

# Check the status and report
if [ "$STATUS" = "$OK" ]; then
        echo "Updated: $CITATIONS citations for doi:$DOI"
else
        echo "Failed to update citations for doi:$DOI"
        echo "Run curl -s -X POST 'http://localhost:8080/api/admin/makeDataCount/:persistentId/updateCitationsForDataset?persistentId=doi:$DOI ' to retry/see the error message"
fi
#processDV $subds
done

# Now iterate over any child Dataverses and recursively process them
for subdv in $(echo "${DVCONTENTS}" | jq -r '.data[] | select(.type == "dataverse") | .id'); do
echo $subdv
processDV $subdv
done

}

# Call the function on the root dataverse to start processing
processDV 1
