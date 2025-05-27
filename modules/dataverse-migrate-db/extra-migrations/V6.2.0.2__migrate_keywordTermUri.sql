--
DO
$$
    DECLARE
        should_migrate BOOLEAN := CASE WHEN UPPER('${V6_3_migrate_keywordTermUri}') = 'TRUE' THEN TRUE ELSE FALSE END;
        keyword_count INTEGER;
    BEGIN
        -- Get the count of rows that match the criteria
        SELECT COUNT(*) INTO keyword_count
        FROM datasetfieldvalue dfv INNER JOIN datasetfield df ON df.id = dfv.datasetfield_id
        WHERE df.datasetfieldtype_id = (SELECT id FROM datasetfieldtype WHERE name = 'keywordValue')
          AND value ILIKE 'http%';

        -- Only show hint if count is greater than 0
        IF keyword_count > 0 THEN
            RAISE NOTICE '--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---';
            RAISE NOTICE 'Found % keywordValue metadata fields starting with "http"', keyword_count;

            IF should_migrate THEN
                RAISE NOTICE 'Migrating keywordValue fields with http... to keywordTermUri as requested by -Dmigrate.keywordTermUri';
                UPDATE datasetfield df
                SET datasetfieldtype_id = (SELECT id FROM datasetfieldtype WHERE name = 'keywordTermURI')
                FROM datasetfieldvalue dfv
                WHERE dfv.datasetfield_id = df.id
                  AND df.datasetfieldtype_id = (SELECT id FROM datasetfieldtype WHERE name = 'keywordValue')
                  AND dfv.value ILIKE 'http%';

                GET DIAGNOSTICS keyword_count = ROW_COUNT;
                RAISE NOTICE 'Updated % rows', keyword_count;

            ELSE
                RAISE NOTICE 'Auto-migrate these into keywordTermUri using `mvn -Dmigrate.keywordTermUri ...` (re-execute migrations)';
            END IF;
            RAISE NOTICE '--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---';
        END IF;
    END
$$;