-- this query will identify datasets where at least one file does not have either terms of access or
-- request access enabled, and will include owner information for those datasets

DO
$$
    DECLARE
        affected_dataset RECORD;
        row_count INTEGER := 0;
    BEGIN
        -- Create a temporary table to store the results
        CREATE TEMPORARY TABLE IF NOT EXISTS affected_dataset_results (
                                                                          email TEXT,
                                                                          name TEXT,
                                                                          dataset_url TEXT
        );

        -- Insert the query results into the temporary table
        INSERT INTO affected_dataset_results
        select au.email,
               concat(au.firstname, ' ', au.lastname) as name,
               concat('dx.doi.org/' , dvo.authority , '/' ,  dvo.identifier) as dataset_url
        from roleassignment ra, dataverserole dvr,
             authenticateduser au, dvobject dvo
        where
            au.useridentifier = rtrim(substring(ra.assigneeidentifier, 2, 100))
          and dvo.id = ra.definitionpoint_id
          and
            ra.role_id = dvr.id and
            dvr.alias in (
                          'fullContributor',
                          'dsContributor',
                          'contributor',
                          'admin',
                          'curator'
                ) and
            ra.definitionpoint_id in (
                select dvo.id from datasetversion v
                                       join termsofuseandaccess ua on ua.id = v.termsofuseandaccess_id
                                       join filemetadata fm on v.id = fm.datasetversion_id
                                       join datafile f on f.id = fm.datafile_id
                                       join dvobject dvo on v.dataset_id = dvo.id
                where ua.fileaccessrequest = false and ua.termsofaccess isnull
                  and f.restricted = true
            );

        -- Get the number of affected rows
        GET DIAGNOSTICS row_count = ROW_COUNT;

        -- Print notice if there are affected datasets
        IF row_count > 0 THEN
            RAISE NOTICE '--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---';
            RAISE NOTICE 'Found % dataset(s) with files lacking proper access settings.', row_count;
            RAISE NOTICE 'For details see Dataverse 5.11 release notes and issue 8191.';

            -- Loop through affected datasets and print details
            FOR affected_dataset IN SELECT * FROM affected_dataset_results LOOP
                    RAISE NOTICE 'Dataset %, Owner % (%)',
                        affected_dataset.dataset_url,
                        affected_dataset.name,
                        affected_dataset.email;
                END LOOP;

            RAISE NOTICE '--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---';
        END IF;

        -- Clean up temporary table
        DROP TABLE IF EXISTS affected_dataset_results;
    END;
$$;
