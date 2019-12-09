-- This query only deals with the datasets that are immediate children of the root dataverse; so we don't need to bother with building the tree/path structures.
WITH maxVersion as (select id  from datasetversion where
concat(datasetversion.dataset_id,':', datasetversion.versionnumber + (.01 * datasetversion.minorversionnumber)) in
(select concat(datasetversion.dataset_id,':', max(datasetversion.versionnumber + (.01 * datasetversion.minorversionnumber))) as max
from datasetversion
join dataset on dataset.id = datasetversion.dataset_id
where versionstate='RELEASED'
and dataset.harvestingclient_id is null
group by dataset_id)), filedatefilter as (select id from dvobject where dtype='DataFile'
-- Make sure the file is published.
and publicationdate is not NULL
-- Optionally subset by file publication date.
--and publicationdate BETWEEN '2018-01-01' AND '2018-12-31'
)
select
    -- ID of the file
    dvo.id as fileid,
    fmd.label as filename,
    -- Strip out newlines and tabs in dataset titles.
    regexp_replace(dsfv.value, '[\r\n\t]+', ' ', 'g' ) as dataset_name,
    -- Only the datasets that are direct children of the root dataverse:
    rootdv.id as dataverse_level_1_id,
    rootdv.alias as dataverse_level_1_alias,
    rootdv.name as dataverse_level_1_friendly_name,
    -- No second level dataverses for these files:
    NULL as dataverse_level_2_id,
    NULL as dataverse_level_2_alias,
    NULL as dataverse_level_2_friendly_name,
    -- And no third level dataverses either:
    NULL as dataverse_level_3_id,
    NULL as dataverse_level_3_alias,
    NULL as dataverse_level_3_friendly_name,
    -- Separate multiple subjects with a delimeter.
    string_agg(cvv.strvalue, ';') AS subjects,
    -- Dataset publication date.
    dsv.releasetime as dataset_publication_date
    -- File creation date.
    --dvo.createdate as file_creation_date,
    -- File publication date.
    --dvo.publicationdate as file_publication_date
from filemetadata fmd, datasetversion dsv,
datasetfieldvalue dsfv, datasetfield dsf,
datasetfield dsfsub, datasetfield_controlledvocabularyvalue dsfcvv,
controlledvocabularyvalue cvv, dvobject dvo,
dvobject datasetdvo, dvobject rootdvo,
dataverse rootdv, dataset, maxversion, filedatefilter
where fmd.datasetversion_id = dsv.id
and dsv.dataset_id = dataset.id
and datasetdvo.id = dataset.id
and datasetdvo.owner_id = rootdv.id
and dvo.owner_id = dataset.id
and rootdvo.owner_id IS null
and rootdv.id = rootdvo.id
and dsv.id = dsf.datasetversion_id
and dsf.id = dsfv.datasetfield_id
and fmd.datafile_id = dvo.id
and dsf.datasetfieldtype_id=1
-- We added dsfsub and dsfcvv to get subjects.
and dsfsub.datasetversion_id = dsv.id
-- For dev databases, "subject" is 20.
and dsfsub.datasetfieldtype_id=19
-- For Harvard's database, "subject" is 19.
--and dsfsub.datasetfieldtype_id=19
and dsfsub.id = dsfcvv.datasetfield_id
and cvv.datasetfieldtype_id = dsfsub.datasetfieldtype_id
and dsfcvv.controlledvocabularyvalues_id = cvv.id
-- Make sure the dataset is published.
and dsv.releasetime is not NULL
-- Get the latest published version of the dataset...
and dsv.id = maxversion.id
-- ... done getting the latest published version of the dataset.
and dvo.id = filedatefilter.id
-- optionally limit to datasets published at a certain time
-- FIXME: Why don't the query complete unless we exclude December?
and dsv.releasetime BETWEEN '2018-01-01' AND '2018-11-30'
-- fast (16 seconds) for all but last day of 2018
--and dsv.releasetime BETWEEN '2018-01-01' AND '2018-12-30'
-- slow (unbounded) for all of 2018
--and dsv.releasetime BETWEEN '2018-01-01' AND '2018-12-31'
-- fast for just the last day of 2018
--and dsv.releasetime BETWEEN '2018-12-30' AND '2018-12-31'
GROUP BY
-- Group by all the fields above *except* subject for the string_agg above.
fileid,
filename,
dataset_name,
dataverse_level_1_id,
dataverse_level_1_alias,
dataverse_level_1_friendly_name,
dataverse_level_2_id,
dataverse_level_2_alias,
dataverse_level_2_friendly_name,
dataverse_level_3_id,
dataverse_level_3_alias,
dataverse_level_3_friendly_name,
dataset_publication_date
order by dataset_publication_date desc;
