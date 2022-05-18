###Duplicate Templates in Database
Prior to this release making a copy of a Dataset Template was creating two copies, one of which is visible in the dataverse, the other not being assigned 
a dataverse was invisible to the user (https://github.com/IQSS/dataverse/issues/8600). This release fixes the issue. 

If you would like to remove these orphan templates you may run the follwing script:

https://github.com/IQSS/dataverse/raw/develop/scripts/issues/8600/delete_orphan_templates_8600.sh

In order to support the script admin APIs for finding and deleting templates have been added.
