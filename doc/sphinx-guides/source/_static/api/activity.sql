create temp view latestdownload as select df.id, max(responsetime) as latest 
from datafile df LEFT OUTER JOIN guestbookresponse gbr  on  df.id=gbr.datafile_id
group by df.id;

-- First, dataverses
select dtype, dv.name as name, owner.name as owner , 'N/A' as downloaded , publicationdate as published
from dataverse dv, dvobject dvo, dataverse owner
where dv.id=dvo.id
and owner.id = dvo.owner_id
and publicationdate is not null
UNION -- datasets
select dtype, dsfv.value as name, owner.name as owner, 'N/A' as downloaded, dsv.releasetime as published
from datasetfieldvalue dsfv, datasetfield dsf, datasetversion dsv, dvobject dvo, dataverse owner
where dsfv.datasetfield_id = dsf.id
and dsf.datasetversion_id = dsv.id
and dsv.dataset_id = dvo.id
and owner.id = dvo.owner_id
and dsf.datasetfieldtype_id=1
and dsv.id in
(select id from datasetversion where 
concat(datasetversion.dataset_id,':', datasetversion.versionnumber + (.01 * datasetversion.minorversionnumber)) in 
(select concat(datasetversion.dataset_id,':', max(datasetversion.versionnumber + (.01 * datasetversion.minorversionnumber))) as max 
from datasetversion
join dataset on dataset.id = datasetversion.dataset_id
where versionstate='RELEASED' 
and dataset.harvestingclient_id is null
group by dataset_id))
UNION -- datafile (NEED VIEW)
select dtype, fmd.label as name, dsfv.value as owner, latest::text as downloaded, dsv.releasetime as published
from filemetadata fmd, datasetversion dsv, datasetfieldvalue dsfv, datasetfield dsf, dvobject dvo, latestdownload ld
where fmd.datasetversion_id = dsv.id
and dsv.id = dsf.datasetversion_id
and dsf.id = dsfv.datasetfield_id
and fmd.datafile_id = dvo.id
and dsf.datasetfieldtype_id=1
and fmd.datafile_id = ld.id
and dsv.id in
(select id from datasetversion where 
concat(datasetversion.dataset_id,':', datasetversion.versionnumber + (.01 * datasetversion.minorversionnumber)) in 
(select concat(datasetversion.dataset_id,':', max(datasetversion.versionnumber + (.01 * datasetversion.minorversionnumber))) as max 
from datasetversion
join dataset on dataset.id = datasetversion.dataset_id
where versionstate='RELEASED' 
and dataset.harvestingclient_id is null
group by dataset_id))
order by published, owner;
