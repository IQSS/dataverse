-- Migration script to delete orphan templates (templates with no associated dataverse), see also issue #8600

DO
$$
    DECLARE
        orphan_templates_count INTEGER;
        affected_collections_count INTEGER;
        row_count INTEGER;
    BEGIN
        -- Get the count of orphan templates
        SELECT COUNT(t.id) INTO orphan_templates_count
        FROM template t
        WHERE dataverse_id IS NULL;

        -- Count dataverse collections that use orphan templates as default
        SELECT COUNT(*) INTO affected_collections_count
        FROM dataverse d
        WHERE d.defaulttemplate_id IN (
            SELECT t.id FROM template t WHERE dataverse_id IS NULL
        );

        -- Only execute queries if the affected count is greater than 0
        IF orphan_templates_count > 0 THEN
            RAISE NOTICE '--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---';
            RAISE NOTICE 'Found % orphan templates (templates with no associated dataverse)', orphan_templates_count;
            RAISE NOTICE 'Found % dataverses using orphan templates as their default template', affected_collections_count;

            -- Please note: The below is quite a bunch of things we need to execute. When using the Admin API call
            --              to delete the template, all the cascading is handled by JPA for us. We don't have that...

            -- First, update all dataverses that use orphan templates as default template
            UPDATE dataverse
            SET defaulttemplate_id = NULL
            WHERE defaulttemplate_id IN (
                SELECT t.id FROM template t WHERE dataverse_id IS NULL
            );

            GET DIAGNOSTICS row_count = ROW_COUNT;
            RAISE NOTICE 'Updated % collections to remove orphan templates set as default', row_count;
            
            -- Create a temporary table to keep track of datasetfields to delete
            CREATE TEMPORARY TABLE temp_datasetfields_to_delete AS
            SELECT id FROM datasetfield
            WHERE template_id IN (
                SELECT t.id FROM template t WHERE dataverse_id IS NULL
            );
            
            -- Create a temporary table to keep track of compound values to delete
            CREATE TEMPORARY TABLE temp_compoundvalues_to_delete AS
            SELECT cv.id
            FROM datasetfieldcompoundvalue cv
            WHERE cv.parentdatasetfield_id IN (
                SELECT id FROM temp_datasetfields_to_delete
            );
            
            -- Delete mappings between datasetfield and controlledvocabularyvalues
            DELETE FROM datasetfield_controlledvocabularyvalue
            WHERE datasetfield_id IN (
                SELECT id FROM temp_datasetfields_to_delete
            );
            
            GET DIAGNOSTICS row_count = ROW_COUNT;
            RAISE NOTICE 'Deleted % vocabulary mappings associated with orphan templates', row_count;
            
            -- Delete datasetfieldvalue records that reference the datasetfields we're going to delete
            DELETE FROM datasetfieldvalue
            WHERE datasetfield_id IN (
                SELECT id FROM temp_datasetfields_to_delete
            );
            
            GET DIAGNOSTICS row_count = ROW_COUNT;
            RAISE NOTICE 'Deleted % datasetfieldvalues associated with orphan templates', row_count;
            
            -- Break the circular reference by setting parentdatasetfieldcompoundvalue_id to NULL
            UPDATE datasetfield
            SET parentdatasetfieldcompoundvalue_id = NULL
            WHERE parentdatasetfieldcompoundvalue_id IN (
                SELECT id FROM temp_compoundvalues_to_delete
            );
            
            GET DIAGNOSTICS row_count = ROW_COUNT;
            RAISE NOTICE 'Updated % datasetfields to remove references to compound values', row_count;
            
            -- Now we can safely delete the compound values
            DELETE FROM datasetfieldcompoundvalue
            WHERE id IN (
                SELECT id FROM temp_compoundvalues_to_delete
            );
            
            GET DIAGNOSTICS row_count = ROW_COUNT;
            RAISE NOTICE 'Deleted % datasetfieldcompoundvalues associated with orphan templates', row_count;
            
            -- Delete datasetfields that reference orphan templates
            DELETE FROM datasetfield 
            WHERE id IN (
                SELECT id FROM temp_datasetfields_to_delete
            );

            GET DIAGNOSTICS row_count = ROW_COUNT;
            RAISE NOTICE 'Deleted % datasetfields referencing orphan templates', row_count;

            -- Clean up temporary tables
            DROP TABLE temp_datasetfields_to_delete;
            DROP TABLE temp_compoundvalues_to_delete;

            -- Then finally delete all orphan templates
            DELETE FROM template
            WHERE dataverse_id IS NULL;

            GET DIAGNOSTICS row_count = ROW_COUNT;
            RAISE NOTICE 'Deleted % orphan templates', row_count;
            RAISE NOTICE '--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---';
        END IF;
    END
$$;