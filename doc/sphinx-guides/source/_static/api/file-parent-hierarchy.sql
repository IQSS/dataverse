WITH RECURSIVE tree (id, dtype, owner_id, level, id_path) AS (
    SELECT  id, dtype, owner_id, -1, ARRAY[id]
    FROM    dvobject
    WHERE   owner_id in (select id from dvobject where owner_id is null)
    AND 	dtype='Dataverse'

    UNION ALL

    SELECT  dvo.id, dvo.dtype, dvo.owner_id, t0.level + 1,
    		(CASE dvo.dtype
    		WHEN 'Dataverse' THEN ARRAY_APPEND(t0.id_path, dvo.id)
    		ELSE id_path
    		END)
    FROM    dvobject dvo
            INNER JOIN tree t0 ON t0.id = dvo.owner_id
)

SELECT  id, owner_id as dataset_id, level, id_path[1] AS dv1_id, id_path[2] as dv2_id, id_path[3] as dv3_id
FROM    tree
Where dtype = 'DataFile'
order by level desc;
