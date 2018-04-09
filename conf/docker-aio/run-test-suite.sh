#!/bin/sh
# This is the canonical list of which "IT" tests are expected to pass.
# Please note the "dataverse.test.baseurl" is set to run for "all-in-one" Docker environment.
mvn test -Dtest=DataversesIT,DatasetsIT,SwordIT,AdminIT,BuiltinUsersIT,UsersIT,UtilIT,ConfirmEmailIT,FileMetadataIT,FilesIT,SearchIT,InReviewWorkflowIT,HarvestingServerIT -Ddataverse.test.baseurl='http://localhost:8083'
