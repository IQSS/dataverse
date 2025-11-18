# Language change for file.deaccessionDialog.reason.selectItem.notValid

"Not a valid dataset." is being changed to "Not valid. This dataset does not comply with repository policies."
This is the default English language version. For installations using customized languages, replacing the Bundle.properties file, please follow these manual instructions to make this modification, if desired.

The SQL statements to modify the datasets is:
UPDATE dvobject SET indextime=null WHERE id in (SELECT dataset_id FROM datasetversion WHERE deaccessionnote='Not a valid dataset.');
UPDATE datasetversion SET deaccessionnote='Not valid. This dataset does not comply with repository policies.' WHERE deaccessionnote='Not a valid dataset.';

Once the database is updated the Solr indexes need to be rebuilt using the following Admin API:

curl http://localhost:8080/api/admin/index/continue
