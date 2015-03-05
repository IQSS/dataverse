#!/bin/bash
SERVER=http://localhost:8080/api

echo "Setting up Harvard-specific settings"
echo  "- Harvard Privacy Policy"
curl -s -X PUT -d http://best-practices.dataverse.org/harvard-policies/harvard-privacy-policy.html $SERVER/s/settings/:ApplicationPrivacyPolicyUrl
echo
