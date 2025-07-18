-- Migrates the old TabularIngestSizeLimit database setting using format suffixes to a JSON based approach. See #11639

DO $$
    DECLARE
        base_setting_content TEXT;
        format_settings_cursor CURSOR FOR
            SELECT name, content
            FROM Setting
            WHERE name LIKE ':TabularIngestSizeLimit:%'
              AND lang IS NULL;
        format_record RECORD;
        format_name TEXT;
        format_value BIGINT;
        json_object JSONB := '{}';
        has_format_settings BOOLEAN := FALSE;
        warning_message TEXT;
    BEGIN
        -- Check if there are any format-specific settings
        SELECT EXISTS(
            SELECT 1 FROM Setting
            WHERE name LIKE ':TabularIngestSizeLimit:%'
              AND lang IS NULL
        ) INTO has_format_settings;

        -- Only proceed if we have format-specific settings
        IF NOT has_format_settings THEN
            RAISE NOTICE 'No format-specific TabularIngestSizeLimit settings found. Skipping migration.';
            RETURN;
        END IF;

        -- Get the base setting (without format suffix) if it exists
        SELECT content INTO base_setting_content
        FROM Setting
        WHERE name = ':TabularIngestSizeLimit'
          AND lang IS NULL;

        -- Add base setting to JSON object if it exists
        IF base_setting_content IS NOT NULL THEN
            -- Validate that base setting is numeric
            BEGIN
                format_value := base_setting_content::BIGINT;
                json_object := json_object || jsonb_build_object('default', format_value);
            EXCEPTION WHEN invalid_text_representation THEN
                RAISE WARNING 'Base TabularIngestSizeLimit setting contains non-numeric value: %. Setting to 0 (disabling ingest!).', base_setting_content;
                json_object := json_object || jsonb_build_object('default', 0);
            END;
        END IF;

        -- Process format-specific settings
        FOR format_record IN format_settings_cursor LOOP
                -- Extract format name (everything after ":TabularIngestSizeLimit:")
                format_name := substring(format_record.name from ':TabularIngestSizeLimit:(.*)');

                -- Validate and convert the content to numeric
                BEGIN
                    format_value := format_record.content::BIGINT;
                    json_object := json_object || jsonb_build_object(format_name, format_value);
                EXCEPTION WHEN invalid_text_representation THEN
                    warning_message := format('Format-specific TabularIngestSizeLimit setting %s contains non-numeric value: %s. Setting to 0 (disabling ingest!).',
                                              format_record.name, format_record.content);
                    RAISE WARNING '%', warning_message;
                    json_object := json_object || jsonb_build_object(format_name, 0);
                END;
            END LOOP;

        -- Insert or update the new JSON-based setting
        INSERT INTO Setting (name, content, lang)
        VALUES (':TabularIngestSizeLimit', json_object::TEXT, NULL)
        ON CONFLICT (name) WHERE lang IS NULL
            DO UPDATE SET content = EXCLUDED.content;

        -- Delete all format-specific settings
        DELETE FROM Setting
        WHERE name LIKE ':TabularIngestSizeLimit:%'
          AND lang IS NULL;

        RAISE NOTICE 'Successfully migrated TabularIngestSizeLimit settings to JSON format: %', json_object::TEXT;
    END $$;