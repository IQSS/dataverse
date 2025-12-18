#!/bin/sh
#counter_weekly.sh

# This script iterates through all published Datasets in all Dataverses and calls the Make Data Count API to update their citations from DataCite
# Note: Requires curl and jq for parsing JSON responses form curl

# A recursive method to process each Dataverse
processDV () {
echo "Processing Dataverse ID#: $1"

#Call the Dataverse API to get the contents of the Dataverse (without credentials, this will only list published datasets and dataverses
DVCONTENTS=$(curl -s http://localhost:8080/api/dataverses/$1/contents)

# Iterate over all datasets, pulling the value of their DOIs (as part of the persistentUrl) from the json returned
for subds in $(echo "${DVCONTENTS}" | jq -r '.data[] | select(.type == "dataset") | .persistentUrl'); do

#The authority/identifier are preceded by a protocol/host, i.e. https://doi.org/
DOI=`expr "$subds" : '.*:\/\/\doi\.org\/\(.*\)'`

# Call the Dataverse API for this dataset and capture both the response and HTTP status code
HTTP_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "http://localhost:8080/api/admin/makeDataCount/:persistentId/updateCitationsForDataset?persistentId=doi:$DOI")

# Extract the HTTP status code from the last line
HTTP_STATUS=$(echo "$HTTP_RESPONSE" | tail -n1)
# Extract the response body (everything except the last line)
RESPONSE_BODY=$(echo "$HTTP_RESPONSE" | sed '$d')

# Check the HTTP status code and report accordingly
case $HTTP_STATUS in
    200)
        # Successfully queued
        # Extract status from the nested data object
        STATUS=$(echo "$RESPONSE_BODY" | jq -r '.data.status')
        
        # Extract message from the nested data object
        if echo "$RESPONSE_BODY" | jq -e '.data.message' > /dev/null 2>&1 && [ "$(echo "$RESPONSE_BODY" | jq -r '.data.message')" != "null" ]; then
            MESSAGE=$(echo "$RESPONSE_BODY" | jq -r '.data.message')
            echo "[SUCCESS] doi:$DOI - $STATUS: $MESSAGE"
        else
            # If message is missing or null, just show the status
            echo "[SUCCESS] doi:$DOI - $STATUS: Citation update queued"
        fi
        ;;
    400)
        # Bad request
        if echo "$RESPONSE_BODY" | jq -e '.message' > /dev/null 2>&1; then
            ERROR=$(echo "$RESPONSE_BODY" | jq -r '.message')
            echo "[ERROR 400] doi:$DOI - Bad request: $ERROR"
        else
            echo "[ERROR 400] doi:$DOI - Bad request"
        fi
        ;;
    404)
        # Not found
        if echo "$RESPONSE_BODY" | jq -e '.message' > /dev/null 2>&1; then
            ERROR=$(echo "$RESPONSE_BODY" | jq -r '.message')
            echo "[ERROR 404] doi:$DOI - Not found: $ERROR"
        else
            echo "[ERROR 404] doi:$DOI - Not found"
        fi
        ;;
    503)
        # Service unavailable (queue full)
        if echo "$RESPONSE_BODY" | jq -e '.message' > /dev/null 2>&1; then
            ERROR=$(echo "$RESPONSE_BODY" | jq -r '.message')
            echo "[ERROR 503] doi:$DOI - Service unavailable: $ERROR"
        elif echo "$RESPONSE_BODY" | jq -e '.data.message' > /dev/null 2>&1; then
            ERROR=$(echo "$RESPONSE_BODY" | jq -r '.data.message')
            echo "[ERROR 503] doi:$DOI - Service unavailable: $ERROR"
        else
            echo "[ERROR 503] doi:$DOI - Service unavailable: Queue is full"
        fi
        ;;
    *)
        # Other error
        echo "[ERROR $HTTP_STATUS] doi:$DOI - Unexpected error"
        echo "Response: $RESPONSE_BODY"
        ;;
esac

done

# Now iterate over any child Dataverses and recursively process them
for subdv in $(echo "${DVCONTENTS}" | jq -r '.data[] | select(.type == "dataverse") | .id'); do
echo $subdv
processDV $subdv
done

}

# Call the function on the root dataverse to start processing 
processDV 1