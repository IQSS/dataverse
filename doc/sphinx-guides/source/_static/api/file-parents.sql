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
)

select dvo.id as fileid, fmd.label as filename, dsfv.value as dataset_name, tree.id_path[1] as dataverse_level_1_id, level1dv.alias as dataverse_level_1_alias, level1dv.name as dataverse_level_1_friendly_name, level2dv.id as dataverse_level_2_id, level2dv.alias as dataverse_level_2_alias, level2dv.name as dataverse_level_2_friendly_name, level3dv.id as dataverse_level_3_id, level3dv.alias as dataverse_level_3_alias, level3dv.name as dataverse_level_3_friendly_name, dsv.releasetime as dataset_publication_date, dvo.publicationdate as file_publication_date
from filemetadata fmd, datasetversion dsv, datasetfieldvalue dsfv, datasetfield dsf, dvobject dvo, dataverse level1dv, tree
left outer join dataverse level2dv on tree.id_path[2] = level2dv.id
left outer join dataverse level3dv on tree.id_path[3] = level3dv.id
where fmd.datasetversion_id = dsv.id
and tree.id = dvo.id
and tree.id_path[1] = level1dv.id
and dsv.id = dsf.datasetversion_id
and dsf.id = dsfv.datasetfield_id
and fmd.datafile_id = dvo.id
and dsf.datasetfieldtype_id=1
and dsv.releasetime is not NULL
and dvo.publicationdate is not NULL
order by file_publication_date desc;
