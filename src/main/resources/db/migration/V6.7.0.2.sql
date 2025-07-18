-- Update Setting table structure for changes from #11639
-- 1. Change column types from TEXT to VARCHAR for better performance
-- 2. Update lang column to use empty string default instead of NULL (avoid non-unique pairs)
-- 3. Add NOT NULL constraints and unique constraint for name+lang pairs

-- First, update any existing NULL lang values to empty string
UPDATE Setting SET lang = '' WHERE lang IS NULL;

-- Postgres doesn't support IF NOT EXISTS for ALTER COLUMN or ADD CONSTRAINT, so we need conditional logic
DO $$
BEGIN
    -- Only alter columns if they need to be changed
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'Setting' AND column_name = 'name'
               AND (data_type = 'text' OR is_nullable = 'YES')) THEN
        ALTER TABLE setting ALTER COLUMN name TYPE VARCHAR(255);
        ALTER TABLE Setting ALTER COLUMN name SET NOT NULL;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'Setting' AND column_name = 'lang'
               AND (data_type = 'text' OR is_nullable = 'YES')) THEN
        ALTER TABLE Setting ALTER COLUMN lang TYPE VARCHAR(10);
        ALTER TABLE Setting ALTER COLUMN lang SET NOT NULL;
        ALTER TABLE Setting ALTER COLUMN lang SET DEFAULT '';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE table_name = 'Setting'
                     AND constraint_name = 'UC_setting_name_lang'
                     AND constraint_type = 'UNIQUE') THEN
        ALTER TABLE Setting ADD CONSTRAINT UC_setting_name_lang UNIQUE (name, lang);
    END IF;

END $$;
