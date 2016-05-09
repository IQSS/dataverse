#!/bin/bash


SERVER=http://localhost:8080/api

echo  "- Enabling Publish Popup Custom Text"
curl -s -X PUT -d true "$SERVER/admin/settings/:DatasetPublishPopupCustomTextOnAllVersions"
curl -s -X PUT -d "Deposit License Requirements"  "$SERVER/admin/settings/:DatasetPublishPopupCustomText"