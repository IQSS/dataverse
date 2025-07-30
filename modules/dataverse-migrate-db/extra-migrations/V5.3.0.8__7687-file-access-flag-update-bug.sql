-- this query will identify datasets where a superuser has run the Curate command and the update included a change to
-- the fileaccessrequest flag, resulting in the file access request updates not being reflected in the published version
DO
$$
    DECLARE
        -- should_migrate BOOLEAN := CASE WHEN UPPER('...') = 'TRUE' THEN TRUE ELSE FALSE END;
        affected_count INTEGER;
    BEGIN
        -- Get the count of rows that match the criteria
        SELECT COUNT(*) INTO affected_count
        from datasetversion dv, termsofuseandaccess ta, dataset da
        where dv.dataset_id=da.id
          and dv.termsofuseandaccess_id=ta.id
          and ta.fileaccessrequest != da.fileaccessrequest
          and dv.versionstate='RELEASED'
          and dv.releasetime in (select max(releasetime)
                                 from datasetversion
                                 where dataset_id=da.id);

        -- Only show hint if count is greater than 0
        IF affected_count > 0 THEN
            RAISE NOTICE '--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---';
            RAISE NOTICE 'Found % datasets affected by issue #7687', affected_count;
            RAISE NOTICE 'For now, please fix these manually. See Dataverse v5.4 release notes about #7687.';
            RAISE NOTICE '--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---';
        END IF;

        -- TODO: an opt-in migration to fix them all would be nice!
    END;
$$;