ENDPOINT=https://localhost:8181
APIKEY=a65048f8-875c-4479-a91d-33cb8cd12821
DATASET=3

echo Calling:
echo curl --insecure $ENDPOINT/api/datasets/$DATASET/versions/:latest?key=$APIKEY
echo
echo curl --insecure -X PUT -H "Content-Type:application/json" -d@dataset-metadata-next.json $ENDPOINT/api/datasets/$DATASET/versions/:draft?key=$APIKEY
echo


# get data:
# curl --insecure $ENDPOINT/api/datasets/$DATASET/versions/:latest?key=$APIKEY
