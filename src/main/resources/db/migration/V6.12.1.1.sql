-- Delete OAI records for datasets that are inside Locally FAIR dataverses.
-- A dataset is Locally FAIR when its owner dataverse has at least one entry
-- in the dataverse_locallyfairassignees table.
-- The OAIRecord.globalid column stores the full persistent identifier string
-- (e.g. "doi:10.5072/FK2/..."), which corresponds to
-- CONCAT(dvobject.protocol, ':', dvobject.authority, dvobject.separator, dvobject.identifier).
DELETE FROM oairecord
WHERE globalid IN (
    SELECT CONCAT(dvobject.protocol, ':', dvobject.authority, dvobject.separator, dvobject.identifier)
    FROM dvobject
    INNER JOIN dataset ON dvobject.id = dataset.id
    INNER JOIN dataverse_locallyfairassignees ON dvobject.owner_id = dataverse_locallyfairassignees.dataverse_id
);
