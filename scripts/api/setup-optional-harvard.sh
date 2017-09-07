#!/bin/bash
SERVER=http://localhost:8080/api

echo "Setting up Harvard-specific settings"
echo "- Application Status header"
curl -s -X PUT -d 'Upgrade in progress...' $SERVER/admin/settings/:StatusMessageHeader
echo "- Application Status message"
curl -s -X PUT -d 'Dataverse is currently being upgraded. You can see the features, bug fixes, and other upgrades for this release in the <a href="http://roadmap.datascience.iq.harvard.edu/milestones/milestone-roadmap/dataverse/" title="Dataverse Roadmap" target="_blank">Dataverse Roadmap</a>.' $SERVER/admin/settings/:StatusMessageText 
echo  "- Harvard Privacy Policy"
curl -s -X PUT -d http://best-practices.dataverse.org/harvard-policies/harvard-privacy-policy.html $SERVER/admin/settings/:ApplicationPrivacyPolicyUrl
curl -s -X PUT -d http://best-practices.dataverse.org/harvard-policies/harvard-api-tou.html $SERVER/admin/settings/:ApiTermsOfUse
echo "- Configuring Harvard's password policy in Dataverse"
#put harvard rules here

echo "- Adjust Solr frag size"
curl -s -X PUT -d 320 $SERVER/admin/settings/:SearchHighlightFragmentSize
echo  "- Google Analytics setting"
curl -X PUT -d true "$SERVER/admin/settings/:ScrubMigrationData"
echo  "- Enabling Shibboleth"
curl -X POST -H "Content-type: application/json" http://localhost:8080/api/admin/authenticationProviders --upload-file ../../doc/sphinx-guides/source/_static/installation/files/etc/shibboleth/shibAuthProvider.json
echo  "- Enabling TwoRavens"
curl -s -X PUT -d true "$SERVER/admin/settings/:TwoRavensTabularView"
echo  "- Enabling Geoconnect"
curl -s -X PUT -d true "$SERVER/admin/settings/:GeoconnectCreateEditMaps"
curl -s -X PUT -d true "$SERVER/admin/settings/:GeoconnectViewMaps"
echo  "- Setting system email"
curl -X PUT -d "Harvard Dataverse Support <support@dataverse.org>" http://localhost:8080/api/admin/settings/:SystemEmail
curl -X PUT -d ", The President &#38; Fellows of Harvard College" http://localhost:8080/api/admin/settings/:FooterCopyright
echo "- Setting up the Harvard Shibboleth institutional group"
curl -s -X POST -H 'Content-type:application/json' --upload-file data/shibGroupHarvard.json "$SERVER/admin/groups/shib?key=$adminKey"
echo
echo "- Setting up the MIT Shibboleth institutional group"
curl -s -X POST -H 'Content-type:application/json' --upload-file data/shibGroupMit.json "$SERVER/admin/groups/shib?key=$adminKey"
echo
curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/customMRA.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/customGSD.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/customARCS.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/customPSRI.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/customPSI.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/customCHIA.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/customDigaai.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/custom_hbgdki.tsv -H "Content-type: text/tab-separated-values"
echo
