#!/bin/sh
export API_TOKEN=e62ac03b-3bc8-4194-bcd6-d0381fe84e39
export SERVER_URL=http://localhost:8080
export ID=:root
export FILENAME='π.png'
export CONTENT='test1'
export DISPLAY_ORDER=0

curl -H "X-Dataverse-key:$API_TOKEN" \
     -X POST \
     -F "id=0" \
     -F "content=$CONTENT" \
     -F "displayOrder=$DISPLAY_ORDER" \
     -F "fileName=$FILENAME" \
     -F "keepFile=false" \
     -F "file=@$FILENAME" \
     "$SERVER_URL/api/dataverses/$ID/featuredItems"
