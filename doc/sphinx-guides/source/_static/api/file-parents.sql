-- For non-root dataverses, build a "path" array containing their hierarchy.
WITH RECURSIVE tree (id, dtype, owner_id, level, id_path) AS (
    SELECT  id, dtype, owner_id, -1, ARRAY[id]
    FROM    dvobject
    WHERE   owner_id in (select id from dvobject where owner_id is null)
    AND     dtype='Dataverse'

    UNION ALL

    SELECT  dvo.id, dvo.dtype, dvo.owner_id, t0.level + 1,
        (CASE dvo.dtype
        WHEN 'Dataverse' THEN ARRAY_APPEND(t0.id_path, dvo.id)
        ELSE id_path
        END)
    FROM    dvobject dvo
            INNER JOIN tree t0 ON t0.id = dvo.owner_id
), maxVersion as (select id  from datasetversion where
concat(datasetversion.dataset_id,':', datasetversion.versionnumber + (.01 * datasetversion.minorversionnumber)) in
(select concat(datasetversion.dataset_id,':', max(datasetversion.versionnumber + (.01 * datasetversion.minorversionnumber))) as max
from datasetversion
join dataset on dataset.id = datasetversion.dataset_id
where versionstate='RELEASED'
and dataset.harvestingclient_id is null
group by dataset_id))
select
    -- ID of the file
    dvo.id as fileid,
    fmd.label as filename,
    -- Strip out newlines in dataset titles.
    regexp_replace(dsfv.value, '[\r\n]+', ' ', 'g' ) as dataset_name,
    -- Dataverses that are direct children of the root dataverse.
    tree.id_path[1] as dataverse_level_1_id,
    level1dv.alias as dataverse_level_1_alias,
    level1dv.name as dataverse_level_1_friendly_name,
    -- Dataverses that are grandchildren of the root dataverse.
    level2dv.id as dataverse_level_2_id,
    level2dv.alias as dataverse_level_2_alias,
    level2dv.name as dataverse_level_2_friendly_name,
    -- Dataverses that are grandchildren of the root dataverse.
    level3dv.id as dataverse_level_3_id,
    level3dv.alias as dataverse_level_3_alias,
    level3dv.name as dataverse_level_3_friendly_name,
    -- Separate multiple subjects with a delimeter.
    string_agg(cvv.strvalue, ';') AS subjects,
    -- File publication date.
    dvo.publicationdate as file_publication_date,
    -- Dataset publication date.
    dsv.releasetime as dataset_publication_date
from filemetadata fmd, datasetversion dsv,
datasetfieldvalue dsfv, datasetfield dsf,
datasetfield dsfsub, datasetfield_controlledvocabularyvalue dsfcvv,
controlledvocabularyvalue cvv, dvobject dvo,
dataverse level1dv, dataset, maxversion, tree
left outer join dataverse level2dv on tree.id_path[2] = level2dv.id
left outer join dataverse level3dv on tree.id_path[3] = level3dv.id
where fmd.datasetversion_id = dsv.id
and dsv.dataset_id = dataset.id
and tree.id = dvo.id
and tree.id_path[1] = level1dv.id
and dsv.id = dsf.datasetversion_id
and dsf.id = dsfv.datasetfield_id
and fmd.datafile_id = dvo.id
and dsf.datasetfieldtype_id=1
-- We added dsfsub and dsfcvv to get subjects.
and dsfsub.datasetversion_id = dsv.id
-- For dev databases, "subject" is 20.
and dsfsub.datasetfieldtype_id=20
-- For Harvard's database, "subject" is 19.
--and dsfsub.datasetfieldtype_id=19
and dsfsub.id = dsfcvv.datasetfield_id
and cvv.datasetfieldtype_id = dsfsub.datasetfieldtype_id
and dsfcvv.controlledvocabularyvalues_id = cvv.id
-- Make sure the dataset is published.
and dsv.releasetime is not NULL
-- Make sure the file is published.
and dvo.publicationdate is not NULL
-- Get the latest published version of the dataset...
and dsv.id = maxversion.id
-- ... done getting the latest published version of the dataset.
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
file_publication_date,
dataset_publication_date
order by file_publication_date desc;
