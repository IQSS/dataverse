#!/bin/bash
SERVER=http://localhost:8080/api

echo "Setting up Harvard-specific settings"
# :Authority and :Shoulder are commented out so this script can be used on test servers
#curl -X PUT -d 10.7910 "$SERVER/admin/settings/:Authority"
#curl -X PUT -d "DVN/" "$SERVER/admin/settings/:Shoulder"
echo "- Application Status header"
curl -s -X PUT -d 'Upgrade in progress...' $SERVER/admin/settings/:StatusMessageHeader
echo "- Application Status message"
curl -s -X PUT -d 'Dataverse is currently being upgraded. You can see the features, bug fixes, and other upgrades for this release in the <a href="http://roadmap.datascience.iq.harvard.edu/milestones/milestone-roadmap/dataverse/" title="Dataverse Roadmap" target="_blank">Dataverse Roadmap</a>.' $SERVER/admin/settings/:StatusMessageText 
echo  "- Harvard Privacy Policy"
curl -s -X PUT -d https://dataverse.org/best-practices/harvard-dataverse-privacy-policy $SERVER/admin/settings/:ApplicationPrivacyPolicyUrl
curl -s -X PUT -d https://dataverse.org/best-practices/harvard-api-tou $SERVER/admin/settings/:ApiTermsOfUse
echo "- Configuring Harvard's password policy in Dataverse"
# Min length is 10 because that is the minimum Harvard requires without periodic expiration
curl -s -X PUT -d 10 $SERVER/admin/settings/:PVMinLength
# If password 20+ characters, other rules do not apply
curl -s -X PUT -d 20 $SERVER/admin/settings/:PVGoodStrength
# The character classes users can choose between and the number of each needed
curl -X PUT -d 'UpperCase:1,Digit:1,LowerCase:1,Special:1' $SERVER/admin/settings/:PVCharacterRules
# The number of character classes a password needs to be valid
curl -s -X PUT -d 3 $SERVER/admin/settings/:PVNumberOfCharacteristics
# The number of character classes a password needs to be valid
curl -s -X PUT -d 4 $SERVER/admin/settings/:PVNumberOfConsecutiveDigitsAllowed
# Harvard requires a dictionary check on common words & names. We use the unix 'words' file, removing ones less than 4 characters. Policy clarification received by Harvard Key was no words 4 characters or longer.
DIR="/usr/local/glassfish4/glassfish/domains/domain1/files" #this can be replaced with a different file path for storing the dictionary
sed '/^.\{,3\}$/d' /usr/share/dict/words > $DIR/pwdictionary
curl -s -X PUT -d "$DIR/pwdictionary" $SERVER/admin/settings/:PVDictionaries
echo "- Adjust Solr frag size"
curl -s -X PUT -d 320 $SERVER/admin/settings/:SearchHighlightFragmentSize
echo  "- Google Analytics setting"
curl -X PUT -d true "$SERVER/admin/settings/:ScrubMigrationData"
echo  "- Enabling Shibboleth"
curl -X POST -H "Content-type: application/json" http://localhost:8080/api/admin/authenticationProviders --upload-file ../../doc/sphinx-guides/source/_static/installation/files/etc/shibboleth/shibAuthProvider.json
echo  "- Enabling TwoRavens"
curl -X POST -H 'Content-type: application/json' --upload-file ../../doc/sphinx-guides/source/_static/installation/files/root/external-tools/twoRavens.json http://localhost:8080/api/admin/externalTools
echo  "- Setting system email"
curl -X PUT -d "Harvard Dataverse Support <support@dataverse.harvard.edu>" http://localhost:8080/api/admin/settings/:SystemEmail
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
echo "Because you have loaded custom metadata blocks, you need to update the include files pulled in by Solr's schema.xml. On the Solr server, you can try running the updateSchemaMDB.sh script mentioned in the Metadata Customization section of the Admin Guide or follow the manual steps listed there."
echo
