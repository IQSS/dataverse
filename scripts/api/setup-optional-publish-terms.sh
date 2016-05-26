#!/bin/bash


SERVER=http://localhost:8080/api

echo  "- Enabling Publish Popup Custom Text"
curl -s -X PUT -d true "$SERVER/admin/settings/:DatasetPublishPopupCustomTextOnAllVersions"
curl -X PUT -d "By default datasets are published with the CC0-“Public Domain Dedication” waiver. Learn more about the CC0 waiver <a target="_blank" href='http://creativecommons.org/choose/zero/'>here</a>. <br><br> To publish with custom Terms of Use, click the Cancel button and go to the Terms tab for this dataset." $SERVER/admin/settings/:DatasetPublishPopupCustomText