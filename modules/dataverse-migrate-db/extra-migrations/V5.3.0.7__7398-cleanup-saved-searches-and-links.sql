--
DO
$$
    DECLARE
        should_delete BOOLEAN := CASE WHEN UPPER('${V5_4_cleanup_searches_and_links}') = 'TRUE' THEN TRUE ELSE FALSE END;
        affected_searches_count INTEGER;
        affected_linked_datasets_count INTEGER;
        affected_linked_collections_count INTEGER;
        row_count INTEGER;
    BEGIN
        -- Get the count of rows that match the criteria
        SELECT COUNT(*) INTO affected_searches_count
        from savedsearch ss, savedsearchfilterquery ssfq, dataverselinkingdataverse dld
        where ss.id = ssfq.savedsearch_id
          and ss.definitionpoint_id = dld.linkingdataverse_id
          and dld.dataverse_id = rtrim(reverse(split_part(reverse(ssfq.filterquery),'/',1)),'"')::integer
          and ss.query='*'
          and ssfq.filterquery like 'subtreePaths%';

        select COUNT(*) INTO affected_linked_datasets_count
        from datasetlinkingdataverse dld, dvobject dvo, dataverselinkingdataverse dvld
        where dld.dataset_id = dvo.id
          and dld.linkingdataverse_id = dvld.linkingdataverse_id
          and dvo.owner_id = dvld.dataverse_id;

        select COUNT(*) INTO affected_linked_collections_count
        from dataverselinkingdataverse dld, dvobject dvo, dataverselinkingdataverse dvld
        where dld.dataverse_id = dvo.id
          and dld.linkingdataverse_id = dvld.linkingdataverse_id
          and dvo.owner_id = dvld.dataverse_id;

        -- Only show hint if count is greater than 0
        IF affected_searches_count > 0 OR affected_linked_datasets_count > 0 OR affected_linked_collections_count > 0 THEN
            RAISE NOTICE '--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---';
            RAISE NOTICE 'Found % saved searches affected by issue #7398', affected_searches_count;
            RAISE NOTICE 'Found % dataset links affected by issue #7398', affected_linked_datasets_count;
            RAISE NOTICE 'Found % collections links affected by issue #7398', affected_linked_collections_count;

            IF should_delete THEN
                RAISE NOTICE 'Cleaning up Saved Searches and Linked Datasets as requested by -Dmigrate.cleanupSavedSearches';

                -- delete the saved searches identified using the ss_for_deletion query
                create temporary table delete_ss on commit drop as (
                    Select ss.id
                    from savedsearch ss, savedsearchfilterquery ssfq, dataverselinkingdataverse dld
                    where ss.id = ssfq.savedsearch_id
                      and ss.definitionpoint_id = dld.linkingdataverse_id
                      and dld.dataverse_id = rtrim(reverse(split_part(reverse(ssfq.filterquery),'/',1)),'"')::integer
                      and ss.query='*'
                      and ssfq.filterquery like 'subtreePaths%'
                );

                GET DIAGNOSTICS row_count = ROW_COUNT;

                delete from savedsearchfilterquery where savedsearch_id in (select id from delete_ss);
                delete from savedsearch where id in (select id from delete_ss);

                RAISE NOTICE 'Deleted % saved searches', row_count;

                COMMIT;

                -- delete linked objects identified using the query in dld_for_deletion
                delete from datasetlinkingdataverse where id in (
                    select dld.id
                    from datasetlinkingdataverse dld, dvobject dvo, dataverselinkingdataverse dvld
                    where dld.dataset_id = dvo.id
                      and dld.linkingdataverse_id = dvld.linkingdataverse_id
                      and dvo.owner_id = dvld.dataverse_id
                );

                GET DIAGNOSTICS row_count = ROW_COUNT;
                RAISE NOTICE 'Deleted % linked datasets', row_count;

                delete from dataverselinkingdataverse where id in (
                    select dld.id
                    from dataverselinkingdataverse dld, dvobject dvo, dataverselinkingdataverse dvld
                    where dld.dataverse_id = dvo.id
                      and dld.linkingdataverse_id = dvld.linkingdataverse_id
                      and dvo.owner_id = dvld.dataverse_id
                );

                GET DIAGNOSTICS row_count = ROW_COUNT;
                RAISE NOTICE 'Deleted % linked collections', row_count;

                COMMIT;

            ELSE
                RAISE NOTICE 'Auto-migrate these using `mvn -Dmigrate.cleanupSavedSearches ...` (re-execute migrations)';
            END IF;
            RAISE NOTICE '--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---';
        END IF;
    END
$$;
