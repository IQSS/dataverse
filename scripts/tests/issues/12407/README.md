Semi-automated test
===================

This is a semi-automated test to check the API endpoints that changed by this [pull request](https://github.com/IQSS/dataverse/pull/12407).

Adjust the configuration variables at the start of the script. 
* Run the _python3_ script before deploying the pull request.
* Remove the resulting draft version of the dataset.
* Deploy the pull request.
* Run the script again.

Result before deploy
--------------------

All requests to the API endpoints return 200-OK status code.
As a result the dataset will contain conflicting file/directorry paths for foo and foo/bar.

Running `scripts/issues/12407/find_duplicates.py` should show the conflicting dataset and file metadata. Note that a draft dataset has no version number. Currently `foo.tab` is a false detection.

### Example of results

|datasetversion_id|path|protocol|authority|dataset_id|versionnumber|minorversionnumber|
|---|---|---|---|---|---|---|
|4|foo|doi|10.5072|DAR/HBGPN5		
|4|foo/bar|doi|10.5072|DAR/HBGPN5		
|4|foo.tab|doi|10.5072|DAR/HBGPN5

![](before-deploy.png)

Result after deploy
-------------------
Output with dashed lines show expected status codes and further notes.

![](after-deploy.png)
