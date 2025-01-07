-- #8739 map publisher tag to distributorName when harvesting
update foreignmetadatafieldmapping set datasetfieldname = 'distributorName' where foreignfieldxpath = ':publisher';
