select dataset_id, count(*) from datasetversion where versionstate='DRAFT' group by dataset_id having count(*) >1;
