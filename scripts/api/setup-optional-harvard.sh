#!/bin/bash
SERVER=http://localhost:8080/api

echo "Setting up Harvard-specific settings"
echo  "- Harvard Privacy Policy"
curl -s -X PUT -d http://best-practices.dataverse.org/harvard-policies/harvard-privacy-policy.html $SERVER/s/settings/:ApplicationPrivacyPolicyUrl
echo  "- Enabling Shibboleth"
curl -X PUT -d true http://localhost:8080/api/s/settings/:ShibEnabled
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/customMRA.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/customGSD.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/customARCS.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/customPSRI.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/customPSI.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/customCHIA.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/customDigaai.tsv -H "Content-type: text/tab-separated-values"
echo
