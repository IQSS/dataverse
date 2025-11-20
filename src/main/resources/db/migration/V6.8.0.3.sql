modify datasetversion.deaccessionnote entries with new Bundle string 'file.deaccessionDialog.reason.selectItem.notValid'
UPDATE dvobject SET indextime=null WHERE id in (SELECT dataset_id FROM datasetversion WHERE deaccessionnote='Not a valid dataset.');
UPDATE datasetversion SET deaccessionnote='Not valid. This dataset does not comply with repository policies.' WHERE deaccessionnote='Not a valid dataset.';
