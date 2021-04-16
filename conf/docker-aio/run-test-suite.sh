#!/bin/bash
# This is the canonical list of which "IT" tests are expected to pass.

dvurl=$1
if [ -z "$dvurl" ]; then
	dvurl="http://localhost:8084"
fi

# Please note the "dataverse.test.baseurl" is set to run for "all-in-one" Docker environment.
# TODO: Rather than hard-coding the list of "IT" classes here, add a profile to pom.xml.
source maven/maven.sh && mvn test -Dtest=DataversesIT,DatasetsIT,SwordIT,AdminIT,BuiltinUsersIT,UsersIT,UtilIT,ConfirmEmailIT,FileMetadataIT,FilesIT,SearchIT,InReviewWorkflowIT,HarvestingServerIT,MoveIT,MakeDataCountApiIT,FileTypeDetectionIT,EditDDIIT,ExternalToolsIT,AccessIT,DuplicateFilesIT,DownloadFilesIT,LinkIT,DeleteUsersIT,DeactivateUsersIT -Ddataverse.test.baseurl=$dvurl
