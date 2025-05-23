--
DO
$$
    DECLARE
        should_migrate BOOLEAN := CASE WHEN UPPER('${V6_3_migrate_keywordTermUri}') = 'TRUE' THEN TRUE ELSE FALSE END;
    BEGIN
        IF should_migrate THEN
            RAISE NOTICE 'Migrating keywordValue fields with http... to keywordTermUri as requested';
            UPDATE datasetfield df
            SET datasetfieldtype_id = (SELECT id FROM datasetfieldtype WHERE name = 'keywordTermURI')
            FROM datasetfieldvalue dfv
            WHERE dfv.datasetfield_id = df.id
              AND df.datasetfieldtype_id = (SELECT id FROM datasetfieldtype WHERE name = 'keywordValue')
              AND dfv.value ILIKE 'http%';
        END IF;
    END
$$;