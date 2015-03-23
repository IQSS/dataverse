#!/bin/bash
SERVER=http://localhost:8080/api

echo "Setting up Harvard-specific settings"
echo  "- Harvard Privacy Policy"
curl -s -X PUT -d http://best-practices.dataverse.org/harvard-policies/harvard-privacy-policy.html $SERVER/s/settings/:ApplicationPrivacyPolicyUrl
curl -X PUT -d true "$SERVER/s/settings/:ScrubMigrationData"
echo  "- Enabling Shibboleth"
curl -X PUT -d true http://localhost:8080/api/s/settings/:ShibEnabled
echo "- Setting up the Harvard Shibboleth institutional group"
curl -s -X POST -H 'Content-type:application/json' --upload-file data/shibGroupHarvard.json "$SERVER/groups/shib?key=$adminKey"
echo
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/customMRA.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/customGSD.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/customARCS.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/customPSRI.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/customPSI.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/customCHIA.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/customDigaai.tsv -H "Content-type: text/tab-separated-values"
echo
